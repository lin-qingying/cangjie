@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.declarations.ResolveStateAccess::class,
)

package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.asResolveState
import org.cangnova.cangjie.cfir.declarations.impl.CfirTypeParameterImpl
import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveContext
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.body.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.CfirInferenceComponents
import org.cangnova.cangjie.cfir.resolve.CfirTypeRelations
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.TEST_MODULE_DATA
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCandidate
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildFunctionSymbol
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildTypedExpression
import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.semantics.CandidateApplicability
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeContext
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [CfirMapTypeArguments] 泛型类型实参推断阶段测试。
 */
class CfirMapTypeArgumentsTest {
    /**
     * 当前测试使用的 resolution context。
     */
    private lateinit var context: CfirResolutionContext
    /**
     * 支持 Parent/Child 关系的推断测试类型上下文。
     */
    private val inferenceTypeContext = InferenceStageTypeContext()

    /**
     * 初始化包含 inference components 的测试上下文。
     */
    @BeforeEach
    fun setUp() {
        context = CfirResolutionContext(
            session = InferStubSession,
            bodyResolveContext = CfirBodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext()),
            typeRelations = CfirTypeRelations(inferenceTypeContext),
            inferenceComponents = CfirInferenceComponents(InferStubSession, inferenceTypeContext),
        )
    }

    /**
     * 验证从 invariant class 类型实参中推断类型变量。
     */
    @Test
    fun `infer from invariant class type argument`() {
        val boxId = ClassId(FqName("test"), Name.identifier("Box"))
        val typeParameter = makeTypeParameter("T")
        val tType = ConeTypeParameterType(ConeTypeParameterLookupTag("T"))

        val symbol = buildFunctionSymbol(
            name = "id",
            returnType = tType,
            parameterTypes = listOf(ConeClassLikeType(ConeClassLookupTagImpl(boxId), listOf(tType))),
            typeParameters = listOf(typeParameter),
        )
        val argType = ConeClassLikeType(ConeClassLookupTagImpl(boxId), listOf(ConePrimitiveType.INT32))
        val callInfo = buildCallInfo("id", arguments = listOf(buildTypedExpression(argType)))
        val candidate = buildCandidate(symbol, callInfo)
        val sink = CfirCheckerSinkImpl(candidate)

        CfirMapTypeArguments.check(candidate, sink, context)
        CfirCreateFreshTypeVariableSubstitutorStage.check(candidate, sink, context)
        CfirMapArguments.check(candidate, sink, context)
        CfirCheckArguments.check(candidate, sink, context)

        assertEquals(ConePrimitiveType.INT32, candidate.substitutedReturnType())
        assertTrue(candidate.constraintSystem?.errors?.isEmpty() == true)
    }

    /**
     * 验证声明上界参与类型变量求解。
     */
    @Test
    fun `declaration upper bound should participate in solving`() {
        val parentId = ClassId(FqName("test"), Name.identifier("Parent"))
        val childId = ClassId(FqName("test"), Name.identifier("Child"))
        val parentType = ConeClassLikeType(ConeClassLookupTagImpl(parentId))
        val childType = ConeClassLikeType(ConeClassLookupTagImpl(childId))
        val typeParameter = makeTypeParameter("T", bounds = listOf(parentType))
        val tType = ConeTypeParameterType(ConeTypeParameterLookupTag("T"))

        val symbol = buildFunctionSymbol(
            name = "pick",
            returnType = tType,
            parameterTypes = listOf(tType),
            typeParameters = listOf(typeParameter),
        )
        val callInfo = buildCallInfo("pick", arguments = listOf(buildTypedExpression(childType)))
        val candidate = buildCandidate(symbol, callInfo)
        val sink = CfirCheckerSinkImpl(candidate)

        CfirMapTypeArguments.check(candidate, sink, context)
        CfirCreateFreshTypeVariableSubstitutorStage.check(candidate, sink, context)
        CfirMapArguments.check(candidate, sink, context)
        CfirCheckArguments.check(candidate, sink, context)

        assertEquals(childType, candidate.substitutedReturnType())
        assertTrue(candidate.constraintSystem?.errors?.isEmpty() == true)
    }

    /**
     * 验证违反声明上界会把候选标记为不可用。
     */
    @Test
    fun `conflicting declaration bound should mark candidate inapplicable`() {
        val parentId = ClassId(FqName("test"), Name.identifier("Parent"))
        val parentType = ConeClassLikeType(ConeClassLookupTagImpl(parentId))
        val typeParameter = makeTypeParameter("T", bounds = listOf(parentType))
        val tType = ConeTypeParameterType(ConeTypeParameterLookupTag("T"))

        val symbol = buildFunctionSymbol(
            name = "bad",
            returnType = tType,
            parameterTypes = listOf(tType),
            typeParameters = listOf(typeParameter),
        )
        val callInfo = buildCallInfo("bad", arguments = listOf(buildTypedExpression(ConePrimitiveType.BOOLEAN)))
        val candidate = buildCandidate(symbol, callInfo)
        val sink = CfirCheckerSinkImpl(candidate)

        CfirMapTypeArguments.check(candidate, sink, context)
        CfirCreateFreshTypeVariableSubstitutorStage.check(candidate, sink, context)
        CfirMapArguments.check(candidate, sink, context)
        CfirCheckArguments.check(candidate, sink, context)

        assertEquals(CandidateApplicability.INAPPLICABLE, candidate.lowestApplicability)
        assertTrue(candidate.diagnostics.any { it is ArgumentTypeMismatch })
    }

    /**
     * 验证 expected return type 可以反向约束泛型返回类型推断。
     */
    @Test
    fun `expected return type should constrain generic return inference`() {
        val boxId = ClassId(FqName("test"), Name.identifier("Box"))
        val typeParameter = makeTypeParameter("T")
        val tType = ConeTypeParameterType(ConeTypeParameterLookupTag("T"))
        val boxOfT = ConeClassLikeType(ConeClassLookupTagImpl(boxId), listOf(tType))
        val boxOfInt = ConeClassLikeType(ConeClassLookupTagImpl(boxId), listOf(ConePrimitiveType.INT32))

        val symbol = buildFunctionSymbol(
            name = "box",
            returnType = boxOfT,
            parameterTypes = emptyList(),
            typeParameters = listOf(typeParameter),
        )
        val callInfo = buildCallInfo("box")
        val candidate = buildCandidate(symbol, callInfo)
        val returnConstrainedContext = CfirResolutionContext(
            session = context.session,
            bodyResolveContext = context.bodyResolveContext,
            typeRelations = context.typeRelations,
            inferenceComponents = context.inferenceComponents,
            expectedType = boxOfInt,
        )
        val sink = CfirCheckerSinkImpl(candidate)

        CfirMapTypeArguments.check(candidate, sink, returnConstrainedContext)
        CfirCreateFreshTypeVariableSubstitutorStage.check(candidate, sink, returnConstrainedContext)
        CfirMapArguments.check(candidate, sink, returnConstrainedContext)
        CfirCheckArguments.check(candidate, sink, returnConstrainedContext)

        assertEquals(boxOfInt, candidate.substitutedReturnType())
        assertTrue(candidate.constraintSystem?.errors?.isEmpty() == true)
        assertTrue(candidate.constraintSystem?.buildResult()?.isFullyResolved == true)
    }

    /**
     * 验证 identity 形态函数的参数和返回类型推断为同一具体类型。
     */
    @Test
    fun `identity should infer same type for parameter and return`() {
        val typeParameter = makeTypeParameter("T")
        val tType = ConeTypeParameterType(ConeTypeParameterLookupTag("T"))

        val symbol = buildFunctionSymbol(
            name = "identity",
            returnType = tType,
            parameterTypes = listOf(tType),
            typeParameters = listOf(typeParameter),
        )
        val callInfo = buildCallInfo("identity", arguments = listOf(buildTypedExpression(ConePrimitiveType.INT64)))
        val candidate = buildCandidate(symbol, callInfo)
        val sink = CfirCheckerSinkImpl(candidate)

        CfirMapTypeArguments.check(candidate, sink, context)
        CfirCreateFreshTypeVariableSubstitutorStage.check(candidate, sink, context)
        CfirMapArguments.check(candidate, sink, context)
        CfirCheckArguments.check(candidate, sink, context)

        assertEquals(ConePrimitiveType.INT64, candidate.substitutedReturnType())
        assertTrue(candidate.constraintSystem?.errors?.isEmpty() == true)
    }

    /**
     * 构造带可选上界的测试类型参数。
     */
    private fun makeTypeParameter(
        name: String,
        bounds: List<ConeCangJieType> = emptyList(),
    ): CfirTypeParameterImpl {
        val symbol = CfirTypeParameterSymbol()
        val boundRefs = bounds.map { boundType ->
            CfirResolvedTypeRefImpl(
                source = null,
                annotations = emptyList(),
                coneType = boundType,
                delegatedTypeRef = null,
            )
        }
        val typeParameter = CfirTypeParameterImpl(
            source = null,
            moduleData = TEST_MODULE_DATA,
            annotations = emptyList(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Source,
            attributes = CfirDeclarationAttributes.EMPTY,
            name = Name.identifier(name),
            bounds = boundRefs,
        )
        typeParameter.resolveState = CfirResolvePhase.BODY_RESOLVE.asResolveState()
        symbol.bind(typeParameter)
        return typeParameter
    }
}

/**
 * 泛型推断阶段测试使用的最小 session。
 */
private object InferStubSession : org.cangnova.cangjie.cfir.session.CfirSession(Kind.Source) {
    /**
     * 返回稳定的调试名称。
     */
    override fun toString(): String = "StubSession"
}

/**
 * 泛型推断阶段测试使用的类型上下文。
 */
private class InferenceStageTypeContext : ConeTypeContext {
    /**
     * Child 的父类型。
     */
    private val parent = ConeClassLikeType(
        lookupTag = ConeClassLookupTagImpl(ClassId(FqName("test"), Name.identifier("Parent"))),
    )

    /**
     * 为名为 Child 的 class-like 类型提供 Parent 父类型。
     */
    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> {
        if (type is ConeClassLikeType && type.classId.shortClassName.asString() == "Child") {
            return listOf(parent)
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
