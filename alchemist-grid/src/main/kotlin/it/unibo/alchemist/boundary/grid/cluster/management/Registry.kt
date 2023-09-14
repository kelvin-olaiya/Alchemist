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
import it.unibo.alchemist.boundary.grid.utils.WorkingDirectory
import it.unibo.alchemist.core.Simulation
import it.unibo.alchemist.model.Position
import java.util.Optional
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
     * Returns the simulationID of the simulations submitted in the cluster.
     */
    fun simulations(): Collection<UUID>

    /**
     * Get all the job of the provided [simulationID].
     */
    fun simulationJobs(simulationID: UUID): Collection<UUID>

    /**
     * Register job assignments.
     */
    fun assignJob(jobID: UUID, serverID: UUID)

    /**
     * Register job unassignment.
     */
    fun unassignJob(jobID: UUID)

    /**
     * Register job reassignment.
     */
    fun reassignJob(jobID: UUID, serverID: UUID)

    /**
     * Get the serverID of the server to which the job is assigned.
     */
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
     * Get the working directory for the specified job.
     * It will possibly contain the simulation file dependencies.
     */
    fun getJobWorkingDirectory(jobID: UUID): WorkingDirectory

    /**
     * Get the job descriptor (variables names anf values) relative to the [jobID].
     */
    fun getJobDescriptor(jobID: UUID): String

    /**
     * Get the status of the [jobID].
     */
    fun jobStatus(jobID: UUID): Pair<JobStatus, UUID>

    /**
     * Set the [jobID] status.
     */
    fun setJobStatus(serverID: UUID, jobID: UUID, status: JobStatus)

    /**
     * Register the failure of the job execution.
     *
     * @param serverID the server who encountered the failure.
     * @param jobID the job that failed.
     * @param error the Exception thrown.
     */
    fun setJobFailure(serverID: UUID, jobID: UUID, error: Throwable)

    /**
     * @return an optional which is empty if no exception was
     * thrown during the execution of the job or is empty otherwise
     */
    fun jobError(jobID: UUID): Optional<Throwable>

    /**
     * Submit a new result.
     *
     * @param jobID the id of the job to which the result is related.
     * @param name the name (more likely the filename) of the job result.
     * @param result a sequence of bytes representing the result.
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
     * Removes simulation related results from the registry.
     */
    fun clearResults(simulationID: UUID)

    /**
     * Check if all the simulation have completed either with success or with errors.
     */
    fun isComplete(simulationID: UUID): Boolean
}
