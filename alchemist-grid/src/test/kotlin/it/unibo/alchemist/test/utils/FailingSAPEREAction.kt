/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.test.utils

import it.unibo.alchemist.model.Context
import it.unibo.alchemist.model.Node
import it.unibo.alchemist.model.Reaction
import it.unibo.alchemist.model.sapere.ILsaMolecule
import it.unibo.alchemist.model.sapere.ILsaNode
import it.unibo.alchemist.model.sapere.actions.LsaAbstractAction

class FailingSAPEREAction(node: ILsaNode?, m: MutableList<ILsaMolecule>?) : LsaAbstractAction(node, m) {
    override fun toString(): String = "Failing SAPERE Action"

    override fun cloneAction(
        node: Node<MutableList<ILsaMolecule>>?,
        reaction: Reaction<MutableList<ILsaMolecule>>?,
    ): LsaAbstractAction = throw NotImplementedError()

    override fun execute() { }

    override fun getContext(): Context = Context.LOCAL
}
