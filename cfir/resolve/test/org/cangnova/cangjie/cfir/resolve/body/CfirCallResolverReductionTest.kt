package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [reduceCollectedCandidates] 候选集合规约行为测试。
 */
class CfirCallResolverReductionTest {

    @Nested
    inner class SuccessfulCandidates {

        @Test
        fun `single successful candidate is returned unchanged`() {
            val candidate = FakeCandidate("success", isSuccessful = true, applicability = CandidateApplicability.RESOLVED)

            val (reduced, applicability) = reduceCollectedCandidates(
                candidates = listOf(candidate),
                collectorApplicability = CandidateApplicability.RESOLVED,
                isCandidateSuccessful = FakeCandidate::isSuccessful,
                candidateApplicability = FakeCandidate::applicability,
                fullyProcessCandidate = { it.processed = true },
                chooseMostSpecific = { it },
            )

            assertEquals(setOf(candidate), reduced)
            assertEquals(CandidateApplicability.RESOLVED, applicability)
            assertTrue(!candidate.processed, "successful candidates should not be fully processed again")
        }

        @Test
        fun `multiple successful candidates delegate ambiguity reduction to conflict resolver`() {
            val lessSpecific = FakeCandidate("less", isSuccessful = true, applicability = CandidateApplicability.RESOLVED)
            val moreSpecific = FakeCandidate("more", isSuccessful = true, applicability = CandidateApplicability.RESOLVED)

            val (reduced, applicability) = reduceCollectedCandidates(
                candidates = listOf(lessSpecific, moreSpecific),
                collectorApplicability = CandidateApplicability.RESOLVED,
                isCandidateSuccessful = FakeCandidate::isSuccessful,
                candidateApplicability = FakeCandidate::applicability,
                fullyProcessCandidate = { it.processed = true },
                chooseMostSpecific = { setOf(moreSpecific) },
            )

            assertEquals(setOf(moreSpecific), reduced)
            assertEquals(CandidateApplicability.RESOLVED, applicability)
            assertTrue(!lessSpecific.processed && !moreSpecific.processed, "successful ambiguity should be reduced without extra full processing")
        }
    }

    @Nested
    inner class ErrorAndEmptyOutcomes {

        @Test
        fun `empty best-candidate set keeps collector applicability`() {
            val (reduced, applicability) = reduceCollectedCandidates<FakeCandidate>(
                candidates = emptyList(),
                collectorApplicability = CandidateApplicability.HIDDEN,
                isCandidateSuccessful = FakeCandidate::isSuccessful,
                candidateApplicability = FakeCandidate::applicability,
                fullyProcessCandidate = { it.processed = true },
                chooseMostSpecific = { it },
            )

            assertTrue(reduced.isEmpty())
            assertEquals(CandidateApplicability.HIDDEN, applicability)
        }

        @Test
        fun `single errored candidate preserves candidate-backed path with resolved-with-error applicability`() {
            val candidate = FakeCandidate(
                name = "errored",
                isSuccessful = false,
                applicability = CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY,
            )

            val (reduced, applicability) = reduceCollectedCandidates(
                candidates = listOf(candidate),
                collectorApplicability = CandidateApplicability.RESOLVED_WITH_ERROR,
                isCandidateSuccessful = FakeCandidate::isSuccessful,
                candidateApplicability = FakeCandidate::applicability,
                fullyProcessCandidate = { it.processed = true },
                chooseMostSpecific = { error("single errored candidate should not hit ambiguity reducer") },
            )

            assertEquals(setOf(candidate), reduced)
            assertEquals(CandidateApplicability.RESOLVED_WITH_ERROR, applicability)
            assertTrue(candidate.processed, "single errored candidate should be fully processed before reference creation")
        }

        @Test
        fun `multiple errored candidates are grouped by normalized applicability before specificity reduction`() {
            val unresolved = FakeCandidate(
                name = "unresolved",
                isSuccessful = false,
                applicability = CandidateApplicability.INAPPLICABLE,
            )
            val resolvedWithErrorA = FakeCandidate(
                name = "error-a",
                isSuccessful = false,
                applicability = CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY,
            )
            val resolvedWithErrorB = FakeCandidate(
                name = "error-b",
                isSuccessful = false,
                applicability = CandidateApplicability.RESOLVED_LOW_PRIORITY,
            )

            val (reduced, applicability) = reduceCollectedCandidates(
                candidates = listOf(unresolved, resolvedWithErrorA, resolvedWithErrorB),
                collectorApplicability = CandidateApplicability.RESOLVED_WITH_ERROR,
                isCandidateSuccessful = FakeCandidate::isSuccessful,
                candidateApplicability = FakeCandidate::applicability,
                fullyProcessCandidate = { it.processed = true },
                chooseMostSpecific = {
                    assertEquals(setOf(resolvedWithErrorA, resolvedWithErrorB), it)
                    setOf(resolvedWithErrorB)
                },
            )

            assertEquals(setOf(resolvedWithErrorB), reduced)
            assertEquals(CandidateApplicability.RESOLVED_WITH_ERROR, applicability)
            assertTrue(unresolved.processed && resolvedWithErrorA.processed && resolvedWithErrorB.processed)
        }
    }
}

/**
 * 候选规约测试使用的轻量候选模型。
 *
 * @property name 候选名称，用于断言和调试。
 * @property isSuccessful 是否代表成功候选。
 * @property applicability 候选自身适用性。
 * @property processed 是否已经执行完整处理。
 */
private data class FakeCandidate(
    /** 候选名称，用于断言和调试。 */
    val name: String,
    /** 是否代表成功候选。 */
    val isSuccessful: Boolean,
    /** 候选自身适用性。 */
    val applicability: CandidateApplicability,
    /** 是否已经执行完整处理。 */
    var processed: Boolean = false,
)
