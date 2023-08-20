/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.launch

import it.unibo.alchemist.AlchemistExecutionOptions
import it.unibo.alchemist.boundary.Loader

/**
 * Launches a simulation set on a cluster of Alchemist nodes running in server mode.
 */
object DistributedExecution : SimulationLauncher() {

    override val name = "Alchemist execution on a grid system"

    override fun additionalValidation(currentOptions: AlchemistExecutionOptions) = with(currentOptions) {
        when {
            variables.isEmpty() -> Validation.Invalid("$name requires a variable set")
            distributed == null -> Validation.Invalid("No configuration file for distributed execution")
            graphics != null -> Validation.OK(Priority.Fallback("Distributed execution will ignore graphical settings"))
            parallelism != AlchemistExecutionOptions.defaultParallelism -> incompatibleWith("custom parallelism")
            else -> Validation.OK()
        }
    }

    override fun launch(loader: Loader, parameters: AlchemistExecutionOptions) {
    }
}
