/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster.management

import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteString
import it.unibo.alchemist.boundary.Exporter
import it.unibo.alchemist.boundary.Loader
import it.unibo.alchemist.boundary.exporters.GlobalExporter
import it.unibo.alchemist.boundary.grid.cluster.AlchemistClusterNode
import it.unibo.alchemist.boundary.grid.cluster.ClusterNode
import it.unibo.alchemist.boundary.grid.cluster.storage.KVStore
import it.unibo.alchemist.boundary.grid.simulation.JobStatus
import it.unibo.alchemist.boundary.grid.simulation.SimulationBatch
import it.unibo.alchemist.boundary.grid.simulation.SimulationConfig
import it.unibo.alchemist.boundary.grid.simulation.SimulationInitializer
import it.unibo.alchemist.core.Engine
import it.unibo.alchemist.core.Simulation
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.times.DoubleTime
import it.unibo.alchemist.proto.ClusterMessages
import it.unibo.alchemist.proto.SimulationMessage
import it.unibo.alchemist.proto.SimulationMessage.Assignment
import it.unibo.alchemist.proto.SimulationMessage.SimulationResult
import it.unibo.alchemist.proto.SimulationMessage.SimulationStatus
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.UUID

class ClusterRegistry(
    private val storage: KVStore,
) : Registry {
    override fun addServer(serverID: UUID, metadata: Map<String, String>) {
        val serverData = ClusterMessages.Registration.newBuilder()
            .setServerID(serverID.toString())
            .putAllMetadata(metadata)
            .build()
        storage.put(KEYS.SERVERS.make(serverID), serverData.toByteArray())
    }

    override fun removeServer(serverID: UUID) {
        storage.delete(KEYS.SERVERS.make(serverID))
    }

    override val nodes: Collection<ClusterNode>
        get() = storage
            .get(KEYS.SERVERS.prefix)
            .map { ClusterMessages.Registration.parseFrom(it.bytes) }
            .map { AlchemistClusterNode(UUID.fromString(it.serverID), java.util.Map.copyOf(it.metadataMap)) }
            .toList()

    override fun submitBatch(batch: SimulationBatch): Pair<UUID, Map<UUID, SimulationInitializer>> {
        val simulationID = submitSimulationConfiguration(batch.configuration)
        return simulationID to createJobs(simulationID, batch.configuration.loader, batch.initializers)
    }

    private fun submitSimulationConfiguration(configuration: SimulationConfig): UUID {
        val protoConfig = SimulationMessage.SimulationConfiguration.newBuilder()
            .setEndStep(configuration.endStep)
            .setEndTime(configuration.endTime.toDouble())
            .putAllDependencies(configuration.dependencies.mapValues { it.value.toByteString() })
            .build()
        val simulationID = UUID.randomUUID()
        storage.put(KEYS.SIMULATIONS.make(simulationID), protoConfig.toByteArray())
        return simulationID
    }

    private fun createJobs(
        simulationID: UUID,
        loader: Loader,
        initializers: Collection<SimulationInitializer>,
    ): Map<UUID, SimulationInitializer> {
        val jobIDToInitializers = mutableMapOf<UUID, SimulationInitializer>()
        initializers.forEach {
            val initializedEnvironment = loader.getWith<Any, _>(it.variables)
            val serializedEnvironment = serializeObject(initializedEnvironment.environment).toByteString()
            val serializedExporters = serializeObject(initializedEnvironment.exporters).toByteString()
            val simulation = SimulationMessage.Simulation.newBuilder()
                .setSimulationID(simulationID.toString())
                .setEnvironment(serializedEnvironment)
                .setExports(serializedExporters)
                .build()
            val jobID = UUID.randomUUID()
            storage.put(KEYS.JOBS.make(jobID), simulation.toByteArray())
            jobIDToInitializers[jobID] = it
        }
        return jobIDToInitializers
    }

    override fun deleteSimulation(simulationID: UUID) {
        storage.delete(KEYS.SIMULATIONS.make(simulationID))
        storage.delete(KEYS.ASSIGNMENTS.make(simulationID), true)
        storage.delete(KEYS.RESULTS.make(simulationID), true)
        simulationJobs(simulationID).forEach {
            storage.delete(KEYS.JOBS.make(it))
            storage.delete(KEYS.JOB_STATUS.make(it))
        }
    }

    override fun simulationID(jobID: UUID): UUID {
        val job = getJob(jobID)
        return UUID.fromString(job.simulationID)
    }

    override fun simulationJobs(simulationID: UUID): Collection<UUID> {
        return storage.getKeys(KEYS.JOBS.prefix)
            .map { it.removePrefix(KEYS.JOBS.prefix) }
            .map { UUID.fromString(it) }
            .map { it to getJob(it) }
            .filter { UUID.fromString(it.second.simulationID) == simulationID }
            .map { it.first }
    }

    override fun assignJob(jobID: UUID, serverID: UUID) {
        val simulationID = simulationID(jobID)
        val assignment = Assignment.newBuilder()
            .setSimulationID(simulationID.toString())
            .setJobID(jobID.toString())
            .setServerID(serverID.toString())
            .build()
        storage.put(KEYS.ASSIGNMENTS.make(simulationID, jobID), assignment.toByteArray())
    }

    override fun unassignJob(jobID: UUID) {
        val simulationID = simulationID(jobID)
        storage.delete(KEYS.ASSIGNMENTS.make(simulationID.toString(), jobID.toString()))
    }

    override fun reassignJob(jobID: UUID, serverID: UUID) {
        unassignJob(jobID)
        assignJob(jobID, serverID)
    }

    override fun assignedTo(jobID: UUID): UUID {
        val simulationID = simulationID(jobID)
        val assignmentBytes = storage.get(KEYS.ASSIGNMENTS.make(simulationID.toString(), jobID.toString()))
            .first()
            .bytes
        val assignment = Assignment.parseFrom(assignmentBytes)
        return UUID.fromString(assignment.serverID)
    }

    override fun assignedJobs(serverID: UUID): Collection<UUID> {
        return storage.get(KEYS.ASSIGNMENTS.prefix)
            .map { Assignment.parseFrom(it.bytes) }
            .filter { UUID.fromString(it.serverID) == serverID }
            .map { UUID.fromString(it.jobID) }
    }

    override fun assignedJobs(serverID: UUID, simulationID: UUID): Collection<UUID> {
        return simulationAssignments(simulationID)[serverID] ?: emptyList()
    }

    override fun simulationAssignments(simulationID: UUID): Map<UUID, Collection<UUID>> {
        return storage.get(KEYS.ASSIGNMENTS.make(simulationID))
            .map { Assignment.parseFrom(it.bytes) }
            .groupBy { UUID.fromString(it.serverID) }
            .mapValues { assignments -> assignments.value.map { UUID.fromString(it.jobID) } }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T, P : Position<P>> simulationByJobId(jobID: UUID): Simulation<T, P> {
        val job = getJob(jobID)
        val simulationID = job.simulationID
        val config = storage.get(KEYS.SIMULATIONS.make(simulationID)).first().bytes
        val simulationConfig = SimulationMessage.SimulationConfiguration.parseFrom(config)
        // save dependencies
        val environment: Environment<T, P> = deserializeObject(job.environment) as Environment<T, P>
        val exports = deserializeObject(job.exports) as List<Exporter<T, P>>
        val engine = Engine(environment, simulationConfig.endStep, DoubleTime(simulationConfig.endTime))
        engine.addOutputMonitor(GlobalExporter(exports))
        return engine
    }

    private fun getJob(jobID: UUID): SimulationMessage.Simulation {
        val job = storage.get(KEYS.JOBS.make(jobID)).first().bytes
        return SimulationMessage.Simulation.parseFrom(job)
    }

    override fun jobStatus(jobID: UUID): Pair<JobStatus, UUID> {
        val simulationStatus = storage.get(KEYS.JOB_STATUS.make(jobID)).first().bytes
        val status = SimulationStatus.parseFrom(simulationStatus)
        return JobStatus.fromProto(status.status) to UUID.fromString(status.serverID)
    }

    override fun setJobStatus(serverID: UUID, jobID: UUID, status: JobStatus) {
        val simulationStatus = SimulationStatus.newBuilder()
            .setStatus(status.proto)
            .setServerID(serverID.toString())
            .build()
        storage.put(KEYS.JOB_STATUS.make(jobID), simulationStatus.toByteArray())
    }

    override fun addResult(jobID: UUID, name: String, result: ByteArray) {
        val simulationResult = SimulationResult.newBuilder()
            .setName(name)
            .setContent(result.toByteString())
            .build()
        val simulationID = simulationID(jobID)
        storage.put(KEYS.RESULTS.make(simulationID.toString(), jobID.toString(), name), simulationResult.toByteArray())
    }

    override fun resultsByJobID(jobID: UUID): Collection<Pair<String, ByteArray>> {
        return simulationResults(simulationID(jobID))
            .filter { UUID.fromString(it.jobID) == jobID }
            .map { it.name to it.content.toByteArray() }
    }

    override fun resultsBySimulationID(simulationID: UUID): Collection<Pair<String, ByteArray>> {
        return simulationResults(simulationID).map { it.name to it.content.toByteArray() }
    }

    private fun simulationResults(simulationID: UUID): Collection<SimulationResult> {
        return storage.get(KEYS.RESULTS.make(simulationID)).map { SimulationResult.parseFrom(it.bytes) }
    }

    override fun isComplete(simulationID: UUID): Boolean {
        val jobIDs = simulationJobs(simulationID)
        return jobIDs.map { jobStatus(it) }.all { it.first in setOf(JobStatus.DONE, JobStatus.FAILED) }
    }

    private fun serializeObject(obj: Any): ByteArray {
        val byteStream = ByteArrayOutputStream()
        val objectStream = ObjectOutputStream(byteStream)
        objectStream.writeObject(obj)
        objectStream.close()
        return byteStream.toByteArray()
    }

    private fun deserializeObject(bytes: ByteString): Any {
        val byteStream = ByteArrayInputStream(bytes.toByteArray())
        val objectStream = ObjectInputStream(byteStream)
        val obj = objectStream.readObject()
        objectStream.close()
        return obj
    }

    companion object {
        internal enum class KEYS(val topic: String) {
            SERVERS("servers"), // ~/serverID
            SIMULATIONS("simulations"), // ~/simulationID
            JOBS("jobs"), // ~/jobID
            JOB_STATUS("jobstatus"), // ~/jobID
            ASSIGNMENTS("assignments"), // ~/simulationID/jobID
            RESULTS("results"), // ~/simulationID/jobID/name
            ;
            fun make(vararg keys: String) = "$topic/${keys.joinToString("/")}"
            fun make(vararg keys: UUID) = make(*keys.map { it.toString() }.toTypedArray())
            val prefix get() = "$topic/"
        }
    }
}
