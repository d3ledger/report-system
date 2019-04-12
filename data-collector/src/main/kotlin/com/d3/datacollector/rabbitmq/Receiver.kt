package com.d3.datacollector.rabbitmq

import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.beans.factory.annotation.Autowired


class Receiver {

    fun receiveMessage(message: String) {
        println("Received <$message>")
    }
}
