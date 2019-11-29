package com.d3.notification.controller

import com.d3.notification.listener.SoraNotificationListener
import com.google.gson.Gson
import mu.KLogging
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.util.*

/**
 * Controller that is responsible for Sora event handling
 */
@CrossOrigin(origins = ["*"], allowCredentials = "true", allowedHeaders = ["*"])
@RestController
@RequestMapping("/notification")
class NotificationController(private val notificationListener: SoraNotificationListener) {

    private val gson = Gson()

    /**
     * Subscribes to `withdrawal proofs collected` event stream
     */
    @GetMapping(path = ["/subscribe/withdrawalProofs/{accountId}"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribeWithdrawalProofs(@PathVariable accountId: String): Flux<ServerSentEvent<String>> {
        val clientID = UUID.randomUUID()
        logger.info("New subscriber. Client id $clientID")
        return Flux.create { emitter ->
            val disposable = notificationListener.subscribeWithdrawalProofs { soraWithdrawalProofEvent ->
                if (soraWithdrawalProofEvent.accountIdToNotify == accountId) {
                    val event = ServerSentEvent.builder<String>()
                        .event("sora-withdrawal-proofs-event")
                        .data(gson.toJson(soraWithdrawalProofEvent))
                        .build()
                    emitter.next(event)
                }
            }
            emitter.onDispose {
                logger.info("Dispose client $clientID")
                disposable.dispose()
            }
        }
    }

    companion object : KLogging()
}
