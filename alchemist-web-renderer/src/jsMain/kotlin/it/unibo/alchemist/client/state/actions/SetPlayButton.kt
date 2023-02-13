/*
 * Copyright (C) 2010-2022, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.client.state.actions

import it.unibo.alchemist.common.utility.Action

/**
 * Redux action to set the Simulation state of the application and switch between Play and pause.
 * @param action the new simulation action to set.
 */
data class SetPlayButton(val action: Action)
