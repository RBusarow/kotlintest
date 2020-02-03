package io.kotest.property.internal

import io.kotest.assertions.show.show
import io.kotest.property.PropertyTesting
import io.kotest.property.ShrinkingMode
import io.kotest.property.Step
import kotlin.time.ExperimentalTime

/**
 * Accepts a value of type A and a function that varies in type A (fixed in any other types) and attempts
 * to shrink the value to find the smallest failing case.
 *
 * For each step in the shrinker, we test all the values. If they all pass then the shrinking ends.
 * Otherwise, the next batch is taken and the shrinks continue.
 *
 * Once all values from a shrink step pass, we return the previous value as the "smallest" failing case.
 */
@UseExperimental(ExperimentalTime::class)
internal suspend fun <A> doShrinking(
   initial: A,
   step: Step<A>,
   mode: ShrinkingMode,
   test: suspend (A) -> Unit
): A {

   val sb = StringBuilder()
   sb.append("Attempting to shrink failed arg ${initial.show()}\n")
   val (candidate, count) = doStep(initial, step, 0, emptySet(), test, sb, mode)
   result(sb, candidate, count)
   println(sb)
   return candidate
}

internal suspend fun <A> doStep(
   initial: A,
   step: Step<A>,
   count: Int,
   tested: Set<A>,
   test: suspend (A) -> Unit,
   sb: StringBuilder,
   mode: ShrinkingMode
): Pair<A, Int> {

   val candidate = step.candidates.fold<A, A?>(null) { candidate, a ->
      if (candidate == null) {
         try {
            test(a)
            sb.append("Shrink #$count: ${a.show()} pass\n")
            null
         } catch (t: Throwable) {
            sb.append("Shrink #$count: ${a.show()} fail\n")
            a
         }
      } else a
   }

   val countp = count + step.candidates.size

   return when {
      candidate == null || !mode.isShrinking(countp) -> Pair(initial, countp)
      else -> doStep(
         candidate,
         step.next(candidate),
         countp,
         tested + step.candidates,
         test,
         sb,
         mode
      )
   }
}

private fun ShrinkingMode.isShrinking(count: Int): Boolean = when (this) {
   ShrinkingMode.Off -> false
   ShrinkingMode.Unbounded -> true
   is ShrinkingMode.Bounded -> count < bound
}

private fun <A> result(sb: StringBuilder, candidate: A, count: Int): A {
   when (count) {
      0 -> sb.append("Shrink result => ${candidate.show()}\n")
      else -> sb.append("Shrink result (after $count shrinks) => ${candidate.show()}\n")
   }
   if (PropertyTesting.shouldPrintShrinkSteps) {
      println(sb)
   }
   return candidate
}
