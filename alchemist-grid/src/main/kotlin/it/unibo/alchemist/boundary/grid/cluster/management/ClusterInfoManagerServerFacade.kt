/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster.management

import it.unibo.alchemist.boundary.grid.cluster.ServerMetadata
import it.unibo.alchemist.core.Simulation
import it.unibo.alchemist.model.Position
import java.util.UUID

interface ClusterInfoManagerServerFacade : ClusterInfoManager {

    fun join(serverID: UUID, serverMetadata: ServerMetadata)

    fun leave(serverID: UUID)

    fun <T, P : Position<P>> getSimulation(jobID: UUID): Simulation<T, P>
}
