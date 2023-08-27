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

interface KVStore : AutoCloseable {

    fun get(key: String, isPrefix: Boolean = true): Collection<ByteSequence>

    fun put(key: String, bytes: ByteSequence)

    fun put(key: String, bytes: ByteArray) = put(key, bytes.toByteSequence())

    fun delete(key: String)

    companion object {
        private fun ByteArray.toByteSequence() = ByteSequence.from(this)
    }
}
