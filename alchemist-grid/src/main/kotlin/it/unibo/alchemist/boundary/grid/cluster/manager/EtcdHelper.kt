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

class EtcdHelper(
    endpoints: List<String>,
) : AutoCloseable {

    private val client = Client.builder().endpoints(*endpoints.toTypedArray()).build()
    private val kvClient = client.kvClient

    fun get(key: String, isPrefix: Boolean = true): Collection<ByteSequence> {
        val response = kvClient.get(key.toByteSequence(), GetOption.newBuilder().isPrefix(isPrefix).build()).get()
        return response.kvs.map { it.value }.toList()
    }

    fun put(key: String, bytes: ByteSequence) = kvClient.put(key.toByteSequence(), bytes)

    fun put(key: String, bytes: ByteArray) = put(key, bytes.toByteSequence())

    fun delete(key: String) = kvClient.delete(key.toByteSequence())

    override fun close() {
        kvClient.close()
        client.close()
    }

    private fun String.toByteSequence() = ByteSequence.from(this.toByteArray())
    private fun ByteArray.toByteSequence() = ByteSequence.from(this)
}
