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
import org.cangnova.cangjie.cfir.resolve.body.CfirReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.TEST_MODULE_DATA
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildCandidate
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildFunctionSymbol
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures.buildTypedExpression
import org.cangnova.cangjie.cfir.resolve.calls.candidate.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceComponents
import org.cangnova.cangjie.cfir.semantics.CandidateApplicability
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker
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

class CfirInferTypeArgumentsTest {
    private lateinit var context: CfirResolutionContext

    @BeforeEach
    fun setUp() {
        val subtypeChecker = ConeSubtypeChecker(InferenceStageTypeContext())
        context = CfirResolutionContext(
            session = InferStubSession,
            bodyResolveContext = CfirBodyResolveContext(CfirReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext()),
            subtypeChecker = subtypeChecker,
            inferenceComponents = CfirInferenceComponents(subtypeChecker),
        )
    }

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

        CfirCreateFreshTypeVariableSubstitutorStage.check(candidate, sink, context)
        CfirMapArguments.check(candidate, sink, context)
        CfirCheckArguments.check(candidate, sink, context)
        CfirInferTypeArguments.check(candidate, sink, context)

        assertEquals(ConePrimitiveType.INT32, candidate.resolvedReturnType())
        assertTrue(candidate.constraintSystem?.errors?.isEmpty() == true)
    }

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

        CfirCreateFreshTypeVariableSubstitutorStage.check(candidate, sink, context)
        CfirMapArguments.check(candidate, sink, context)
        CfirCheckArguments.check(candidate, sink, context)
        CfirInferTypeArguments.check(candidate, sink, context)

        assertEquals(childType, candidate.resolvedReturnType())
        assertTrue(candidate.constraintSystem?.errors?.isEmpty() == true)
    }

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

        CfirCreateFreshTypeVariableSubstitutorStage.check(candidate, sink, context)
        CfirMapArguments.check(candidate, sink, context)
        CfirCheckArguments.check(candidate, sink, context)
        CfirInferTypeArguments.check(candidate, sink, context)

        assertEquals(CandidateApplicability.INAPPLICABLE, candidate.lowestApplicability)
        assertTrue(candidate.diagnostics.any { it is ArgumentTypeMismatch })
    }

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
            subtypeChecker = context.subtypeChecker,
            inferenceComponents = context.inferenceComponents,
            expectedType = boxOfInt,
        )
        val sink = CfirCheckerSinkImpl(candidate)

        CfirCreateFreshTypeVariableSubstitutorStage.check(candidate, sink, returnConstrainedContext)
        CfirMapArguments.check(candidate, sink, returnConstrainedContext)
        CfirCheckArguments.check(candidate, sink, returnConstrainedContext)
        CfirInferTypeArguments.check(candidate, sink, returnConstrainedContext)

        assertEquals(boxOfInt, candidate.resolvedReturnType())
        assertTrue(candidate.constraintSystem?.errors?.isEmpty() == true)
    }

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

        CfirCreateFreshTypeVariableSubstitutorStage.check(candidate, sink, context)
        CfirMapArguments.check(candidate, sink, context)
        CfirCheckArguments.check(candidate, sink, context)
        CfirInferTypeArguments.check(candidate, sink, context)

        assertEquals(ConePrimitiveType.INT64, candidate.resolvedReturnType())
        assertTrue(candidate.constraintSystem?.errors?.isEmpty() == true)
    }

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

private object InferStubSession : org.cangnova.cangjie.cfir.session.CfirSession(Kind.Source) {
    override fun toString(): String = "StubSession"
}

private class InferenceStageTypeContext : ConeTypeContext {
    private val parent = ConeClassLikeType(
        lookupTag = ConeClassLookupTagImpl(ClassId(FqName("test"), Name.identifier("Parent"))),
    )

    override fun supertypes(type: ConeCangJieType): Collection<ConeCangJieType> {
        if (type is ConeClassLikeType && type.classId.shortClassName.asString() == "Child") {
            return listOf(parent)
        }
        return emptyList()
    }

    override fun isSameTypeConstructor(a: ConeCangJieType, b: ConeCangJieType): Boolean {
        if (a is ConePrimitiveType && b is ConePrimitiveType) return a.kind == b.kind
        if (a is ConeClassLikeType && b is ConeClassLikeType) return a.classId == b.classId
        return a == b
    }
}
