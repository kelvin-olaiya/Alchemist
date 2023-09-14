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
import it.unibo.alchemist.boundary.grid.cluster.management.ObservableClusterRegistry
import it.unibo.alchemist.boundary.grid.cluster.storage.EtcdKVStore
import it.unibo.alchemist.boundary.grid.communication.RabbitmqConfig
import it.unibo.alchemist.boundary.grid.simulation.ComplexityImpl
import it.unibo.alchemist.boundary.grid.simulation.SimulationBatchImpl
import it.unibo.alchemist.boundary.grid.simulation.SimulationConfigImpl
import it.unibo.alchemist.boundary.grid.simulation.SimulationInitializer
import it.unibo.alchemist.boundary.launchers.ConfigurationProvider.getEtcdEndpoints
import it.unibo.alchemist.boundary.launchers.ConfigurationProvider.getRabbitmqConfig
import it.unibo.alchemist.model.Time
import org.slf4j.LoggerFactory
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory

/**
 * Launches a simulation set on a cluster of Alchemist nodes running in server mode.
 */
class DistributedExecution @JvmOverloads constructor(
    private val configurationPath: String,
    private val variables: List<String> = emptyList(),
    private val exportPath: String = createTempDirectory("alchemist-export").absolutePathString()
        .also { logger.warn("As no output folder is specified Alchemist will export data in $it") },
) : SimulationLauncher() {

    override fun launch(loader: Loader) {
        RabbitmqConfig.setUpConnection(getRabbitmqConfig(configurationPath))
        val cluster = ClusterImpl(ObservableClusterRegistry(EtcdKVStore(getEtcdEndpoints(configurationPath))))
        val configuration = SimulationConfigImpl(loader, Long.MAX_VALUE, Time.INFINITY)
        val initializers = loader.variables.cartesianProductOf(variables).map(::SimulationInitializer)
        val batch = SimulationBatchImpl(configuration, initializers)
        val workerSet = cluster.workerSet(ComplexityImpl())
        logger.debug("Distributing simulation batch")
        val result = workerSet.dispatchBatch(batch)
        if (result.numOfErrors == 0) {
            result.saveAllLocaly(exportPath)
        } else {
            logger.debug("Simulation batch encountered execution errors ({})", result.numOfErrors)
            result.results.forEach {
                if (it.error.isPresent) {
                    logger.debug(
                        "Error for job {}: {}",
                        it.jobDescriptor,
                        it.error.get().toString(),
                    )
                } else {
                    it.saveLocally(exportPath)
                }
            }
        }
        logger.debug("Simulation batch completed")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DistributedExecution::class.java)
    }
}
