/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.simulation

class SimulationBatchImpl(
    override val configuration: SimulationConfig,
    override val initializers: Collection<SimulationInitializer>,
) : SimulationBatch {

    override val complexity: Complexity
        get() = ComplexityImpl(DEFAULT_RAM_COMPLEXITY, DEFAULT_CPU_COMPLEXITY)

    companion object {
        private const val DEFAULT_CPU_COMPLEXITY = 0.0
        private const val DEFAULT_RAM_COMPLEXITY = 0.0
    }
}
