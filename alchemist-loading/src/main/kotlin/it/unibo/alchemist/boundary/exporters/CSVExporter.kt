/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.exporters

import com.google.common.base.Charsets
import it.unibo.alchemist.boundary.exporters.composers.CSVDocumentComposer
import it.unibo.alchemist.boundary.exporters.composers.DocumentComposer
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.model.times.DoubleTime
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintStream
import java.io.Serializable
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory

/**
 * Writes on file data provided by a number of {@link Extractor}s. Produces a
 * CSV with '#' as comment character.e
 * @param fileNameRoot the starting name of the file to export data to.
 * @param interval the sampling time, defaults to [AbstractExporter.DEFAULT_INTERVAL].
 * @param exportPath if no path is specified it will generate the file inside a temporary folder.
 * @param appendTime if true it will always generate a new file, false to overwrite.
 * @param fileExtension the extension for the exported files, by default 'csv'
 */
class CSVExporter<T, P : Position<P>> @JvmOverloads constructor(
    private val fileNameRoot: String = "",
    val interval: Double = DEFAULT_INTERVAL,
    val exportPath: String = createTempDirectory("alchemist-export").absolutePathString()
        .also { logger.warn("No output folder specified but export required. Alchemist will export data in $it") },
    val fileExtension: String = "csv",
    private val appendTime: Boolean = false,
) : AbstractExporter<T, P>(interval), Serializable {

    private lateinit var outputPrintStream: PrintStream
    private val composer: DocumentComposer<T, P> = CSVDocumentComposer({ verboseVariablesDescriptor }, { dataExtractors })

    override fun setup(environment: Environment<T, P>) {
        if (!File(exportPath).exists()) {
            File(exportPath).mkdirs()
        }
        val path = if (exportPath.endsWith(File.separator)) exportPath else "${exportPath}${File.separator}"
        val time = if (appendTime) "${System.currentTimeMillis()}" else ""
        val filePrefix = listOf(fileNameRoot, variablesDescriptor, time)
            .filter(String::isNotBlank)
            .joinToString(separator = "_")
        require(filePrefix.isNotEmpty()) {
            "No fileNameRoot provided for exporting data, no variables in the environment, and timestamp unset:" +
                "the file name would be empty. Please provide a file name."
        }
        outputPrintStream = PrintStream("$path$filePrefix.$fileExtension", Charsets.UTF_8.name())
        composer.setup(environment)
        exportData(environment, null, DoubleTime(), 0)
    }

    override fun exportData(environment: Environment<T, P>, reaction: Actionable<T>?, time: Time, step: Long) {
        composer.update(environment, reaction, time, step)
    }

    override fun close(environment: Environment<T, P>, time: Time, step: Long) {
        composer.finalize(environment, time, step)
        outputPrintStream.print(composer.text)
        outputPrintStream.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CSVExporter::class.java)
    }
}
