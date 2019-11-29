package com.d3.notification.listener

import com.d3.notifications.event.SoraEthWithdrawalProofsEvent
import com.google.gson.Gson
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Delivery
import io.reactivex.subjects.PublishSubject
import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.Closeable

private const val SORA_EVENTS_QUEUE_NAME = "sora_notification_events_queue"
private const val EVENT_TYPE_HEADER = "event_type"

/**o
 * Listener that listens to Sora events that comes through RabbitMQ
 */
@Component
class SoraNotificationListener(
    @Value("\${rmq.host}")
    private val rmqHost: String,
    @Value("\${rmq.port}")
    private val rmqPort: Int
) : Closeable {

    private val gson = Gson()
    private val connectionFactory = ConnectionFactory()
    private val channel: Channel
    private val connection: Connection
    private val sourceEthWithdrawalProofs = PublishSubject.create<SoraEthWithdrawalProofsEvent>()
    private val consumerTag: String

    /**
     * Initiates RabbitMQ connection, creates listeners, etc
     */
    init {
        // Connect to RMQ
        connectionFactory.host = rmqHost
        connectionFactory.port = rmqPort
        connection = connectionFactory.newConnection()
        channel = connection.createChannel()
        // Create queue that handles duplicates
        channel.queueDeclare(SORA_EVENTS_QUEUE_NAME, true, false, false, createDeduplicationArgs())
        consumerTag = channel.basicConsume(SORA_EVENTS_QUEUE_NAME, true,
            { _: String, delivery: Delivery ->
                val json = String(delivery.body)
                val eventType = delivery.properties.headers[EVENT_TYPE_HEADER]?.toString() ?: ""
                logger.info("Got event type $eventType with message ${String(delivery.body)}")
                when (eventType) {
                    // Handle proof collection events
                    SoraEthWithdrawalProofsEvent::class.java.canonicalName -> {
                        val withdrawalEventProof = gson.fromJson(json, SoraEthWithdrawalProofsEvent::class.java)
                        //logger.info("Got withdrawal proof event: $withdrawalEventProof")
                        sourceEthWithdrawalProofs.onNext(withdrawalEventProof)
                    }
                    else -> {
                        logger.warn("Event type $eventType is not supported")
                    }
                }
            }
            , { _ -> })
    }

    /**
     * Subscribes to `enough proofs collected` events
     * @param subscribeLogic - subscribing logic
     */
    fun subscribeWithdrawalProofs(subscribeLogic: (SoraEthWithdrawalProofsEvent) -> Unit) =
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

    override fun close() {
        channel.basicCancel(consumerTag)
        connection.close()
    }

    companion object : KLogging()
}
