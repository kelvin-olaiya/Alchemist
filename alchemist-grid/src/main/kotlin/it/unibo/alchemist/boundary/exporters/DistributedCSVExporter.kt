/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.exporters

import it.unibo.alchemist.boundary.DistributedExporter
import it.unibo.alchemist.boundary.exporters.composers.CSVDocumentComposer
import it.unibo.alchemist.boundary.exporters.composers.DocumentComposer
import it.unibo.alchemist.boundary.grid.cluster.management.Registry
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.times.DoubleTime
import java.util.UUID

class DistributedCSVExporter<T, P : Position<P>> @JvmOverloads constructor(
    private val fileNameRoot: String = "",
    interval: Double = DEFAULT_INTERVAL,
    val fileExtension: String = "csv",
) : AbstractExporter<T, P>(interval), DistributedExporter<T, P> {

    private lateinit var registry: Registry
    private lateinit var jobID: UUID
    private val composer: DocumentComposer<T, P> =
        CSVDocumentComposer({ verboseVariablesDescriptor }, { dataExtractors })

    override fun bindRegistry(registry: Registry) {
        this.registry = registry
    }

    override fun bindJobId(jobID: UUID) {
        this.jobID = jobID
    }

    override fun close(environment: Environment<T, P>, time: Time, step: Long) {
        composer.finalize(environment, time, step)
        val filePrefix = listOf(fileNameRoot, variablesDescriptor).joinToString(separator = "_")
        val fileName = "$filePrefix.$fileExtension"
        registry.addResult(jobID, fileName, composer.bytes)
    }

    override fun setup(environment: Environment<T, P>) {
        composer.setup(environment)
        exportData(environment, null, DoubleTime(), 0)
    }

    override fun exportData(environment: Environment<T, P>, reaction: Actionable<T>?, time: Time, step: Long) {
        composer.update(environment, reaction, time, step)
    }
}
