/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster

import com.google.protobuf.kotlin.toByteString
import it.unibo.alchemist.boundary.Loader
import it.unibo.alchemist.boundary.grid.cluster.storage.KVStore
import it.unibo.alchemist.boundary.grid.communication.CommunicationQueues
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.publishToQueue
import it.unibo.alchemist.boundary.grid.simulation.SimulationBatch
import it.unibo.alchemist.boundary.grid.simulation.SimulationConfig
import it.unibo.alchemist.boundary.grid.simulation.SimulationInitializer
import it.unibo.alchemist.proto.SimulationMessage
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.UUID

class BatchDispatcher(
    override val nodes: Collection<ClusterNode>,
    private val dispatchStrategy: DispatchStrategy,
    private val storage: KVStore,
) : Dispatcher {

    override fun dispatchBatch(batch: SimulationBatch): Collection<UUID> {
        val jobIDs = submitSimulationBatch(batch)
        val assignements = dispatchStrategy.makeAssignments(nodes.toList(), jobIDs)
        assignements.entries.forEach {
            val jobQueue = CommunicationQueues.JOBS.of(it.key.serverID)
            it.value.forEach { jobID ->
                val message = SimulationMessage.RunSimulation.newBuilder().setJobID(jobID.toString()).build()
                publishToQueue(jobQueue, message.toByteArray())
            }
        }
        return jobIDs
    }

    private fun submitSimulationBatch(batch: SimulationBatch): List<UUID> {
        val simulationID = submitSimulationConfiguration(batch.configuration)
        return submitJobs(simulationID, batch.configuration.loader, batch.initializers)
    }

    private fun submitSimulationConfiguration(configuration: SimulationConfig): UUID {
        val protoConfig = SimulationMessage.SimulationConfiguration.newBuilder()
            .setEndStep(configuration.endStep)
            .setEndTime(configuration.endTime.toDouble())
            .putAllDependencies(configuration.dependencies.mapValues { it.value.toByteString() })
            .build()
        val simulationID = UUID.randomUUID()
        storage.put("$SIMULATIONS_KEY/$simulationID", protoConfig.toByteArray())
        return simulationID
    }

    private fun submitJobs(
        simulationID: UUID,
        loader: Loader,
        initializers: Collection<SimulationInitializer>,
    ): List<UUID> {
        val jobIDs = mutableListOf<UUID>()
        initializers.forEach {
            val initializedEnvironment = loader.getWith<Any, _>(it.variables)
            val serializedEnvironment = serializeObject(initializedEnvironment.environment).toByteString()
            val serializedExporters = serializeObject(initializedEnvironment.exporters).toByteString()
            val protoJob = SimulationMessage.Simulation.newBuilder()
                .setSimulationID(simulationID.toString())
                .setEnvironment(serializedEnvironment)
                .setExports(serializedExporters)
                .build()
            val jobID = UUID.randomUUID()
            storage.put("$JOBS_KEY/$jobID", protoJob.toByteArray())
            jobIDs.add(jobID)
        }
        return jobIDs
    }

    private fun serializeObject(obj: Any): ByteArray {
        val byteStream = ByteArrayOutputStream()
        val objectStream = ObjectOutputStream(byteStream)
        objectStream.writeObject(obj)
        objectStream.close()
        return byteStream.toByteArray()
    }

    companion object {
        const val SIMULATIONS_KEY = "simulations"
        const val JOBS_KEY = "jobs"
    }
}
