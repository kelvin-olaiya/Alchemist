/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.simulation

interface BatchResult {

    /**
     * Number of errors encountered during
     * the execution of the simulation batch.
     */
    val numOfErrors: Int

    /**
     * A collection of result of the simulations that completed either
     * successfully or with error
     */
    val results: Collection<SimulationResult>

    /**
     * Utility function to save all result files locally
     */
    fun saveAllLocaly(exportPath: String)
}
