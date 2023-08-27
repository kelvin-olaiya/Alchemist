/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import java.util.UUID

object RabbitmqConfig {

    const val HEALTH_QUEUE_METADATA_KEY = "rabbitmq-health-queue"
    const val JOBS_QUEUE_METADATA_KEY = "rabbitmq-jobs-queue"

    fun getQueueNameFor(serverID: UUID, topic: String) = "$serverID-$topic"

    val connection: Connection = ConnectionFactory().also {
        it.username = "guest"
        it.password = "guest"
        it.host = "localhost"
        it.port = 5672
    }.newConnection()

    val channel: Channel = connection.createChannel()
}
