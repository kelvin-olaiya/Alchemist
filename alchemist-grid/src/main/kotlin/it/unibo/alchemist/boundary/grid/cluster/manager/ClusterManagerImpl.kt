/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster.manager

import it.unibo.alchemist.boundary.grid.cluster.AlchemistRemoteServer
import it.unibo.alchemist.boundary.grid.cluster.RemoteServer
import it.unibo.alchemist.proto.Cluster
import it.unibo.alchemist.proto.Common
import java.util.Map.copyOf
import java.util.UUID

class ClusterManagerImpl(
    private val etcd: EtcdHelper,
) : ClusterInfoManagerClientFacade, ClusterInfoManagerServerFacade, AutoCloseable {

    override val servers: Collection<RemoteServer> get() {
        return etcd.get(ETCD_SERVERS_KEY).map { Cluster.Registration.parseFrom(it.bytes) }.map {
            AlchemistRemoteServer(UUID.fromString(it.serverID.value), copyOf(it.metadataMap))
        }.toList()
    }

    override fun join(serverID: UUID, serverMetadata: ServerMetadata) {
        val protoServerID = Common.ID.newBuilder().setValue(serverID.toString())
        val serverData = Cluster.Registration.newBuilder()
            .setServerID(protoServerID)
            .putAllMetadata(serverMetadata.metadata)
            .build()
        etcd.put(asEtcdServerKey(serverID.toString()), serverData.toByteArray()).join()
    }

    override fun leave(serverID: UUID) {
        etcd.delete(asEtcdServerKey(serverID.toString())).join()
    }

    override fun close() {
        etcd.close()
    }

    companion object {
        private const val ETCD_SERVERS_KEY = "servers"
        private fun asEtcdServerKey(serverID: String) = "${ETCD_SERVERS_KEY}/$serverID"
    }
}
