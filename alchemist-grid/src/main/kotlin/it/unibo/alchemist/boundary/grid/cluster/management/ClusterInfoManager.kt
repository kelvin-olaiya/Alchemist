/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster.management

import it.unibo.alchemist.boundary.grid.cluster.RemoteServer
import it.unibo.alchemist.boundary.grid.cluster.ServerMetadata
import java.util.Optional
import java.util.UUID

interface ClusterInfoManager {

    /**
     * Returns a collection of server in the cluster.
     */
    val servers: Collection<RemoteServer>

    /**
     * Returns the server's metadata
     */
    fun metadataOf(serverID: UUID): Optional<ServerMetadata>
}
