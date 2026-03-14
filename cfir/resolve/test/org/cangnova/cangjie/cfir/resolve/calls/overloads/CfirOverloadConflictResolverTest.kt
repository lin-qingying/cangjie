@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCandidate
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildFunctionSymbol
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildTypedExpression
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * CfirOverloadConflictResolver 重载消歧测试。
 */
class CfirOverloadConflictResolverTest {

    private lateinit var resolver: CfirOverloadConflictResolver

    @BeforeEach
    fun setUp() {
        val subtypeChecker = ConeSubtypeChecker(OverloadTestTypeContext())
        resolver = CfirOverloadConflictResolver(subtypeChecker)
    }

    @Nested
    inner class SingleCandidate {

        @Test
        fun `single candidate returns itself`() {
            val candidate = makeCandidate("f", listOf(ConePrimitiveType.INT32))
            val result = resolver.chooseMaximallySpecificCandidates(setOf(candidate))
            assertEquals(1, result.size)
            assertSame(candidate, result.single())
        }
    }

    @Nested
    inner class SpecificityComparison {

        @Test
        fun `more specific parameter type wins`() {
            // f(Child) vs f(Parent)，Child <: Parent → f(Child) 更特定
            val candidateChild = makeCandidate("f", listOf(TYPE_CHILD))
            val candidateParent = makeCandidate("f", listOf(TYPE_PARENT))

            val result = resolver.chooseMaximallySpecificCandidates(setOf(candidateChild, candidateParent))

            assertEquals(1, result.size)
            assertSame(candidateChild, result.single())
        }

        @Test
        fun `unrelated types remain ambiguous`() {
            // f(Boolean) vs f(Int32) — 无子类型关系
            val candidateBool = makeCandidate("f", listOf(ConePrimitiveType.BOOLEAN))
            val candidateInt = makeCandidate("f", listOf(ConePrimitiveType.INT32))

            val result = resolver.chooseMaximallySpecificCandidates(setOf(candidateBool, candidateInt))

            assertEquals(2, result.size)
        }
    }

    @Nested
    inner class GenericDiscrimination {

        @Test
        fun `non-generic wins over generic`() {
            // f(Int32) vs f<T>(T)
            val nonGenericSymbol = buildFunctionSymbol("f", parameterTypes = listOf(ConePrimitiveType.INT32))
            val genericSymbol = buildFunctionSymbol(
                "f",
                parameterTypes = listOf(ConePrimitiveType.INT32),
                typeParameters = listOf(makeStubTypeParameter("T")),
            )

            val callInfo = buildCallInfo("f", listOf(buildTypedExpression(ConePrimitiveType.INT32)))
            val nonGeneric = buildCandidate(nonGenericSymbol, callInfo)
            val generic = buildCandidate(genericSymbol, callInfo)

            val result = resolver.chooseMaximallySpecificCandidates(setOf(nonGeneric, generic))

            assertEquals(1, result.size)
            assertSame(nonGeneric, result.single())
        }
    }

    @Nested
    inner class DefaultsDiscrimination {

        @Test
        fun `fewer defaults wins`() {
            val callInfo = buildCallInfo("f", listOf(buildTypedExpression(ConePrimitiveType.INT32)))

            // f(Int32) — 0 defaults
            val symbol1 = buildFunctionSymbol("f", parameterTypes = listOf(ConePrimitiveType.INT32))
            val candidate1 = buildCandidate(symbol1, callInfo)
            candidate1.numDefaults = 0

            // f(Int32, Bool = true) — 1 default
            val symbol2 = buildFunctionSymbol(
                "f",
                parameterTypes = listOf(ConePrimitiveType.INT32, ConePrimitiveType.BOOLEAN),
                parameterDefaults = listOf(false, true),
            )
            val candidate2 = buildCandidate(symbol2, callInfo)
            candidate2.numDefaults = 1

            val result = resolver.chooseMaximallySpecificCandidates(setOf(candidate1, candidate2))

            assertEquals(1, result.size)
            assertSame(candidate1, result.single())
        }
    }

    // ---- 辅助方法 ----

    private fun makeCandidate(name: String, paramTypes: List<ConeCangjieType>): CfirCandidate {
        val symbol = buildFunctionSymbol(name, parameterTypes = paramTypes)
        val callInfo = buildCallInfo(name, paramTypes.map { buildTypedExpression(it) })
        return buildCandidate(symbol, callInfo)
    }

    @OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)
    private fun makeStubTypeParameter(name: String): org.cangnova.cangjie.cfir.declarations.CfirTypeParameter {
        val symbol = org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol()
        val tp = org.cangnova.cangjie.cfir.declarations.impl.CfirTypeParameterImpl(
            symbol = symbol,
            origin = org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin.Source,
            annotations = emptyList(),
            moduleData = org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.TEST_MODULE_DATA,
            resolvePhase = org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.BODY_RESOLVE,
            attributes = org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes.EMPTY,
            name = Name.identifier(name),
            bounds = emptyList(),
        )
        symbol.bind(tp)
        return tp
    }
}

/**
 * TypeContext：支持 Child <: Parent（用于特化度比较测试）。
 */
private class OverloadTestTypeContext : ConeTypeContext {
    override fun supertypes(type: ConeCangjieType): Collection<ConeCangjieType> {
        // Child 的超类型包含 Parent
        if (type is ConeClassLikeType && type.classId == TYPE_CHILD.classId) {
            return listOf(TYPE_PARENT)
        }
        return emptyList()
    }

    override fun isSameTypeConstructor(a: ConeCangjieType, b: ConeCangjieType): Boolean {
        if (a is ConePrimitiveType && b is ConePrimitiveType) return a.kind == b.kind
        if (a is ConeClassLikeType && b is ConeClassLikeType) return a.classId == b.classId
        return a == b
    }
}

/** 测试用类型：Child <: Parent */
private val TYPE_PARENT = ConeClassLikeType(ConeClassLookupTagImpl(ClassId(FqName("test"), Name.identifier("Parent"))))
private val TYPE_CHILD = ConeClassLikeType(ConeClassLookupTagImpl(ClassId(FqName("test"), Name.identifier("Child"))))
