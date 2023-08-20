/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.simulation

import java.io.Serializable

/**
 * A single simulation initializer. It holds the necessary information (a combination of variables)
 * to create a simulation instance.
 *
 * [variables] a combination of variables. Typically, derived from a cartesian product.
 */
data class SimulationInitializer(val variables: Map<String, Serializable>)
