@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.TestSession
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCandidate
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildFunctionSymbol
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildTypedExpression
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.newResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.newStubBodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.newTestSession
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.resolve.calls.results.TypeSpecificityComparator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * [ConeOverloadConflictResolver] 测试。
 *
 * 候选消歧只依赖声明签名，不需要预先运行检查阶段；
 * 候选集合大于 1 时消歧会访问 `session.extendProvider`，
 * 因此每个测试使用带空 [org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider] 的独立 session。
 */
class CfirOverloadConflictResolverTest {

    private lateinit var session: TestSession
    private lateinit var context: ResolutionContext
    private lateinit var resolver: ConeOverloadConflictResolver

    @BeforeEach
    fun setUp() {
        session = newTestSession()
        context = newResolutionContext(session)
        resolver = ConeOverloadConflictResolver(
            TypeSpecificityComparator.NONE,
            session.inferenceComponents,
            newStubBodyResolveComponents(session),
        )
    }

    @Nested
    inner class SingleCandidate {
        @Test
        fun `single candidate should be returned as is`() {
            val candidate = buildCandidate(session, buildFunctionSymbol(session, "f"), buildCallInfo(session, "f"))

            assertEquals(setOf(candidate), resolver.chooseMaximallySpecificCandidates(listOf(candidate)))
        }
    }

    @Nested
    inner class SpecificityComparison {
        @Test
        fun `more specific parameter type wins`() {
            val lessSpecific = buildFunctionSymbol(session, "f", parameterTypes = listOf(ConePrimitiveType.INT32))
            val moreSpecific = buildFunctionSymbol(session, "f", parameterTypes = listOf(ConePrimitiveType.INT64))
            val callInfo = buildCallInfo(session, "f", arguments = listOf(buildTypedExpression(ConePrimitiveType.INT32)))
            val candidates = setOf(
                buildCandidate(session, lessSpecific, callInfo),
                buildCandidate(session, moreSpecific, callInfo),
            )

            val result = resolver.chooseMaximallySpecificCandidates(candidates)

            assertEquals(1, result.size)
            assertSame(moreSpecific, result.single().symbol)
        }

        @Test
        fun `unrelated parameter types remain ambiguous`() {
            val int32 = buildFunctionSymbol(session, "f", parameterTypes = listOf(ConePrimitiveType.INT32))
            val boolean = buildFunctionSymbol(session, "f", parameterTypes = listOf(ConePrimitiveType.BOOLEAN))
            val callInfo = buildCallInfo(session, "f", arguments = listOf(buildTypedExpression(ConePrimitiveType.INT32)))
            val candidates = setOf(
                buildCandidate(session, int32, callInfo),
                buildCandidate(session, boolean, callInfo),
            )

            val result = resolver.chooseMaximallySpecificCandidates(candidates)

            assertEquals(2, result.size)
        }
    }

    @Nested
    inner class GenericDiscrimination {
        @Test
        fun `non generic candidate wins over generic candidate`() {
            val t = ExtendTestFixtures.newTypeParameter(session.moduleData, "T")
            val generic = buildFunctionSymbol(
                session, "f",
                returnType = ExtendTestFixtures.typeParameterType(t),
                parameterTypes = listOf(ConePrimitiveType.INT32),
                typeParameters = listOf(t),
            )
            val nonGeneric = buildFunctionSymbol(
                session, "f",
                returnType = ConePrimitiveType.INT32,
                parameterTypes = listOf(ConePrimitiveType.INT32),
            )
            val callInfo = buildCallInfo(session, "f", arguments = listOf(buildTypedExpression(ConePrimitiveType.INT32)))
            val candidates = setOf(
                buildCandidate(session, generic, callInfo),
                buildCandidate(session, nonGeneric, callInfo),
            )

            val result = resolver.chooseMaximallySpecificCandidates(candidates)

            assertEquals(1, result.size)
            assertSame(nonGeneric, result.single().symbol)
        }
    }
}
