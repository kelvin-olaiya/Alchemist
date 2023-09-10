/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.exporters.composers

import it.unibo.alchemist.model.Actionable
import it.unibo.alchemist.model.Environment
import it.unibo.alchemist.model.Position
import it.unibo.alchemist.model.Time

interface DocumentComposer<T, P : Position<P>> {

    /**
     *
     */
    fun setup(environment: Environment<T, P>)

    /**
     *
     */
    fun update(environment: Environment<T, P>, reaction: Actionable<T>?, time: Time, step: Long)

    /**
     *
     */
    fun finalize(environment: Environment<T, P>, time: Time, step: Long)

    /**
     *
     */
    val text: String

    /**
     *
     */
    val bytes: ByteArray
}
