/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster.storage

import io.etcd.jetcd.ByteSequence
import io.etcd.jetcd.Client
import io.etcd.jetcd.options.DeleteOption
import io.etcd.jetcd.options.GetOption
import io.etcd.jetcd.options.WatchOption
import io.etcd.jetcd.watch.WatchEvent.EventType
import java.io.Closeable

class EtcdKVStore(
    endpoints: List<String>,
) : KVStore {

    private val client = Client.builder()
        .endpoints(*endpoints.toTypedArray())
        .maxInboundMessageSize(Int.MAX_VALUE)
        .build()
    private val kvClient = client.kvClient
    private val watch = client.watchClient

    override fun get(key: String, isPrefix: Boolean): Collection<ByteSequence> = getKVs(key, isPrefix).map { it.second }

    override fun getKVs(key: String, isPrefix: Boolean): Collection<Pair<String, ByteSequence>> {
        val response = kvClient.get(key.toByteSequence(), GetOption.newBuilder().isPrefix(isPrefix).build()).get()
        return response.kvs.map { it.key.toString() to it.value }.toList()
    }

    override fun getKeys(key: String, isPrefix: Boolean): Collection<String> = getKVs(key, isPrefix).map { it.first }

    override fun put(key: String, bytes: ByteSequence) {
        kvClient.put(key.toByteSequence(), bytes).join()
    }

    override fun delete(key: String, isPrefix: Boolean) {
        kvClient.delete(key.toByteSequence(), DeleteOption.newBuilder().isPrefix(isPrefix).build()).join()
    }

    private fun watch(
        key: String,
        callback: (new: ByteSequence, prev: ByteSequence) -> Unit,
        watchEvents: Collection<EventType>,
    ): Closeable {
        return watch.watch(key.toByteSequence(), WatchOption.newBuilder().isPrefix(true).build()) {
            it.events.forEach { event ->
                if (event.eventType in watchEvents) callback(event.keyValue.value, event.prevKV.value)
            }
        }
    }

    override fun watch(
        key: String,
        callback: (new: ByteSequence, prev: ByteSequence) -> Unit,
    ): Closeable = watch(key, callback, setOf(EventType.PUT, EventType.DELETE))

    override fun watchPut(
        key: String,
        callback: (new: ByteSequence, prev: ByteSequence) -> Unit,
    ): Closeable = watch(key, callback, setOf(EventType.PUT))

    override fun watchDelete(
        key: String,
        callback: (new: ByteSequence, prev: ByteSequence) -> Unit,
    ): Closeable = watch(key, callback, setOf(EventType.DELETE))

    override fun close() {
        kvClient.close()
        client.close()
    }

    private fun String.toByteSequence() = ByteSequence.from(this.toByteArray())
}
