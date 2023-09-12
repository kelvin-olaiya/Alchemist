/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.proto

import it.unibo.alchemist.boundary.grid.simulation.JobStatus
import it.unibo.alchemist.proto.SimulationMessage.Status

object ProtoExtensions {

    val JobStatus.proto get() = when (this) {
        JobStatus.DISPATCHED -> Status.DISPATCHED
        JobStatus.RUNNING -> Status.RUNNING
        JobStatus.DONE -> Status.DONE
        JobStatus.FAILED -> Status.FAILED
        JobStatus.UNRECOGNIZED -> Status.UNRECOGNIZED
    }

    val Status.toJobStatus get(): JobStatus = when (this) {
        Status.DISPATCHED -> JobStatus.DISPATCHED
        Status.RUNNING -> JobStatus.RUNNING
        Status.DONE -> JobStatus.DONE
        Status.FAILED -> JobStatus.FAILED
        Status.UNRECOGNIZED -> JobStatus.UNRECOGNIZED
    }
}
