/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.simulation

import it.unibo.alchemist.boundary.Loader
import it.unibo.alchemist.model.Time

interface SimulationContext {

    /**
     * The loader for this simulation context.
     */
    val loader: Loader

    /**
     * The end step for this simulation context.
     */
    val endStep: Double

    /**
     * The end time for this simulation context.
     */
    val endTime: Time

    /**
     * A mapping between file names and their contents
     * necessary for the simulation.
     */
    val dependencies: Map<String, ByteArray>
}
