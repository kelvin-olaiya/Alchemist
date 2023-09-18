/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.simulation

import it.unibo.alchemist.boundary.grid.cluster.management.Registry
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintStream
import java.util.Optional
import java.util.UUID

class SimulationResultImpl(
    override val jobID: UUID,
    private val registry: Registry,
) : SimulationResult {

    override val jobDescriptor = registry.getJobDescriptor(jobID)

    override val error: Optional<Throwable> = registry.jobError(jobID)

    override fun saveLocally(exportPath: String) {
        if (error.isPresent) {
            throw Exception(error.get())
        }
        if (!File(exportPath).exists()) {
            File(exportPath).mkdirs()
        }
        val results = registry.resultsByJobID(jobID)
        val path = if (exportPath.endsWith(File.separator)) exportPath else "$exportPath${File.separator}"
        results.forEach {
            val fileName = "$path${it.first}"
            val outputStream = PrintStream(fileName)
            outputStream.print(String(it.second))
            outputStream.close()
            logger.info("The result file $fileName has been correctly saved")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SimulationResult::class.java)
    }
}
