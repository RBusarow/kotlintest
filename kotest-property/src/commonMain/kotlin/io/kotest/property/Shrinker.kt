package io.kotest.property

/**
 * Given a value, T, this function returns reduced values to be used as candidates
 * for shrinking.
 *
 * A smaller value is defined per Shrinker. For a string it may be considered a string with
 * less characters, or less duplication/variation in the characters. For an integer it is typically
 * considered a smaller value with a positive sign.
 *
 * Shrinkers can return one or more values in a shrink step. Shrinkers can
 * return more than one value if there is no single "best path". For example,
 * when shrinking an integer, you probably want to return a single smaller value
 * at a time. For strings, you may wish to return a string that is simpler (YZ -> YY),
 * as well as smaller (YZ -> Y).
 *
 * If the value cannot be shrunk further, or the type
 * does not support meaningful shrinking, then this function should
 * return an empty list.
 */
interface Shrinker<A> {

   /**
    * Returns the "next level" of shrinks for the given value, or empty list if a "base case" has been reached.
    * For example, to shrink an int k we may decide to return k/2 and k-1.
    */
   fun shrink(value: A): List<A>
}

data class RTree<A>(val value: A, val children: Lazy<RTree<A>>)

data class Step<A>(val candidates: List<A>, val next: (A) -> Step<A>)

fun emptyStep(): Step<Nothing>

val emptyStep: Step<Nothing> = Step(emptyList()) { emptyStep }

/**
 * Generates the next [Step] of shrinks with a function to continue the shrinking process.
 */
fun <A> Shrinker<A>.step(a: A): Step<A> {
   val candidates = shrink(a)
   return Step(candidates) { this@step.step(it) }
}

fun <A> Shrinker<A>.rtree(a: A): RTree<A> {
   fun rtree(a: A, list: List<A>) = RTree(a, lazy { list.map { rtree(it, shrink(it)) } })
   val children = lazy { shrink(a) }
   return RTree(a, children)
}
