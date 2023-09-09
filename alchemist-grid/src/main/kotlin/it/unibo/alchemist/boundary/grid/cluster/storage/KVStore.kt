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
import java.io.Closeable

interface KVStore : AutoCloseable {

    fun get(key: String, isPrefix: Boolean = true): Collection<ByteSequence>

    fun getKVs(key: String, isPrefix: Boolean = true): Collection<Pair<String, ByteSequence>>

    fun getKeys(key: String, isPrefix: Boolean = true): Collection<String>

    fun put(key: String, bytes: ByteSequence)

    fun put(key: String, bytes: ByteArray) = put(key, bytes.toByteSequence())

    fun delete(key: String, isPrefix: Boolean = false)

    fun watch(key: String, callback: (new: ByteSequence, prev: ByteSequence) -> Unit): Closeable

    fun watchPut(
        key: String,
        callback: (new: ByteSequence, prev: ByteSequence) -> Unit,
    ): Closeable

    fun watchDelete(
        key: String,
        callback: (new: ByteSequence, prev: ByteSequence) -> Unit,
    ): Closeable

    companion object {
        private fun ByteArray.toByteSequence() = ByteSequence.from(this)
    }
}
