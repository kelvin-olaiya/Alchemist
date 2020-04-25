/*
 * Copyright (C) 2010-2020, Danilo Pianini and contributors
 * listed in the main project's alchemist/build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.model.implementations.geometry

import it.unibo.alchemist.model.interfaces.geometry.Vector2D
import org.apache.commons.lang3.ObjectUtils
import org.danilopianini.lang.MathUtils.fuzzyGreaterEquals
import java.lang.IllegalArgumentException

/**
 * Maps a value in [min, max] to [newMin, newMax].
 */
fun Double.map(min: Double, max: Double, newMin: Double, newMax: Double): Double = when {
    this !in min..max -> throw IllegalArgumentException("$this is not in [$min, $max]")
    fuzzyGreaterEquals(min, max) || fuzzyGreaterEquals(newMin, newMax) ->
        throw IllegalArgumentException("either [$min, $max] and/or [$newMin, $newMax] is empty")
    else -> (this - min) / (max - min) * (newMax - newMin) + newMin
}

/**
 * Creates a [ClosedRange] from a couple of unordered values.
 */
fun <T : Comparable<T>> rangeFromUnordered(bound1: T, bound2: T): ClosedRange<T> =
    ObjectUtils.min(bound1, bound2)..ObjectUtils.max(bound1, bound2)

/**
 * Checks whether two ranges coincide. It's a different way of checking if they're equals,
 * which doesn't depend on their actual implementation. Note that the way you obtain a range
 * may influence the actual class used, and you can have two coincident ranges which don't
 * result equals because of different classes.
 */
fun <T : Comparable<T>> ClosedRange<T>.coincide(other: ClosedRange<T>): Boolean =
    start == other.start && endInclusive == other.endInclusive

/**
 * Checks whether the range contains the [other] one.
 * This method is faster than checking if the range contains both of the endpoints of [other].
 */
fun <T : Comparable<T>> ClosedRange<T>.contains(other: ClosedRange<T>): Boolean =
    start <= other.start && endInclusive >= other.endInclusive

/**
 * Checks whether the range intersects the [other] one.
 */
fun <T : Comparable<T>> ClosedRange<T>.intersects(other: ClosedRange<T>): Boolean =
    start <= other.endInclusive && endInclusive >= other.start

/*
 * Finds the intersection between two ranges which are guaranteed to intersect.
 */
private fun <T : Comparable<T>> ClosedRange<T>.unsafeIntersection(other: ClosedRange<T>): ClosedRange<T> =
    ObjectUtils.max(start, other.start)..ObjectUtils.min(endInclusive, other.endInclusive)

/**
 * Finds the intersection between two ranges, the resulting range may feature a single value
 * (if the ranges only share an endpoint) or can be null, if they don't intersect at all.
 */
fun <T : Comparable<T>> ClosedRange<T>.intersection(other: ClosedRange<T>): ClosedRange<T>? = when {
    intersects(other) -> unsafeIntersection(other)
    else -> null
}

/**
 * Checks whether two ranges intersect, excluding their bounds (i.e., excluding both
 * [ClosedRange.start] and [ClosedRange.endInclusive]). This means false is returned in
 * case the ranges share a single endpoint.
 */
fun <T : Comparable<T>> ClosedRange<T>.intersectsBoundsExcluded(other: ClosedRange<T>): Boolean =
    start < other.endInclusive && endInclusive > other.start

/**
 * Performs a subtraction between ranges. The operation can produce an empty list (e.g. if
 * the current range is contained in the [other] one), a list featuring a single element,
 * or a list featuring two elements (e.g. if the current range contains the [other] one).
 */
infix operator fun <T : Comparable<T>> ClosedRange<T>.minus(other: ClosedRange<T>): List<ClosedRange<T>> = when {
    other.contains(this) -> emptyList()
    !intersects(other) -> listOf(this)
    start >= other.start -> listOf(rangeFromUnordered(endInclusive, other.endInclusive))
    endInclusive <= other.endInclusive -> listOf(rangeFromUnordered(start, other.start))
    else -> listOf(rangeFromUnordered(start, other.start), rangeFromUnordered(endInclusive, other.endInclusive))
}

/**
 * Subtracts all the given ranges from the current one. See [ClosedRange.minus].
 */
fun <T : Comparable<T>> ClosedRange<T>.subtractAll(others: List<ClosedRange<T>>): List<ClosedRange<T>> = when {
    others.isEmpty() -> listOf(this)
    else -> (this - others.first()).flatMap { it.subtractAll(others.drop(1)) }
}

/**
 * Given a non empty list of points represented as vectors, this method finds the extreme
 * coordinates (i.e. min and max coordinates) either on the X-axis or the Y-axis. [getXCoords]
 * indicates which coordinates to extract, these are used to create the returned [ClosedRange].
 */
fun <V : Vector2D<V>> List<V>.findExtremeCoords(getXCoords: Boolean): ClosedRange<Double> =
    ({ v: V -> v.x.takeIf { getXCoords } ?: v.y }).let { selector ->
        val min = minBy(selector)?.run(selector)
        val max = maxBy(selector)?.run(selector)
        require(min != null && max != null) { "no point could be found" }
        min..max
    }
