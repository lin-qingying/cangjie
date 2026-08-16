@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.TestSession
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCandidate
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildFunctionSymbol
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildTypedExpression
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.newResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.newTestSession
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.runStagesForTest
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [CfirMapArguments] 阶段测试：实参到形参的映射与个数校验。
 */
class CfirMapArgumentsTest {

    private lateinit var session: TestSession
    private lateinit var context: ResolutionContext

    @BeforeEach
    fun setUp() {
        session = newTestSession()
        context = newResolutionContext(session)
    }

    @Nested
    inner class ExactMatch {
        @Test
        fun `zero arguments`() {
            val symbol = buildFunctionSymbol(session, "f")
            val callInfo = buildCallInfo(session, "f")
            val candidate = buildCandidate(session, symbol, callInfo)
            runStagesForTest(candidate, context, CfirMapArguments)

            assertEquals(CandidateApplicability.RESOLVED, candidate.applicability)
            assertTrue(candidate.argumentMapping.isEmpty())
            assertEquals(0, candidate.numDefaults)
        }

        @Test
        fun `two arguments`() {
            val parameterTypes = listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN)
            val symbol = buildFunctionSymbol(session, "f", parameterTypes = parameterTypes)
            val arg0 = buildTypedExpression(ConePrimitiveType.INT32)
            val arg1 = buildTypedExpression(ConePrimitiveType.BOOLEAN)
            val callInfo = buildCallInfo(session, "f", arguments = listOf(arg0, arg1))
            val candidate = buildCandidate(session, symbol, callInfo)
            runStagesForTest(candidate, context, CfirMapArguments)

            assertEquals(CandidateApplicability.RESOLVED, candidate.applicability)
            assertEquals(2, candidate.argumentMapping.size)
            assertEquals(symbol.cfir.valueParameters.toSet(), candidate.argumentMapping.values.toSet())
        }
    }

    @Nested
    inner class Defaults {
        @Test
        fun `number of default arguments is counted`() {
            val symbol = buildFunctionSymbol(
                session, "f",
                parameterTypes = listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN, ConePrimitiveType.FLOAT64),
                parameterDefaults = listOf(false, true, true),
            )
            val arg0 = buildTypedExpression(ConePrimitiveType.INT32)
            val callInfo = buildCallInfo(session, "f", arguments = listOf(arg0))
            val candidate = buildCandidate(session, symbol, callInfo)
            runStagesForTest(candidate, context, CfirMapArguments)

            assertEquals(CandidateApplicability.RESOLVED, candidate.applicability)
            assertEquals(2, candidate.numDefaults)
            assertEquals(1, candidate.argumentMapping.size)
        }
    }

    @Nested
    inner class TooFewArguments {
        @Test
        fun `too few arguments are rejected`() {
            val symbol = buildFunctionSymbol(
                session, "f",
                parameterTypes = listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN),
            )
            val callInfo = buildCallInfo(session, "f", arguments = listOf(buildTypedExpression(ConePrimitiveType.INT32)))
            val candidate = buildCandidate(session, symbol, callInfo)
            runStagesForTest(candidate, context, CfirMapArguments)

            assertEquals(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR, candidate.applicability)
            assertTrue(candidate.diagnostics.isNotEmpty())
            assertEquals(0, candidate.numDefaults)
        }
    }

    @Nested
    inner class TooManyArguments {
        @Test
        fun `too many arguments are rejected`() {
            val symbol = buildFunctionSymbol(
                session, "f",
                parameterTypes = listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN),
            )
            val arguments = listOf(
                buildTypedExpression(ConePrimitiveType.INT32),
                buildTypedExpression(ConePrimitiveType.BOOLEAN),
                buildTypedExpression(ConePrimitiveType.FLOAT64),
            )
            val callInfo = buildCallInfo(session, "f", arguments = arguments)
            val candidate = buildCandidate(session, symbol, callInfo)
            runStagesForTest(candidate, context, CfirMapArguments)

            assertEquals(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR, candidate.applicability)
            assertTrue(candidate.diagnostics.isNotEmpty())
            assertEquals(0, candidate.numDefaults)
        }
    }
}
