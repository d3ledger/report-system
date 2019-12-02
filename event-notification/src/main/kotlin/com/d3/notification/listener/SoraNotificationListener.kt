package com.d3.notification.listener

import com.d3.notification.domain.EthWithdrawalProofs
import com.d3.notification.repository.EthWithdrawalProofRepository
import com.d3.notifications.event.SoraEthWithdrawalProofsEvent
import com.google.gson.Gson
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import com.rabbitmq.client.impl.DefaultExceptionHandler
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import mu.KLogging
import org.hibernate.exception.JDBCConnectionException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.CannotCreateTransactionException
import java.io.Closeable
import java.util.concurrent.Executors
import kotlin.system.exitProcess

private const val SORA_EVENTS_RX_QUEUE_NAME = "sora_notification_events_rx_queue"
private const val SORA_EVENTS_PERSIST_QUEUE_NAME = "sora_notification_events_persist_queue"
private const val SORA_EVENTS_EXCHANGE_NAME = "sora_notification_events_exchange"
private const val EVENT_TYPE_HEADER = "event_type"

/**o
 * Listener that listens to Sora events that comes through RabbitMQ
 */
@Component
class SoraNotificationListener(
    @Value("\${rmq.host}")
    private val rmqHost: String,
    @Value("\${rmq.port}")
    private val rmqPort: Int,
    private val ethWithdrawalProofRepository: EthWithdrawalProofRepository
) : Closeable {

    private val gson = Gson()
    private val connectionFactory = ConnectionFactory()
    private val channel: Channel
    private val connection: Connection
    private val sourceEthWithdrawalProofs = PublishSubject.create<SoraEthWithdrawalProofsEvent>()
    private val consumerTags = ArrayList<String>()
    private val persistencyExecutorService = Executors.newSingleThreadExecutor()
    private val rxExecutorService = Executors.newCachedThreadPool()

    /**
     * Initiates RabbitMQ connection, creates listeners, etc
     */
    init {
        // Connect to RMQ
        connectionFactory.host = rmqHost
        connectionFactory.port = rmqPort
        connection = connectionFactory.newConnection()
        channel = connection.createChannel()
        channel.basicQos(32)
        // Handle connection errors
        connectionFactory.exceptionHandler = object : DefaultExceptionHandler() {
            override fun handleConnectionRecoveryException(conn: Connection, exception: Throwable) {
                logger.error("RMQ connection error", exception)
                exitProcess(1)
            }

            override fun handleUnexpectedConnectionDriverException(conn: Connection, exception: Throwable) {
                logger.error("RMQ connection error", exception)
                exitProcess(1)
            }
        }
        channel.exchangeDeclare(SORA_EVENTS_EXCHANGE_NAME, "fanout", true)

        // Create queue that handles duplicates. The queue just publishes incoming events via RX
        channel.queueDeclare(SORA_EVENTS_RX_QUEUE_NAME, true, false, false, createDeduplicationArgs())
        channel.queueBind(SORA_EVENTS_RX_QUEUE_NAME, SORA_EVENTS_EXCHANGE_NAME, "")
        consumerTags.add(registerEthWithdrawalProofConsumer(SORA_EVENTS_RX_QUEUE_NAME) { event, ack, _ ->
            rxExecutorService.submit {
                try {
                    logger.info("Publish event via RX. Event $event")
                    sourceEthWithdrawalProofs.onNext(event)
                } finally {
                    ack()
                }
            }
        })

        // Create queue that handles duplicates. The queue is responsible for persisting events in the DB
        channel.queueDeclare(SORA_EVENTS_PERSIST_QUEUE_NAME, true, false, false, createDeduplicationArgs())
        channel.queueBind(SORA_EVENTS_PERSIST_QUEUE_NAME, SORA_EVENTS_EXCHANGE_NAME, "")
        consumerTags.add(registerEthWithdrawalProofConsumer(SORA_EVENTS_PERSIST_QUEUE_NAME) { event, ack, nack ->
            persistencyExecutorService.submit {
                try {
                    logger.info("Persist event. Event $event")
                    ethWithdrawalProofRepository.save(EthWithdrawalProofs.mapDomain(event))
                    logger.info("Event $event has been successfully persisted")
                    ack()
                } catch (e: CannotCreateTransactionException) {
                    if (e.cause is JDBCConnectionException) {
                        logger.error("Cannot persist event. No reason to live anymore. Try to re-queue", e)
                        nack()
                    } else {
                        logger.error("Cannot persist event. No reason to live anymore.", e)
                        exitProcess(1)
                    }
                } catch (e: Exception) {
                    logger.error("Cannot persist event. No reason to live anymore.", e)
                    exitProcess(1)
                }
            }
        })
    }

    /**
     * Subscribes to `enough proofs collected` events
     * @param subscribeLogic - subscribing logic
     */
    fun subscribeWithdrawalProofs(subscribeLogic: (SoraEthWithdrawalProofsEvent) -> Unit): Disposable =
        sourceEthWithdrawalProofs.subscribe(subscribeLogic)

    /**
     * Creates RabbitMQ queue arguments for deduplication
     */
    private fun createDeduplicationArgs() = hashMapOf<String, Any>(
        // enable deduplication
        Pair("x-message-deduplication", true),
        // save deduplication data on disk rather that memory
        Pair("x-cache-persistence", "disk"),
        // save deduplication data 1 hour
        Pair("x-cache-ttl", 60_000 * 60)
    )

    /**
     * Registers 'eth withdrawal proof' event consumer
     * @param queueName - name of queue
     * @param consumer - the main consumer logic. Warning! Responsibility of calling ack() and nack() relies on the consumer code.
     * @return consumer tag
     */
    private fun registerEthWithdrawalProofConsumer(
        queueName: String,
        consumer: (SoraEthWithdrawalProofsEvent, ack: () -> Unit, nack: () -> Unit) -> Unit
    ) = channel.basicConsume(queueName, false,
        { _: String, delivery: Delivery ->
            val json = String(delivery.body)
            val eventType = delivery.properties.headers[EVENT_TYPE_HEADER]?.toString() ?: ""
            logger.info("Got event type $eventType with message ${String(delivery.body)}")

            when (eventType) {
                // Handle proof collection events
                SoraEthWithdrawalProofsEvent::class.java.canonicalName -> {
                    val withdrawalEventProof = gson.fromJson(json, SoraEthWithdrawalProofsEvent::class.java)
                    consumer(withdrawalEventProof,
                        { channel.basicAck(delivery.envelope.deliveryTag, false) },
                        { channel.basicNack(delivery.envelope.deliveryTag, false, true) })
                }
                else -> {
                    logger.warn("Event type $eventType is not supported")
                }
            }
        }
        , { _ -> })

    override fun close() {
        consumerTags.forEach { consumerTag ->
            channel.basicCancel(consumerTag)
        }
        connection.close()
        rxExecutorService.shutdownNow()
        persistencyExecutorService.shutdownNow()
    }

    companion object : KLogging()
}
