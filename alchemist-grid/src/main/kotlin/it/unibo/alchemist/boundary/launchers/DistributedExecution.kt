/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.launchers

import it.unibo.alchemist.boundary.Loader
import it.unibo.alchemist.boundary.grid.cluster.ClusterImpl
import it.unibo.alchemist.boundary.grid.cluster.storage.EtcdKVStore
import it.unibo.alchemist.boundary.grid.simulation.ComplexityImpl
import it.unibo.alchemist.boundary.grid.simulation.SimulationBatchImpl
import it.unibo.alchemist.boundary.grid.simulation.SimulationConfigFactory
import it.unibo.alchemist.boundary.grid.simulation.SimulationInitializer
import it.unibo.alchemist.model.Time
import org.slf4j.LoggerFactory

/**
 * Launches a simulation set on a cluster of Alchemist nodes running in server mode.
 */
class DistributedExecution(
    private val variables: List<String> = emptyList(),
    private val distributedConfigPath: String? = null,
) : SimulationLauncher() {

    private val logger = LoggerFactory.getLogger(DistributedExecution::class.java)

    override fun launch(loader: Loader) {
        val endpoints = listOf("http://localhost:10001", "http://localhost:10002", "http://localhost:10003")
        val cluster = ClusterImpl(EtcdKVStore(endpoints))
        val configuration = SimulationConfigFactory.newSimulationConfig(loader, Long.MAX_VALUE, Time.INFINITY)
        val initializers = loader.variables.cartesianProductOf(variables).map(::SimulationInitializer)
        val batch = SimulationBatchImpl(configuration, initializers)
        val workerSet = cluster.workerSet(ComplexityImpl())
        workerSet.dispatchBatch(batch)
        logger.debug("Simulation batch has been distributed")
    }
}
