@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.CfirTypeRelations
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCandidate
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildFunctionSymbol
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildTypedExpression
import org.cangnova.cangjie.cfir.resolve.body.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.semantics.CandidateApplicability
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * `CfirMapArguments` 参数映射阶段测试。
 */
class CfirMapArgumentsTest {

    @Nested
    inner class ExactMatch {

        @Test
        fun `zero args to zero params`() {
            val symbol = buildFunctionSymbol("f")
            val callInfo = buildCallInfo("f")
            val candidate = buildCandidate(symbol, callInfo)

            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), stubContext())

            assertEquals(CandidateApplicability.RESOLVED, candidate.lowestApplicability)
            assertEquals(0, candidate.numDefaults)
            assertTrue(candidate.argumentMapping.isEmpty())
        }

        @Test
        fun `two args to two params`() {
            val symbol = buildFunctionSymbol(
                "f",
                parameterTypes = listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN),
            )
            val callInfo = buildCallInfo(
                "f",
                arguments = listOf(
                    buildTypedExpression(ConePrimitiveType.INT32),
                    buildTypedExpression(ConePrimitiveType.BOOLEAN),
                ),
            )
            val candidate = buildCandidate(symbol, callInfo)

            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), stubContext())

            assertEquals(CandidateApplicability.RESOLVED, candidate.lowestApplicability)
            assertEquals(0, candidate.numDefaults)
            assertEquals(2, candidate.argumentMapping.size)
        }
    }

    @Nested
    inner class WrongArgumentCount {

        @Test
        fun `too few arguments`() {
            val symbol = buildFunctionSymbol(
                "f",
                parameterTypes = listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN),
            )
            val callInfo = buildCallInfo(
                "f",
                arguments = listOf(buildTypedExpression(ConePrimitiveType.INT32)),
            )
            val candidate = buildCandidate(symbol, callInfo)

            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), stubContext())

            assertEquals(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR, candidate.lowestApplicability)
        }

        @Test
        fun `too many arguments`() {
            val symbol = buildFunctionSymbol(
                "f",
                parameterTypes = listOf(ConePrimitiveType.INT32),
            )
            val callInfo = buildCallInfo(
                "f",
                arguments = listOf(
                    buildTypedExpression(ConePrimitiveType.INT32),
                    buildTypedExpression(ConePrimitiveType.BOOLEAN),
                ),
            )
            val candidate = buildCandidate(symbol, callInfo)

            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), stubContext())

            assertEquals(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR, candidate.lowestApplicability)
        }
    }

    @Nested
    inner class DefaultValueParameters {

        @Test
        fun `skip one default parameter`() {
            val symbol = buildFunctionSymbol(
                "f",
                parameterTypes = listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN),
                parameterDefaults = listOf(false, true),
            )
            val callInfo = buildCallInfo(
                "f",
                arguments = listOf(buildTypedExpression(ConePrimitiveType.INT32)),
            )
            val candidate = buildCandidate(symbol, callInfo)

            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), stubContext())

            assertEquals(CandidateApplicability.RESOLVED, candidate.lowestApplicability)
            assertEquals(1, candidate.numDefaults)
            assertEquals(1, candidate.argumentMapping.size)
        }

        @Test
        fun `all parameters have defaults and no args provided`() {
            val symbol = buildFunctionSymbol(
                "f",
                parameterTypes = listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN),
                parameterDefaults = listOf(true, true),
            )
            val callInfo = buildCallInfo("f")
            val candidate = buildCandidate(symbol, callInfo)

            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), stubContext())

            assertEquals(CandidateApplicability.RESOLVED, candidate.lowestApplicability)
            assertEquals(2, candidate.numDefaults)
        }
    }

    private fun stubContext(): CfirResolutionContext {
        return CfirResolutionContext(
            session = StubSessionForTest,
            bodyResolveContext = StubBodyResolveContext,
            typeRelations = StubTypeRelations,
        )
    }
}

// ---- 测试用 Stub 对象 ----

private object StubSessionForTest : org.cangnova.cangjie.cfir.session.CfirSession(Kind.Source) {
    override fun toString(): String = "StubSession"
}

private val StubBodyResolveContext = org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveContext(
    ReturnTypeCalculator.Default,
    org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext(),
)

private val StubTypeRelations = CfirTypeRelations(
    object : org.cangnova.cangjie.cfir.types.ConeTypeContext {
        override fun supertypes(type: org.cangnova.cangjie.cfir.types.ConeCangJieType) = emptyList<org.cangnova.cangjie.cfir.types.ConeCangJieType>()
        override fun isSameTypeConstructor(a: org.cangnova.cangjie.cfir.types.ConeCangJieType, b: org.cangnova.cangjie.cfir.types.ConeCangJieType) = a == b
    },
)
