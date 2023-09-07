/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.communication

import java.util.UUID

/**
 * Server related data for the correct cluster management.
 */
class CommunicationQueues(
    serverID: UUID,
) {
    private val queues: MutableMap<String, String> = mutableMapOf()

    init {
        queues[HEALTH_QUEUE] = queueNameFor(serverID, HEALTH_QUEUE)
        queues[JOBS_QUEUE] = queueNameFor(serverID, JOBS_QUEUE)
    }

    private fun queueNameFor(serverID: UUID, topic: String) = "$serverID-$topic"

    val hearthbeats get() = queues[HEALTH_QUEUE]
    val jobOrders get() = queues[JOBS_QUEUE]

    companion object {
        private const val HEALTH_QUEUE = "health"
        private const val JOBS_QUEUE = "jobs"
    }
}
