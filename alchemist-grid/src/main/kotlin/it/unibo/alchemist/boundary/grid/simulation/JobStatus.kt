/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.simulation

import it.unibo.alchemist.proto.SimulationMessage.Status

enum class JobStatus(
    val proto: Status,
) {
    DISPATCHED(Status.DISPATCHED),
    RUNNING(Status.RUNNING),
    DONE(Status.DONE),
    FAILED(Status.FAILED),
    UNRECOGNIZED(Status.UNRECOGNIZED),
    ;

    companion object {
        fun fromProto(proto: Status) = when (proto) {
            Status.DISPATCHED -> DISPATCHED
            Status.RUNNING -> RUNNING
            Status.DONE -> DONE
            Status.FAILED -> FAILED
            Status.UNRECOGNIZED -> UNRECOGNIZED
        }
    }
}
