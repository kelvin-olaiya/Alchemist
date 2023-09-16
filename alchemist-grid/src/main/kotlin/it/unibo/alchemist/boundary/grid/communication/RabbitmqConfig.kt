/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.communication

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import it.unibo.alchemist.boundary.launchers.ConfigurationProvider

object RabbitmqConfig {

    private lateinit var connection: Connection

    val channel: Channel by lazy { connection.createChannel() }

    fun setUpConnection(config: ConfigurationProvider.RabbitmqConnectionConfig) {
        connection = ConnectionFactory().also {
            it.username = config.username
            it.password = config.password
            it.host = config.host
            it.port = config.port
        }.newConnection()
    }

    fun closeConnection() {
        channel.close()
        connection.close()
    }
}
