/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster.management

import it.unibo.alchemist.boundary.grid.cluster.ClusterNode
import it.unibo.alchemist.boundary.grid.simulation.JobStatus
import it.unibo.alchemist.boundary.grid.simulation.SimulationBatch
import it.unibo.alchemist.boundary.grid.simulation.SimulationInitializer
import it.unibo.alchemist.core.Simulation
import it.unibo.alchemist.model.Position
import java.util.UUID

/**
 * Manages the cluster functional information.
 */
interface Registry {

    /**
     * Registers a new server.
     */
    fun addServer(serverID: UUID, metadata: Map<String, String>)

    /**
     * Remove a server from the registry
     */
    fun removeServer(serverID: UUID)

    /**
     * Return a collection of server nodes
     */
    val nodes: Collection<ClusterNode>

    /**
     * Submits the simulation configuration and create a job for each
     * initializer in the batch.
     *
     * @return a [Pair] in which the first field is the simulationID and the second is
     * Collection of UUID related to the simulation initializers.
     */
    fun submitBatch(batch: SimulationBatch): Pair<UUID, Map<UUID, SimulationInitializer>>

    /**
     * Removes all the simulation related information from the registry.
     */
    fun deleteSimulation(simulationID: UUID)

    /**
     * Get the simulationID of the provided [jobID].
     */
    fun simulationID(jobID: UUID): UUID

    /**
     * Get all the job of the provided [simulationID].
     */
    fun simulationJobs(simulationID: UUID): Collection<UUID>

    fun assignJob(jobID: UUID, serverID: UUID)

    fun unassignJob(jobID: UUID)

    fun reassignJob(jobID: UUID, serverID: UUID)

    fun assignedTo(jobID: UUID): UUID

    /**
     * Get all assigned job to the [serverID].
     */
    fun assignedJobs(serverID: UUID): Collection<UUID>

    /**
     * Get the [simulationID] assigned job to the [serverID].
     */
    fun assignedJobs(serverID: UUID, simulationID: UUID): Collection<UUID>

    /**
     * Get a mapping between serverID and assigned jobID.
     */
    fun simulationAssignments(simulationID: UUID): Map<UUID, Collection<UUID>>

    /**
     * Get an instance of the [jobID] simulation.
     */
    fun <T, P : Position<P>> simulationByJobId(jobID: UUID): Simulation<T, P>

    /**
     * Get the status of the [jobID].
     */
    fun jobStatus(jobID: UUID): Pair<JobStatus, UUID>

    /**
     * Set the [jobID] status.
     */
    fun setJobStatus(serverID: UUID, jobID: UUID, status: JobStatus)

    /**
     * Submit a new result.
     */
    fun addResult(jobID: UUID, name: String, result: ByteArray)

    /**
     * Get all results related to the [jobID].
     */
    fun resultsByJobID(jobID: UUID): Collection<Pair<String, ByteArray>>

    /**
     * Get all the results related to the [simulationID].
     */
    fun resultsBySimulationID(simulationID: UUID): Collection<Pair<String, ByteArray>>

    /**
     * Check if all the simulation have completed either with success or with errors.
     */
    fun isComplete(simulationID: UUID): Boolean
}
