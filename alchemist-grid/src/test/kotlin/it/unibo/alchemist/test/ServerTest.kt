/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.test

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import it.unibo.alchemist.boundary.grid.cluster.ClusterImpl
import it.unibo.alchemist.boundary.grid.communication.CommunicationQueues
import it.unibo.alchemist.boundary.grid.communication.RabbitmqConfig.setUpConnection
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.declareQueue
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.publishToQueue
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.registerQueueConsumer
import it.unibo.alchemist.boundary.launchers.ConfigurationProvider.getRabbitmqConfig
import it.unibo.alchemist.proto.ClusterMessages.HealthCheckRequest
import it.unibo.alchemist.proto.ClusterMessages.HealthCheckResponse
import it.unibo.alchemist.test.utils.DistributionTestUtils
import it.unibo.alchemist.test.utils.DistributionTestUtils.awaitServerJoin
import it.unibo.alchemist.test.utils.DistributionTestUtils.startServers
import it.unibo.alchemist.test.utils.DistributionTestUtils.use
import it.unibo.alchemist.test.utils.TestConstants.composeFilePath
import it.unibo.alchemist.test.utils.TestConstants.distributionConfigurationFile
import it.unibo.alchemist.test.utils.TestConstants.registry
import it.unibo.alchemist.test.utils.TestConstants.serverConfigFile
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class ServerTest : StringSpec({

    extensions(DistributionTestUtils.getDockerExtension(composeFilePath))

    "server responds to health check request" {
        var replyed = false
        startServers(serverConfigFile, 1).use {
            val cluster = ClusterImpl(registry)
            awaitServerJoin(cluster, 1, 10.seconds)
            val serverID = registry.nodes.first().serverID
            setUpConnection(getRabbitmqConfig(distributionConfigurationFile))
            val responseQueue = declareQueue()
            registerQueueConsumer(responseQueue) { _, delivery ->
                val response = HealthCheckResponse.parseFrom(delivery.body)
                if (UUID.fromString(response.serverID) == serverID) {
                    replyed = true
                }
            }
            val healthCheckMessage = HealthCheckRequest.newBuilder().setReplyTo(responseQueue).build()
            publishToQueue(CommunicationQueues.HEALTH.of(serverID), responseQueue, healthCheckMessage.toByteArray())
            eventually(5.seconds) {
                replyed.shouldBeTrue()
            }
        }
    }

    "Server checks and removes faulty servers" {
        startServers(serverConfigFile, 1).use {
            val cluster = ClusterImpl(registry)
            awaitServerJoin(cluster, 1, 10.seconds)
            registry.addServer(UUID.randomUUID(), mapOf())
            registry.nodes shouldHaveSize 2
            eventually(10.seconds) {
                registry.nodes shouldHaveSize 1
            }
        }
    }
})
