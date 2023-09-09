/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.simulation

import it.unibo.alchemist.boundary.grid.cluster.management.ObservableRegistry
import java.util.UUID

class ResultObserver(
    val simulationID: UUID,
    private val registry: ObservableRegistry,
) {
    fun saveLocally(jobID: UUID) {
    }
}
