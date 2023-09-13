/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.utils

import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * Manages a temporary working directory.
 */
class WorkingDirectory : AutoCloseable {

    private val directory = Files.createTempDirectory("alchemist").toFile()

    /**
     * The temporary directory url.
     */
    val url: URL get() = directory.toURI().toURL()

    /**
     * Get the folder file content.
     */
    fun getFileContent(filename: String): String {
        val file = File(getFileAbsolutePath(filename))
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8)
    }

    /**
     * An absolute path for a given filename in this directory.
     */
    fun getFileAbsolutePath(filename: String): String = "${directory.absolutePath}${File.separator}$filename"

    /**
     * Writes multiple files inside the directory
     */
    fun writeFiles(files: Map<String, ByteArray>) {
        files.forEach { (name, content) ->
            val file = File(getFileAbsolutePath(name))
            if (file.parentFile.exists() || file.parentFile.mkdirs()) {
                FileUtils.writeByteArrayToFile(file, content)
            } else {
                throw IllegalStateException("Could not create directory structure for $file")
            }
        }
    }

    override fun close() {
        FileUtils.deleteDirectory(directory)
    }
}
