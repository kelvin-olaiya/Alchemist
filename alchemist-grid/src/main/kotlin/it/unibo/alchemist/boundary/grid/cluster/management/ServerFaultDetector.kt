/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster.management

import org.slf4j.LoggerFactory
import java.util.UUID

class ServerFaultDetector(
    private val toWatchServerID: UUID,
    timeoutMillis: Long,
    maxReplyMiss: Int,
    private val stopFlag: StopFlag,
    val onFaultDetection: (UUID) -> Unit = {},
) : Runnable {
    private val faultDetector = FaultDetector(timeoutMillis, maxReplyMiss)
    override fun run() {
        while (!stopFlag.isSet() && faultDetector.test(toWatchServerID)) { /**/ }
        if (!stopFlag.isSet()) onFaultDetection(toWatchServerID)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServerFaultDetector::class.java)
    }
}
