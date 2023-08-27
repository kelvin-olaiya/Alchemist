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

class AlchemistRemoteServer(
    override val serverID: UUID,
    override val metadata: Map<String, String>,
) : RemoteServer {

    override fun submitJob(simulationID: UUID, parameters: SimulationInitializer): UUID {
        TODO("Not yet implemented")
    }
}
