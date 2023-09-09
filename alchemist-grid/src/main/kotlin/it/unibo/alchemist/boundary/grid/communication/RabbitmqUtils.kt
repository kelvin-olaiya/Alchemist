/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.communication

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.DeliverCallback
import it.unibo.alchemist.boundary.grid.communication.RabbitmqConfig.channel

object RabbitmqUtils {

    fun declareQueue(name: String) {
        channel.queueDeclare(name, false, false, false, null)
    }

    fun publishToQueue(
        queueName: String,
        payload: ByteArray,
        properties: AMQP.BasicProperties = AMQP.BasicProperties(),
    ) {
        channel.basicPublish("", queueName, properties, payload)
    }

    fun publishToQueue(queueName: String, replyTo: String, payload: ByteArray) =
        publishToQueue(queueName, payload, AMQP.BasicProperties().builder().replyTo(replyTo).build())

    fun registerQueueConsumer(queueName: String, callback: DeliverCallback) {
        channel.basicConsume(queueName, false, callback) { _ -> }
    }
}
