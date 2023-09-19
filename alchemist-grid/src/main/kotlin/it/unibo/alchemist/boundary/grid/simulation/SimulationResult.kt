/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.simulation

import java.util.Optional
import java.util.UUID

interface SimulationResult {

    /**
     * The id of the job this result is related to.
     */
    val jobID: UUID

    /**
     * The descriptor of the job this result is related to.
     */
    val jobDescriptor: String

    /**
     * An optional which is filled with the exception that may have been thrown
     * during simulation execution.
     */
    val error: Optional<Throwable>

    /**
     * Save the result of the simulation locally.
     */
    fun saveLocally(exportPath: String)
}
