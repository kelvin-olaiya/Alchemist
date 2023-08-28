/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster.management

import com.google.protobuf.kotlin.toByteString
import it.unibo.alchemist.boundary.Loader
import it.unibo.alchemist.boundary.grid.cluster.AlchemistRemoteServer
import it.unibo.alchemist.boundary.grid.cluster.RemoteServer
import it.unibo.alchemist.boundary.grid.cluster.ServerMetadata
import it.unibo.alchemist.boundary.grid.cluster.storage.KVStore
import it.unibo.alchemist.boundary.grid.simulation.SimulationBatch
import it.unibo.alchemist.boundary.grid.simulation.SimulationConfig
import it.unibo.alchemist.boundary.grid.simulation.SimulationInitializer
import it.unibo.alchemist.proto.Cluster
import it.unibo.alchemist.proto.SimulationOuterClass.Simulation
import it.unibo.alchemist.proto.SimulationOuterClass.SimulationConfiguration
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.Map.copyOf
import java.util.Optional
import java.util.UUID

class ClusterManagerImpl(
    private val kvStore: KVStore,
) : ClusterInfoManagerClientFacade, ClusterInfoManagerServerFacade, AutoCloseable {

    private fun submitSimulationConfiguration(configuration: SimulationConfig): UUID {
        val protoConfig = SimulationConfiguration.newBuilder()
            .setEndStep(configuration.endStep)
            .setEndTime(configuration.endTime.toDouble())
            .putAllDependencies(configuration.dependencies.mapValues { it.value.toByteString() })
            .build()
        val simulationID = UUID.randomUUID()
        kvStore.put("$SIMULATIONS_KEY/$simulationID", protoConfig.toByteArray())
        return simulationID
    }

    private fun <T> submitJobs(
        simulationID: UUID,
        loader: Loader,
        initializers: Collection<SimulationInitializer>,
    ): List<UUID> {
        val jobIDs = mutableListOf<UUID>()
        initializers.forEach {
            val initializedEnvironment = loader.getWith<T, _>(it.variables)
            val protoJob = Simulation.newBuilder()
                .setSimulationID(simulationID.toString())
                .setEnvironment(serializeObject(initializedEnvironment.environment).toByteString())
                .setExports(serializeObject(initializedEnvironment.exporters).toByteString())
                .build()
            val jobID = UUID.randomUUID()
            kvStore.put("$JOBS_KEY/$jobID", protoJob.toByteArray())
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

    override fun submitSimulationBatch(batch: SimulationBatch): List<UUID> {
        val simulationID = submitSimulationConfiguration(batch.configuration)
        return submitJobs<Any>(simulationID, batch.configuration.loader, batch.initializers)
    }

    override val servers: Collection<RemoteServer> get() {
        return kvStore.get(SERVERS_KEY).map { Cluster.Registration.parseFrom(it.bytes) }.map {
            AlchemistRemoteServer(UUID.fromString(it.serverID), copyOf(it.metadataMap))
        }.toList()
    }

    override fun metadataOf(serverID: UUID): Optional<ServerMetadata> =
        Optional.ofNullable(servers.find { it.serverID == serverID }).map { it.metadata }

    override fun join(serverID: UUID, serverMetadata: ServerMetadata) {
        val serverData = Cluster.Registration.newBuilder()
            .setServerID(serverID.toString())
            .putAllMetadata(serverMetadata)
            .build()
        kvStore.put(asEtcdServerKey(serverID.toString()), serverData.toByteArray())
    }

    override fun leave(serverID: UUID) {
        kvStore.delete(asEtcdServerKey(serverID.toString()))
    }

    override fun close() {
        kvStore.close()
    }

    companion object {
        private const val SERVERS_KEY = "servers"
        private const val SIMULATIONS_KEY = "simulations"
        private const val INITIALIZERS_KEY = "initializers"
        private const val JOBS_KEY = "jobs"
        private const val RESULTS_KEY = "results"
        private fun asEtcdServerKey(serverID: String) = "${SERVERS_KEY}/$serverID"
    }
}
