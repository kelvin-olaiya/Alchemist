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
import it.unibo.alchemist.boundary.grid.communication.RabbitmqConfig.channel
import it.unibo.alchemist.boundary.grid.communication.ServerQueues.HEALTH_QUEUE_METADATA_KEY
import it.unibo.alchemist.proto.Cluster.HealthCheckRequest
import it.unibo.alchemist.proto.Cluster.HealthCheckResponse
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ClusterHealthChecker(
    private val clusterManager: ClusterInfoManagerServerFacade,
    val timeoutMillis: Long,
    val maxReplyMiss: Int,
) : Runnable {
    private val responsesQueue = channel.queueDeclare().queue
    private val healthCheckRequestMessage = HealthCheckRequest.newBuilder().setReplyTo(responsesQueue).build()
    private var callbackRegistered = false

    override fun run() {
        val failingHistory = ConcurrentHashMap<String, Int>()
        val pendingRequests = Collections.synchronizedList(mutableListOf<String>())
        while (true) {
            val servers = clusterManager.servers
            pendingRequests.clear()
            servers.forEach { pendingRequests.add(it.serverID.toString()) }
            if (!callbackRegistered) {
                channel.basicConsume(responsesQueue, true, { _, delivery ->
                    val response = HealthCheckResponse.parseFrom(delivery.body)
                    pendingRequests.remove(response.serverID)
                }, { _ -> })
                callbackRegistered = true
            }
            servers.forEach {
                channel.basicPublish(
                    "",
                    it.metadata[HEALTH_QUEUE_METADATA_KEY],
                    AMQP.BasicProperties().builder().replyTo(responsesQueue).build(),
                    healthCheckRequestMessage.toByteArray(),
                )
            }
            Thread.sleep(timeoutMillis)
            pendingRequests.forEach {
                failingHistory.compute(it) { _, v -> v?.plus(1) ?: 1 }
            }
            val diedServers = failingHistory.filter { it.value >= maxReplyMiss }.map { it.key }.toSet()
            diedServers.forEach {
                clusterManager.leave(UUID.fromString(it))
                failingHistory.remove(it)
            }
        }
    }
}
