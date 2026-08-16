@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.TestSession
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCandidate
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildFunctionSymbol
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildTypedExpression
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.newResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.newTestSession
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.runStagesForTest
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInArguments
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [CfirCheckArguments] 阶段测试：实参类型与形参类型的兼容性检查。
 */
class CfirCheckArgumentsTest {

    private lateinit var session: TestSession
    private lateinit var context: ResolutionContext

    @BeforeEach
    fun setUp() {
        session = newTestSession()
        context = newResolutionContext(session)
    }

    private fun runArgumentCheck(
        parameterTypes: List<ConePrimitiveType>,
        argumentTypes: List<ConePrimitiveType>,
    ) = runArgumentCheckWithExpressions(parameterTypes, argumentTypes.map { buildTypedExpression(it) })

    private fun runArgumentCheckWithExpressions(
        parameterTypes: List<ConePrimitiveType>,
        arguments: List<CfirExpression>,
    ): Candidate {
        val symbol = buildFunctionSymbol(session, "f", parameterTypes = parameterTypes)
        val callInfo = buildCallInfo(session, "f", arguments = arguments)
        val candidate = buildCandidate(session, symbol, callInfo)
        runStagesForTest(candidate, context, CfirMapArguments, CfirCheckArguments)
        return candidate
    }

    @Nested
    inner class TypeCompatible {
        @Test
        fun `same type is compatible`() {
            val candidate = runArgumentCheck(listOf(ConePrimitiveType.INT32), listOf(ConePrimitiveType.INT32))

            assertEquals(CandidateApplicability.RESOLVED, candidate.applicability)
            assertTrue(candidate.diagnostics.isEmpty())
            assertEquals(1, candidate.argumentMapping.size)
        }

        @Test
        fun `int32 is not compatible with boolean`() {
            val candidate = runArgumentCheck(listOf(ConePrimitiveType.BOOLEAN), listOf(ConePrimitiveType.INT32))

            assertEquals(CandidateApplicability.INAPPLICABLE, candidate.applicability)
            assertEquals(1, candidate.diagnostics.size)
            assertTrue(candidate.diagnostics.single() is ArgumentTypeMismatch)
        }

        @Test
        fun `boolean is not compatible with int32`() {
            val candidate = runArgumentCheck(listOf(ConePrimitiveType.INT32), listOf(ConePrimitiveType.BOOLEAN))

            assertEquals(CandidateApplicability.INAPPLICABLE, candidate.applicability)
            assertTrue(candidate.diagnostics.single() is ArgumentTypeMismatch)
        }

        @Test
        fun `second argument mismatch makes the whole candidate inapplicable`() {
            val candidate = runArgumentCheck(
                listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN),
                listOf(ConePrimitiveType.INT32, ConePrimitiveType.INT32),
            )

            assertEquals(CandidateApplicability.INAPPLICABLE, candidate.applicability)
            assertEquals(1, candidate.diagnostics.size)
            assertTrue(candidate.diagnostics.single() is ArgumentTypeMismatch)
        }

        @Test
        fun `ideal int and ideal float are compatible with concrete numeric types`() {
            val idealInt = runArgumentCheck(listOf(ConePrimitiveType.INT32), listOf(ConePrimitiveType.IDEAL_INT))
            assertEquals(CandidateApplicability.RESOLVED, idealInt.applicability)

            val idealFloat = runArgumentCheck(listOf(ConePrimitiveType.FLOAT64), listOf(ConePrimitiveType.IDEAL_FLOAT))
            assertEquals(CandidateApplicability.RESOLVED, idealFloat.applicability)
        }

        @Test
        fun `error type argument is not applicable`() {
            val errorArgument = buildTypedExpression(ConeErrorType(ConeSimpleDiagnostic("test error")))
            val candidate = runArgumentCheckWithExpressions(listOf(ConePrimitiveType.INT32), listOf(errorArgument))

            // 与旧实现“error 类型静默跳过并保持 RESOLVED”不同，
            // 新实现通过 ErrorTypeInArguments 让整个候选不可用。
            assertEquals(CandidateApplicability.INAPPLICABLE, candidate.applicability)
            assertEquals(1, candidate.diagnostics.size)
            assertTrue(candidate.diagnostics.single() is ErrorTypeInArguments)
        }
    }
}
