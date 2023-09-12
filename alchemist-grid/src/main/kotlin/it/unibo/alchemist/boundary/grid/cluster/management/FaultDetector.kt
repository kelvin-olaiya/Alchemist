/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster.management

import it.unibo.alchemist.boundary.grid.communication.CommunicationQueues
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.declareQueue
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.deleteQueue
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.deregisterQueueConsumer
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.registerQueueConsumer
import it.unibo.alchemist.proto.ClusterMessages
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Predicate

class FaultDetector(
    private val timeoutMillis: Long,
    private val maxReplyMiss: Int,
) : Predicate<UUID> {

    override fun test(serverID: UUID): Boolean {
        val mutex = ReentrantLock()
        val responsesQueue = declareQueue()
        val hearthbeatMessage =
            ClusterMessages.HealthCheckRequest.newBuilder().setReplyTo(responsesQueue).build()
        var hasReplied = false
        var replyMisses = 0
        val consumerTag = registerQueueConsumer(responsesQueue) { _, delivery ->
            val response = ClusterMessages.HealthCheckResponse.parseFrom(delivery.body)
            logger.debug("Received heartbeat response from server {}", serverID)
            if (response.serverID == serverID.toString()) {
                mutex.lock()
                hasReplied = true
                mutex.unlock()
            }
        }
        logger.debug("Checking server {} liveness", serverID)
        while (!hasReplied && replyMisses < maxReplyMiss) {
            RabbitmqUtils.publishToQueue(
                CommunicationQueues.HEALTH.of(serverID),
                responsesQueue,
                hearthbeatMessage.toByteArray(),
            )
            logger.debug("Sent heartbeat request for server {}", serverID)
            Thread.sleep(timeoutMillis)
            mutex.lock()
            if (!hasReplied) {
                replyMisses++
            }
            mutex.unlock()
        }
        deregisterQueueConsumer(consumerTag)
        deleteQueue(responsesQueue)
        return hasReplied
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FaultDetector::class.java)
    }
}
