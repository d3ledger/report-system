package com.d3.notification

import com.d3.notifications.event.SoraECDSASignature
import com.d3.notifications.event.SoraEthWithdrawalProofsEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import integration.helper.ContainerHelper
import integration.helper.KGenericContainer
import mu.KLogging
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.testcontainers.containers.Network
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals

const val DEFAULT_RMQ_PORT = 5672
const val DEFAULT_POSTGRES_PORT = 5432

private const val SORA_EVENTS_EXCHANGE_NAME = "sora_notification_events_exchange"
private const val EVENT_TYPE_HEADER = "event_type"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventNotificationIntegrationTest {

    private val gson = Gson()
    private val connectionFactory = ConnectionFactory()
    private lateinit var channel: Channel
    private lateinit var connection: Connection

    private val containerHelper = ContainerHelper()

    private val rmqContainer =
        KGenericContainer("rabbitmq:3-management").withExposedPorts(DEFAULT_RMQ_PORT).withNetwork(Network.SHARED)
            .withNetworkAliases("rmq")

    private val postgresContainer =
        KGenericContainer("postgres").withExposedPorts(DEFAULT_POSTGRES_PORT).withNetwork(Network.SHARED)
            .withNetworkAliases("postgres")

    private val notificationContextFolder = "${containerHelper.userDir}/build/docker/"
    private val notificationDockerfile = "${containerHelper.userDir}/build/docker/Dockerfile"

    private val notificationContainer =
        containerHelper.createSoraPluginContainer(notificationContextFolder, notificationDockerfile)
            .withNetwork(Network.SHARED)

    @BeforeAll
    fun setUp() {
        rmqContainer.start()
        postgresContainer.withEnv("POSTGRES_PASSWORD", "test").withEnv("POSTGRES_USER", "test").start()
        notificationContainer
            .withEnv("POSTGRES_HOST", "postgres")
            .withEnv("POSTGRES_DATABASE", "postgres")
            .withEnv("POSTGRES_PORT", DEFAULT_POSTGRES_PORT.toString())
            .withEnv("RMQ_HOST", "rmq")
            .withEnv("RMQ_PORT", DEFAULT_RMQ_PORT.toString())
            .withEnv("SPRING_DATASOURCE_USERNAME", "test")
            .withEnv("SPRING_DATASOURCE_PASSWORD", "test")
            .withExposedPorts(8080)
            .start()
        connectionFactory.host = rmqContainer.containerIpAddress
        connectionFactory.port = rmqContainer.getMappedPort(DEFAULT_RMQ_PORT)
        connection = connectionFactory.newConnection()
        channel = connection.createChannel()
    }

    @AfterAll
    fun tearDown() {
        if (::channel.isInitialized) {
            channel.close()
        }
        if (::connection.isInitialized) {
            connection.close()
        }
        rmqContainer.stop()
        postgresContainer.stop()
    }

    /**
     * @given running instance of the 'event-notification' service
     * @when new proof appears in the queue
     * @then the proof is persisted properly
     */
    @Test
    fun testGetWithdrawalProofsHistoryPersist() {
        val accountId = "account@domain"
        val event = SoraEthWithdrawalProofsEvent(
            accountIdToNotify = accountId,
            tokenContractAddress = "0x123",
            amount = BigDecimal("1.5"),
            relay = "some relay",
            proofs = listOf(SoraECDSASignature("r", "s", BigInteger.TEN)),
            irohaTxHash = "hash",
            to = "to address",
            id = "some id",
            txTime = System.currentTimeMillis(),
            blockNum = 3,
            txIndex = 123
        )
        publishWithdrawalProofEvent(event)
        Thread.sleep(3_000)
        val result = khttp.get(
            "http://${notificationContainer.containerIpAddress}:${notificationContainer.getMappedPort(8080)}/notification/find/withdrawalProofs/$accountId",
            headers = mapOf(Pair("Content-type", "application/json"))
        )
        assertEquals(200, result.statusCode)
        val listType = object : TypeToken<List<SoraEthWithdrawalProofsEvent>>() {}.type
        val resultList: List<SoraEthWithdrawalProofsEvent> = gson.fromJson(result.text, listType)
        assertEquals(1, resultList.size)
        assertEquals(event.toJson(), resultList[0].toJson())
    }

    /**
     * @given running instance of the 'event-notification' service
     * @when a client connects to the streaming endpoint and a new proof appears in the queue
     * @then the proof is sent to the client via SSE
     */
    @Test
    fun testGetWithdrawalProofsHistoryStream() {
        val eventReference = AtomicReference<SoraEthWithdrawalProofsEvent>()
        val eventWaiter = CountDownLatch(1)
        val accountId = "account@domain"
        val client = WebClient.create(
            "http://${notificationContainer.containerIpAddress}:${notificationContainer.getMappedPort(8080)}"
        )
        val eventStream = client.get()
            .uri("/notification/subscribe/withdrawalProofs/$accountId")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(String::class.java)

        eventStream.subscribe(
            { content ->
                val event = gson.fromJson(content, SoraEthWithdrawalProofsEvent::class.java)
                eventReference.set(event)
                eventWaiter.countDown()
            },
            { error -> logger.error("Cannot read from stream", error) },
            { logger.info("Done reading from stream") })

        val event = SoraEthWithdrawalProofsEvent(
            accountIdToNotify = accountId,
            tokenContractAddress = "0x123",
            amount = BigDecimal("1.5"),
            relay = "some relay",
            proofs = listOf(SoraECDSASignature("r", "s", BigInteger.TEN)),
            irohaTxHash = "hash",
            to = "to address",
            id = "some id",
            txTime = System.currentTimeMillis(),
            blockNum = 3,
            txIndex = 123
        )
        Thread.sleep(5_000)
        publishWithdrawalProofEvent(event)
        eventWaiter.await()
        assertEquals(event.toJson(), eventReference.get().toJson())
    }

    /**
     * Publishes 'withdrawal proof' event
     * @param event - event to publish
     */
    private fun publishWithdrawalProofEvent(event: SoraEthWithdrawalProofsEvent) {
        channel.basicPublish(
            SORA_EVENTS_EXCHANGE_NAME,
            "",
            MessageProperties.MINIMAL_PERSISTENT_BASIC.builder().headers(
                mutableMapOf(
                    Pair<String, Any>(
                        EVENT_TYPE_HEADER,
                        SoraEthWithdrawalProofsEvent::class.java.canonicalName
                    )
                )
            ).build(),
            event.toString().toByteArray()
        )
    }

    companion object : KLogging()

}
