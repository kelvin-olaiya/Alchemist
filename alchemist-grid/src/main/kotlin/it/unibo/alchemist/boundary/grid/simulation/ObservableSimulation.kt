/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.simulation

import it.unibo.alchemist.boundary.grid.utils.WorkingDirectory
import it.unibo.alchemist.core.Simulation
import it.unibo.alchemist.model.Position
import org.kaikikm.threadresloader.ResourceLoader
import java.util.UUID

class ObservableSimulation<T, P : Position<P>>(
    private val simulation: Simulation<T, P>,
    private val jobID: UUID,
    private val workingDirectory: WorkingDirectory,
) : Simulation<T, P> by simulation {

    private val onCompleteCallbacks = mutableSetOf<(UUID) -> Unit>()
    private val onStartCallbacks = mutableSetOf<(UUID) -> Unit>()
    private val onErrorCallbacks = mutableSetOf<(UUID, Exception) -> Unit>()

    fun addStartCallback(callback: (UUID) -> Unit) {
        onStartCallbacks.add(callback)
    }

    fun addCompletionCallback(callback: (UUID) -> Unit) {
        onCompleteCallbacks.add(callback)
    }

    fun addOnErrorCallback(callback: (UUID, Exception) -> Unit) {
        onErrorCallbacks.add(callback)
    }

    override fun run() {
        onStartCallbacks.forEach { it(jobID) }
        try {
            ResourceLoader.injectURLs(workingDirectory.url)
            simulation.play()
            simulation.run()
        } catch (e: Exception) {
            onErrorCallbacks.forEach { it(jobID, e) }
        }
        onCompleteCallbacks.forEach { it(jobID) }
    }
}
