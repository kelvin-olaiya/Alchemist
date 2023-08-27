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
import org.slf4j.LoggerFactory

/**
 * Launches a simulation set on a cluster of Alchemist nodes running in server mode.
 */
class DistributedExecution(
    private val variables: List<String> = emptyList(),
    private val distributedConfigPath: String?,
) : SimulationLauncher() {

    private val logger = LoggerFactory.getLogger(DistributedExecution::class.java)

    override fun launch(loader: Loader) {
        logger.debug("batch distributed")
    }
}
