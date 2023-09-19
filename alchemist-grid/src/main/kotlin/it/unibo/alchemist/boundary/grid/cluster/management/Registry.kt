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
import java.io.Closeable
import java.util.Optional
import java.util.UUID

/**
 * Manages the cluster functional information.
 */
interface Registry : Closeable, AutoCloseable {

    /**
     * Registers a new server.
     */
    fun addServer(serverID: UUID, metadata: Map<String, String>)

    /**
     * Removes a server from the registry
     */
    fun removeServer(serverID: UUID)

    /**
     * Returns a collection of server nodes
     */
    val nodes: Collection<ClusterNode>

    /**
     * Submits the simulation configuration and create a job for each
     * initializer in the batch.
     *
     * @return a [Pair] in which the first field is the simulationID assigned
     * to the simulation configuration, and the second field is a
     * Collection of UUID corresponding to the simulation initializers.
     */
    fun submitBatch(batch: SimulationBatch): Pair<UUID, Map<UUID, SimulationInitializer>>

    /**
     * Removes all the simulation related information from the registry.
     */
    fun deleteSimulation(simulationID: UUID)

    /**
     * Get the simulationID of the provided [jobID].
     *
     * @throws NoSuchElementException if no job is found with
     * the [jobID].
     */
    fun simulationID(jobID: UUID): UUID

    /**
     * Returns a collection simulationIDs of all the simulations
     * submitted in the registry.
     */
    fun simulations(): Collection<UUID>

    /**
     * Get all the jobs related to the provided [simulationID].
     *
     * @throws NoSuchElementException if no simulation is found with
     * the [simulationID].
     */
    fun simulationJobs(simulationID: UUID): Collection<UUID>

    /**
     * Registers the assignment of job identified by [jobID] to the server identified
     * by [serverID] assignments.
     */
    fun assignJob(jobID: UUID, serverID: UUID)

    /**
     * Removes the assignment of the job identified by [jobID].
     */
    fun unassignJob(jobID: UUID)

    /**
     * Reassigns the job identified by [jobID] to another server
     * identified by [serverID].
     */
    fun reassignJob(jobID: UUID, serverID: UUID)

    /**
     * Gets the serverID of the server to which the job identified by [jobID]
     * is assigned.
     * @throws NoSuchElementException if no job is found with
     * the [jobID].
     */
    fun assignedTo(jobID: UUID): UUID

    /**
     * Gets all assigned job to the server identified by [serverID].
     */
    fun assignedJobs(serverID: UUID): Collection<UUID>

    /**
     * Returns a collection of ids of the jobs assigned to the server identified by [serverID] related
     * to the simulation identified by [simulationID].
     */
    fun assignedJobs(serverID: UUID, simulationID: UUID): Collection<UUID>

    /**
     * Returns a map in which a serverID is mapped to a collection
     * of ids of the jobs assigned to the server it identifies.
     *
     * @throws NoSuchElementException if no simulation is found with
     * the [simulationID].
     */
    fun simulationAssignments(simulationID: UUID): Map<UUID, Collection<UUID>>

    /**
     * Returns an instance of a simulation that will be built from the
     * simulation configuration and the simulation initializers relative
     * to the job identified by [jobID].
     *
     * @throws NoSuchElementException if no job is found with
     * the [jobID].
     */
    fun <T, P : Position<P>> simulationByJobId(jobID: UUID): Simulation<T, P>

    /**
     * Returns the working directory for the specified job.
     * It will possibly contain the simulation file dependencies.
     *
     * @throws NoSuchElementException if no job is found with
     * the [jobID].
     */
    fun getJobWorkingDirectory(jobID: UUID): WorkingDirectory

    /**
     * Returns the job descriptor (variables names and values) for the job identified by [jobID].
     *
     * @throws NoSuchElementException if no job is found with
     * the [jobID].
     */
    fun getJobDescriptor(jobID: UUID): String

    /**
     * Get the status of the job identified by [jobID].
     *
     * @throws NoSuchElementException if no job is found with
     * the [jobID].
     */
    fun jobStatus(jobID: UUID): Pair<JobStatus, UUID>

    /**
     * Sets the [JobStatus] of the job identified by [jobID].
     */
    fun setJobStatus(serverID: UUID, jobID: UUID, status: JobStatus)

    /**
     * Registers the failure of the job execution.
     *
     * @param serverID the server who encountered the failure.
     * @param jobID the job that failed.
     * @param error the Exception thrown.
     */
    fun setJobFailure(serverID: UUID, jobID: UUID, error: Throwable)

    /**
     * Returns an [Optional] that will be possibly filled with the exception
     * thrown during the execution of the job or is empty otherwise
     *
     * @throws NoSuchElementException if no job is found with
     * the [jobID].
     */
    fun jobError(jobID: UUID): Optional<Throwable>

    /**
     * Submits a simulation result.
     *
     * @param jobID the id of the job to which the result is related.
     * @param name the name (more likely the filename) of the job result.
     * @param result a sequence of bytes representing the result.
     */
    fun addResult(jobID: UUID, name: String, result: ByteArray)

    /**
     * Returns all the results related to the job identified by [jobID].
     *
     * @throws NoSuchElementException if no job is found with
     * the [jobID].
     */
    fun resultsByJobID(jobID: UUID): Collection<Pair<String, ByteArray>>

    /**
     * Get all the results related to the simulation identified by [simulationID].
     *
     * @throws NoSuchElementException if no simulation is found with
     * the [simulationID].
     */
    fun resultsBySimulationID(simulationID: UUID): Collection<Pair<String, ByteArray>>

    /**
     * Removes simulation related results from the registry.
     */
    fun clearResults(simulationID: UUID)

    /**
     * Check if all the simulation have completed either with success or with errors.
     *
     * @throws NoSuchElementException if no simulation is found with
     * the [simulationID].
     */
    fun isComplete(simulationID: UUID): Boolean
}
