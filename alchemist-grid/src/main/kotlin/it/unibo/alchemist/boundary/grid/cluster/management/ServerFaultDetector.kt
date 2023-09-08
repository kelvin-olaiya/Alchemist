/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster.management

import com.rabbitmq.client.AMQP
import it.unibo.alchemist.boundary.grid.communication.CommunicationQueues
import it.unibo.alchemist.boundary.grid.communication.RabbitmqConfig.channel
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.publishToQueue
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.registerQueueConsumer
import it.unibo.alchemist.proto.ClusterMessages
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock

class ServerFaultDetector(
    val toWatchServerID: UUID,
    val timeoutMillis: Long,
    val maxReplyMiss: Int,
    val onFaultDetection: (UUID) -> Unit = {},
) : Runnable {
    private val logger = LoggerFactory.getLogger(ServerFaultDetector::class.java)
    private val responsesQueue = channel.queueDeclare().queue
    private val hearthbeatMessage =
        ClusterMessages.HealthCheckRequest.newBuilder().setReplyTo(responsesQueue).build()
    private var callbackRegistered = false
    private var replyMisses = 0
    private val mutex = ReentrantLock()

    override fun run() {
        var hasReplied: Boolean
        registerQueueConsumer(responsesQueue) { _, delivery ->
            val response = ClusterMessages.HealthCheckResponse.parseFrom(delivery.body)
            logger.debug("Received heartbeat response from server {}", toWatchServerID)
            mutex.lock()
            if (response.serverID == toWatchServerID.toString()) {
                hasReplied = true
                replyMisses = 0
            }
            mutex.unlock()
        }
        logger.debug("server checker started!")
        while (true) {
            hasReplied = false
            publishToQueue(
                CommunicationQueues.HEALTH.of(toWatchServerID),
                hearthbeatMessage.toByteArray(),
                AMQP.BasicProperties().builder().replyTo(responsesQueue).build(),
            )
            logger.debug("Sent heartbeat request for server {}", toWatchServerID)
            Thread.sleep(timeoutMillis)
            mutex.lock()
            if (!hasReplied) {
                replyMisses++
            } else {
                replyMisses = 0
            }
            if (replyMisses >= maxReplyMiss) {
                onFaultDetection(toWatchServerID)
                return
            }
            mutex.unlock()
        }
    }
}
