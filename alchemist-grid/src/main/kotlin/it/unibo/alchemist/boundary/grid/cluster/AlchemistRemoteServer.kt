/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster

import it.unibo.alchemist.boundary.grid.simulation.SimulationInitializer
import java.util.UUID

interface AlchemistRemoteServer {

    /**
     * The server unique id.
     */
    val serverID: UUID

    /**
     * Submit the parameters for the simulation with the provided [simulationID].
     * Return the job id for future reference.
     */
    fun submitJob(simulationID: UUID, parameters: SimulationInitializer): UUID
}
