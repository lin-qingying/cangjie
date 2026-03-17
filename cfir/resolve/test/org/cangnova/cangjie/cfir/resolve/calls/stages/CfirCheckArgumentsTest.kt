@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCandidate
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildFunctionSymbol
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildTypedExpression
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateApplicability
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveContext
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.body.CfirReturnTypeCalculator
import org.cangnova.cangjie.cfir.types.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * CfirCheckArguments 鍙傛暟绫诲瀷妫€鏌ラ樁娈垫祴璇曘€? */
class CfirCheckArgumentsTest {

    private lateinit var context: CfirResolutionContext

    @BeforeEach
    fun setUp() {
        context = CfirResolutionContext(
            session = StubSession,
            bodyResolveContext = CfirBodyResolveContext(CfirReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext()),
            subtypeChecker = ConeSubtypeChecker(TestTypeContext()),
        )
    }

    @Nested
    inner class TypeCompatible {

        @Test
        fun `same type is compatible`() {
            val symbol = buildFunctionSymbol("f", parameterTypes = listOf(ConePrimitiveType.INT32))
            val callInfo = buildCallInfo("f", arguments = listOf(buildTypedExpression(ConePrimitiveType.INT32)))
            val candidate = buildCandidate(symbol, callInfo)
            // 鍏堝仛鍙傛暟鏄犲皠
            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            CfirCheckArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            assertEquals(CfirCandidateApplicability.RESOLVED, candidate.lowestApplicability)
        }

        @Test
        fun `IdealInt is compatible with Int32`() {
            val symbol = buildFunctionSymbol("f", parameterTypes = listOf(ConePrimitiveType.INT32))
            val callInfo = buildCallInfo("f", arguments = listOf(buildTypedExpression(ConePrimitiveType.IDEAL_INT)))
            val candidate = buildCandidate(symbol, callInfo)
            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            CfirCheckArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            assertEquals(CfirCandidateApplicability.RESOLVED, candidate.lowestApplicability)
        }

        @Test
        fun `IdealFloat is compatible with Float64`() {
            val symbol = buildFunctionSymbol("f", parameterTypes = listOf(ConePrimitiveType.FLOAT64))
            val callInfo = buildCallInfo("f", arguments = listOf(buildTypedExpression(ConePrimitiveType.IDEAL_FLOAT)))
            val candidate = buildCandidate(symbol, callInfo)
            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            CfirCheckArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            assertEquals(CfirCandidateApplicability.RESOLVED, candidate.lowestApplicability)
        }
    }

    @Nested
    inner class TypeIncompatible {

        @Test
        fun `Int32 is not compatible with Bool`() {
            val symbol = buildFunctionSymbol("f", parameterTypes = listOf(ConePrimitiveType.BOOLEAN))
            val callInfo = buildCallInfo("f", arguments = listOf(buildTypedExpression(ConePrimitiveType.INT32)))
            val candidate = buildCandidate(symbol, callInfo)
            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            CfirCheckArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            assertEquals(CfirCandidateApplicability.INAPPLICABLE, candidate.lowestApplicability)
            assertTrue(candidate.diagnostics.any {
                it is org.cangnova.cangjie.cfir.resolve.calls.candidate.ArgumentTypeMismatch
            })
        }

        @Test
        fun `Bool is not compatible with Int32`() {
            val symbol = buildFunctionSymbol("f", parameterTypes = listOf(ConePrimitiveType.INT32))
            val callInfo = buildCallInfo("f", arguments = listOf(buildTypedExpression(ConePrimitiveType.BOOLEAN)))
            val candidate = buildCandidate(symbol, callInfo)
            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            CfirCheckArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            assertEquals(CfirCandidateApplicability.INAPPLICABLE, candidate.lowestApplicability)
        }

        @Test
        fun `second argument type mismatch`() {
            val symbol = buildFunctionSymbol(
                "f",
                parameterTypes = listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN),
            )
            val callInfo = buildCallInfo(
                "f",
                arguments = listOf(
                    buildTypedExpression(ConePrimitiveType.INT32),
                    buildTypedExpression(ConePrimitiveType.INT32), // 搴旇鏄?Bool
                ),
            )
            val candidate = buildCandidate(symbol, callInfo)
            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            CfirCheckArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            assertEquals(CfirCandidateApplicability.INAPPLICABLE, candidate.lowestApplicability)
        }
    }

    @Nested
    inner class ErrorTypeHandling {

        @Test
        fun `error type argument is silently skipped`() {
            val symbol = buildFunctionSymbol("f", parameterTypes = listOf(ConePrimitiveType.INT32))
            val callInfo = buildCallInfo("f", arguments = listOf(buildTypedExpression(ConeErrorType("test error"))))
            val candidate = buildCandidate(symbol, callInfo)
            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            CfirCheckArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            assertEquals(CfirCandidateApplicability.RESOLVED, candidate.lowestApplicability)
        }
    }
}

// ---- Stub 瀵硅薄 ----

private object StubSession : org.cangnova.cangjie.cfir.session.CfirSession(Kind.Source) {
    override fun toString(): String = "StubSession"
}

/** 鏀寔 IdealInt/IdealFloat 瀛愮被鍨嬪垽瀹氱殑娴嬭瘯 TypeContext */
private class TestTypeContext : ConeTypeContext {
    override fun supertypes(type: ConeCangjieType): Collection<ConeCangjieType> = emptyList()
    override fun isSameTypeConstructor(a: ConeCangjieType, b: ConeCangjieType): Boolean {
        if (a is ConePrimitiveType && b is ConePrimitiveType) return a.kind == b.kind
        return a == b
    }
}

