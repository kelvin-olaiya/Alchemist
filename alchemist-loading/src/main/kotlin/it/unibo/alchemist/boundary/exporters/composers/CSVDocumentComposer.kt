/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.exporters.composers

import it.unibo.alchemist.boundary.Extractor
import it.unibo.alchemist.boundary.exporters.CSVExporter
import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Time
import it.unibo.alchemist.util.BugReporting
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.SortedMap
import java.util.TimeZone
import kotlin.reflect.KClass

class CSVDocumentComposer<T, P : Position<P>>(
    private val verboseVariablesDescriptorSupplier: () -> String,
    private val dataExtractorsSupplier: () -> List<Extractor<*>>,
) : DocumentComposer<T, P>, Serializable {

    private val csvOutput = StringBuilder()
    private lateinit var dataExtractors: List<Extractor<*>>
    private lateinit var verboseVariablesDescriptor: String

    override fun setup(environment: Environment<T, P>) {
        dataExtractors = dataExtractorsSupplier.invoke()
        verboseVariablesDescriptor = verboseVariablesDescriptorSupplier.invoke()

        with(csvOutput) {
            csvOutput
            println(SEPARATOR)
            print("# Alchemist log file - simulation started at: ")
            print(now())
            println(" #")
            println(SEPARATOR)
            println('#')
            print("# ")
            println(verboseVariablesDescriptor)
            println('#')
            println("# The columns have the following meaning: ")
            print("# ")
            dataExtractors.flatMap {
                it.columnNames
            }.forEach {
                print(it)
                print(" ")
            }
            println()
        }
    }

    override fun update(environment: Environment<T, P>, reaction: Actionable<T>?, time: Time, step: Long) {
        val line: String = dataExtractors.joinToString(separator = " ") { extractor ->
            val data = extractor.extractDataAsText(environment, reaction, time, step)
            val names = extractor.columnNames
            when {
                data.size <= 1 -> data.values.joinToString(" ")
                // Labels and keys match
                data.size == names.size && data.keys.containsAll(names) -> names.joinToString(" ") {
                    requireNotNull(data[it]) {
                        BugReporting.reportBug(
                            "Bug in ${this::class.simpleName}",
                            mapOf("key" to it, "data" to data),
                        )
                    }
                }
                // If the labels do not match keys, require predictable iteration order
                else -> {
                    require(data.hasPredictableIteration) {
                        BugReporting.reportBug(
                            """
                            Extractor "${extractor::class.simpleName}" is likely bugged:
                            1. the set of labels $names does not match the keys ${data.keys}, but iteration may fail as
                            2. it returned a map with non-predictable iteration order of type ${data::class.simpleName}"
                            """.trimIndent(),
                        )
                    }
                    data.values.joinToString(" ")
                }
            }
        }
        csvOutput.println(line)
    }

    override fun finalize(environment: Environment<T, P>, time: Time, step: Long) {
        with(csvOutput) {
            println(SEPARATOR)
            print("# End of data export. Simulation finished at: ")
            print(now())
            println(" #")
            println(SEPARATOR)
        }
    }

    override val text get() = csvOutput.toString()

    override val bytes get() = text.toByteArray()

    private fun StringBuilder.println(line: String = "") {
        this.append("$line\n")
    }

    private fun StringBuilder.println(char: Char) {
        this.append("$char\n")
    }
    private fun StringBuilder.print(line: String) {
        this.append(line)
    }

    companion object {
        /**
         * Character used to separate comments from data on export files.
         */
        private const val SEPARATOR = "#####################################################################"

        private val logger = LoggerFactory.getLogger(CSVExporter::class.java)

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        private fun now(): String = dateFormat.format(Date())

        /**
         * Types listed here are supported as featuring a predictable iteration order.
         * New types that feature such support should be allow-listed here.
         */
        private val mapsWithPredictableIteration: List<KClass<out Map<*, *>>> = listOf(
            LinkedHashMap::class,
            SortedMap::class,
        )

        private val Map<String, Any>.hasPredictableIteration get() = mapsWithPredictableIteration.any { kclass ->
            kclass.java.isAssignableFrom(this::class.java)
        }
    }
}
