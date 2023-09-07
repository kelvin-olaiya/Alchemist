/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid

import com.google.protobuf.ByteString
import it.unibo.alchemist.boundary.grid.cluster.ClusterImpl
import it.unibo.alchemist.boundary.grid.cluster.storage.KVStore
import it.unibo.alchemist.boundary.grid.communication.CommunicationQueues
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.declareQueue
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.registerQueueConsumer
import it.unibo.alchemist.boundary.grid.simulation.ObservableSimulation
import it.unibo.alchemist.core.Engine
import it.unibo.alchemist.core.Simulation
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.times.DoubleTime
import it.unibo.alchemist.proto.ClusterMessages
import it.unibo.alchemist.proto.SimulationMessage
import it.unibo.alchemist.proto.SimulationMessage.RunSimulation
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor

class AlchemistServer(
    private val serverID: UUID,
    private val storage: KVStore,
) {

    private val executor =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private val assignedJobs = mutableMapOf<UUID, Future<*>>()
    private val queues = CommunicationQueues(serverID)

    init {
        queues.jobOrders?.let {
            declareQueue(it)
            registerQueueConsumer(it) { _, delivery ->
                val jobID = UUID.fromString(RunSimulation.parseFrom(delivery.body).jobID)
                val simulation = ObservableSimulation(getSimulation<Any, _>(jobID), jobID)
                simulation.addCompletionCallback { println("simulation terminated") }
                val future = executor.submit(simulation)
                assignedJobs[jobID] = future
            }
        }
    }

    fun register(serverMetadata: Map<String, String> = mapOf()) {
        val serverData = ClusterMessages.Registration.newBuilder()
            .setServerID(serverID.toString())
            .putAllMetadata(serverMetadata)
            .build()
        storage.put(serverKey(serverID), serverData.toByteArray())
    }

    fun deregister() {
        storage.delete(serverKey(serverID))
    }

    val cluster get() = ClusterImpl(storage)

    @Suppress("UNCHECKED_CAST")
    private fun <T, P : Position<P>> getSimulation(jobID: UUID): Simulation<T, P> {
        val job = storage.get("$JOBS_KEY/$jobID").first().bytes
        val simulation = SimulationMessage.Simulation.parseFrom(job)
        val simulationID = simulation.simulationID
        val config = storage.get("$SIMULATIONS_KEY/$simulationID").first().bytes
        val simulationConfig = SimulationMessage.SimulationConfiguration.parseFrom(config)
        // save dependencies
        val environment: Environment<T, P> = deserializeObject(simulation.environment) as Environment<T, P>
        return Engine(environment, simulationConfig.endStep, DoubleTime(simulationConfig.endTime))
    }

    private fun deserializeObject(bytes: ByteString): Any {
        val byteStream = ByteArrayInputStream(bytes.toByteArray())
        val objectStream = ObjectInputStream(byteStream)
        val obj = objectStream.readObject()
        objectStream.close()
        return obj
    }

    val runningSimulations get() = (executor as ThreadPoolExecutor).activeCount

    private fun serverKey(serverID: UUID) = "$SERVERS_KEY/$serverID"

    companion object {
        const val SERVERS_KEY = "servers"
        const val SIMULATIONS_KEY = "simulations"
        const val JOBS_KEY = "jobs"
    }
}
