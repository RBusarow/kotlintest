package com.sksamuel.kotest.property.shrinking

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.PropertyTesting
import io.kotest.property.ShrinkingMode
import io.kotest.property.arbitrary.StringShrinker
import io.kotest.property.checkAll
import io.kotest.property.internal.doShrinking
import io.kotest.property.shrinks
import io.kotest.property.step

class StringShrinkerTest : StringSpec({

   beforeSpec {
      PropertyTesting.shouldPrintShrinkSteps = false
   }

   afterSpec {
      PropertyTesting.shouldPrintShrinkSteps = true
   }

   "StringShrinker should include empty string as the first candidate" {
      checkAll { a: String ->
         if (a.isNotEmpty())
            StringShrinker.shrink(a)[0].shouldHaveLength(0)
      }
   }

   "StringShrinker should bisect input as 2nd and 4th candidate" {
      checkAll { a: String ->
         if (a.length > 1) {
            val candidates = StringShrinker.shrink(a)
            candidates[1].shouldHaveLength(a.length / 2 + a.length % 2)
            candidates[3].shouldHaveLength(a.length / 2)
         }
      }
   }

   "StringShrinker should include 2 padded 'a's as the 3rd to 5th candidates" {
      checkAll { a: String ->
         if (a.length > 1) {
            val candidates = StringShrinker.shrink(a)
            candidates[2].shouldEndWith("a".repeat(a.length / 2))
            candidates[4].shouldStartWith("a".repeat(a.length / 2))
         }
      }
   }

   "StringShrinker should shrink to expected value" {
      checkAll<String> { a ->

         val shrunk = doShrinking(a, StringShrinker.step(a), ShrinkingMode.Unbounded) {
            it.shouldNotContain("#")
         }

         if (a.contains("#")) {
            shrunk shouldBe "#"
         } else {
            shrunk shouldBe a
         }
      }
   }

   "f:StringShrinker should prefer padded values" {
      val a = "97asd!@#ASD'''234)*safmasd"
      doShrinking(a, StringShrinker.step(a), ShrinkingMode.Unbounded) {
         it.length.shouldBeLessThan(4)
      } shouldBe "aaaa"

//      val b = "97a"
//      doShrinking(b, StringShrinker.shrinks(b), ShrinkingMode.Unbounded) {
//         it.length.shouldBeLessThan(13)
//      } shouldBe "97a"
   }
})
