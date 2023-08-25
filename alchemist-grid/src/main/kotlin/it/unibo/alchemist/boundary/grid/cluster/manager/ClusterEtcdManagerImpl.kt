/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster.manager

import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import io.etcd.jetcd.options.GetOption
import it.unibo.alchemist.boundary.grid.cluster.AlchemistRemoteServer
import it.unibo.alchemist.boundary.grid.cluster.RemoteServer
import java.util.UUID

class ClusterEtcdManagerImpl(
    endpoints: List<String>,
) : ClusterEtcdManager, EtcdClientManager, EtcdServerManager, AutoCloseable {

    private val client = Client.builder().endpoints(*endpoints.toTypedArray()).build()
    private val kvClient = client.kvClient

    override val servers: Collection<RemoteServer> get() {
        return get(ETCD_SERVERS_KEY).map { Cluster.ServerJoin.parseFrom(it.bytes) }.map {
            AlchemistRemoteServer(UUID.fromString(it.serverID.value))
        }.toList()
    }

    override fun join(serverID: UUID, serverMetadata: ServerMetadata) {
        val protoServerID = Common.ID.newBuilder().setValue(serverID.toString())
        val serverData = Cluster.ServerJoin.newBuilder()
            .setServerID(protoServerID)
            .setQueueName(serverMetadata.rabbitmq)
            .build()
        put("${ETCD_SERVERS_KEY}/$serverID", serverData.toByteArray()).join()
    }

    override fun leave(serverID: UUID) {
        TODO("Not yet implemented")
    }

    private fun get(key: String, isPrefix: Boolean = true): Collection<ByteSequence> {
        val response = kvClient.get(key.toByteSequence(), GetOption.newBuilder().isPrefix(isPrefix).build()).get()
        return response.kvs.map { it.value }.toList()
    }

    private fun put(key: String, bytes: ByteSequence) = kvClient.put(key.toByteSequence(), bytes)
    private fun put(key: String, bytes: ByteArray) = put(key, bytes.toByteSequence())

    private fun String.toByteSequence() = ByteSequence.from(this.toByteArray())
    private fun ByteArray.toByteSequence() = ByteSequence.from(this)

    override fun close() {
        kvClient.close()
        client.close()
    }

    companion object {
        private const val ETCD_SERVERS_KEY = "servers"
    }
}
