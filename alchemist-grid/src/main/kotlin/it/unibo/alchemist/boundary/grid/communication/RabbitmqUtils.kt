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

    fun publishToQueue(queueName: String, payload: ByteArray) {
        channel.basicPublish("", queueName, AMQP.BasicProperties(), payload)
    }

    fun registerQueueConsumer(queueName: String, callback: DeliverCallback) {
        channel.basicConsume(queueName, false, callback) { _ -> }
    }
}
