@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.declarations.asResolveState
import org.cangnova.cangjie.cfir.resolve.CfirTypeRelations
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
 * `CfirOverloadConflictResolver` 的重载消歧测试。
 */
class CfirOverloadConflictResolverTest {

    /**
     * 每个测试前重新创建的重载冲突解析器。
     */
    private lateinit var resolver: CfirOverloadConflictResolver

    /**
     * 初始化支持 `Child <: Parent` 的重载解析器。
     */
    @BeforeEach
    fun setUp() {
        resolver = CfirOverloadConflictResolver(CfirTypeRelations(OverloadTestTypeContext()))
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

        }

        @Test
        fun `unrelated types remain ambiguous`() {
            // f(Boolean) vs f(Int32)，两者不存在子类型关系
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

            // f(Int32)，使用 0 个默认值参数
            val symbol1 = buildFunctionSymbol("f", parameterTypes = listOf(ConePrimitiveType.INT32))
            val candidate1 = buildCandidate(symbol1, callInfo)
            candidate1.numDefaults = 0

            // f(Int32, Bool = true)，使用 1 个默认值参数
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

    /**
     * 基于函数名和参数类型构造测试候选。
     */
    private fun makeCandidate(name: String, paramTypes: List<ConeCangJieType>): CfirCandidate {
        val symbol = buildFunctionSymbol(name, parameterTypes = paramTypes)
        val callInfo = buildCallInfo(name, paramTypes.map { buildTypedExpression(it) })
        return buildCandidate(symbol, callInfo)
    }

    @OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class, org.cangnova.cangjie.cfir.declarations.ResolveStateAccess::class)
    /**
     * 构造已经绑定并处于 BODY_RESOLVE 阶段的测试类型参数。
     */
    private fun makeStubTypeParameter(name: String): org.cangnova.cangjie.cfir.declarations.CfirTypeParameter {
        val symbol = org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol()
        val tp = org.cangnova.cangjie.cfir.declarations.impl.CfirTypeParameterImpl(
            source = null,
            moduleData = org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.TEST_MODULE_DATA,
            annotations = emptyList(),
            symbol = symbol,
            origin = org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin.Source,
            attributes = org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes.EMPTY,
            name = Name.identifier(name),
            bounds = emptyList(),
        )
        tp.resolveState = org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.BODY_RESOLVE.asResolveState()
        symbol.bind(tp)
        return tp
    }
}

/**
 * 测试用 `TypeContext`，支持 `Child <: Parent`。
 */
private class OverloadTestTypeContext : ConeTypeContext {
    /**
     * 为 Child 提供 Parent 作为直接父类型。
     */
    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> {
        // Child 的直接超类型包含 Parent
        if (type is ConeClassLikeType && type.classId == TYPE_CHILD.classId) {
            return listOf(TYPE_PARENT)
        }
        return emptyList()
    }

    /**
     * 按 primitive kind 或 class id 判断类型构造器一致性。
     */
    override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean {
        if (a is ConePrimitiveType && b is ConePrimitiveType) return a.kind == b.kind
        if (a is ConeClassLikeType && b is ConeClassLikeType) return a.classId == b.classId
        return a == b
    }
}

/** 测试用类型：`Child <: Parent`。 */
private val TYPE_PARENT = ConeClassLikeType(ConeClassLookupTagImpl(ClassId(FqName("test"), Name.identifier("Parent"))))
/** 测试用子类型：`Child <: Parent`。 */
private val TYPE_CHILD = ConeClassLikeType(ConeClassLookupTagImpl(ClassId(FqName("test"), Name.identifier("Child"))))
