@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCandidate
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildFunctionSymbol
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildTypedExpression
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveContext
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.body.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.CfirTypeRelations
import org.cangnova.cangjie.cfir.semantics.CandidateApplicability
import org.cangnova.cangjie.cfir.types.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * `CfirCheckArguments` 参数类型检查阶段测试。
 */
class CfirCheckArgumentsTest {

    /**
     * 参数检查阶段测试使用的 resolution context。
     */
    private lateinit var context: CfirResolutionContext

    /**
     * 初始化支持 ideal 类型兼容判断的测试上下文。
     */
    @BeforeEach
    fun setUp() {
        context = CfirResolutionContext(
            session = StubSession,
            bodyResolveContext = CfirBodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext()),
            typeRelations = CfirTypeRelations(TestTypeContext()),
        )
    }

    @Nested
    inner class TypeCompatible {

        @Test
        fun `same type is compatible`() {
            val symbol = buildFunctionSymbol("f", parameterTypes = listOf(ConePrimitiveType.INT32))
            val callInfo = buildCallInfo("f", arguments = listOf(buildTypedExpression(ConePrimitiveType.INT32)))
            val candidate = buildCandidate(symbol, callInfo)
            // 先执行参数映射
            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            CfirCheckArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            assertEquals(CandidateApplicability.RESOLVED, candidate.lowestApplicability)
        }

        @Test
        fun `IdealInt is compatible with Int32`() {
            val symbol = buildFunctionSymbol("f", parameterTypes = listOf(ConePrimitiveType.INT32))
            val callInfo = buildCallInfo("f", arguments = listOf(buildTypedExpression(ConePrimitiveType.IDEAL_INT)))
            val candidate = buildCandidate(symbol, callInfo)
            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            CfirCheckArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            assertEquals(CandidateApplicability.RESOLVED, candidate.lowestApplicability)
        }

        @Test
        fun `IdealFloat is compatible with Float64`() {
            val symbol = buildFunctionSymbol("f", parameterTypes = listOf(ConePrimitiveType.FLOAT64))
            val callInfo = buildCallInfo("f", arguments = listOf(buildTypedExpression(ConePrimitiveType.IDEAL_FLOAT)))
            val candidate = buildCandidate(symbol, callInfo)
            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            CfirCheckArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            assertEquals(CandidateApplicability.RESOLVED, candidate.lowestApplicability)
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

            assertEquals(CandidateApplicability.INAPPLICABLE, candidate.lowestApplicability)
            assertTrue(candidate.diagnostics.any {
                it is ArgumentTypeMismatch
            })
        }

        @Test
        fun `Bool is not compatible with Int32`() {
            val symbol = buildFunctionSymbol("f", parameterTypes = listOf(ConePrimitiveType.INT32))
            val callInfo = buildCallInfo("f", arguments = listOf(buildTypedExpression(ConePrimitiveType.BOOLEAN)))
            val candidate = buildCandidate(symbol, callInfo)
            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            CfirCheckArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            assertEquals(CandidateApplicability.INAPPLICABLE, candidate.lowestApplicability)
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
                    buildTypedExpression(ConePrimitiveType.INT32), // 应该是 Bool
                ),
            )
            val candidate = buildCandidate(symbol, callInfo)
            CfirMapArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            CfirCheckArguments.check(candidate, CfirCheckerSinkImpl(candidate), context)

            assertEquals(CandidateApplicability.INAPPLICABLE, candidate.lowestApplicability)
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

            assertEquals(CandidateApplicability.RESOLVED, candidate.lowestApplicability)
        }
    }
}

// ---- Stub 对象 ----

/**
 * 参数检查测试使用的最小 session。
 */
private object StubSession : org.cangnova.cangjie.cfir.session.CfirSession(Kind.Source) {
    /**
     * 返回稳定的调试名称。
     */
    override fun toString(): String = "StubSession"
}

/** 支持 `IdealInt` / `IdealFloat` 子类型判定的测试 `TypeContext`。 */
private class TestTypeContext : ConeTypeContext {
    /**
     * 测试上下文不提供额外父类型。
     */
    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> = emptyList()
    /**
     * 按 primitive kind 或结构相等判断类型构造器一致性。
     */
    override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean {
        if (a is ConePrimitiveType && b is ConePrimitiveType) return a.kind == b.kind
        return a == b
    }
}
