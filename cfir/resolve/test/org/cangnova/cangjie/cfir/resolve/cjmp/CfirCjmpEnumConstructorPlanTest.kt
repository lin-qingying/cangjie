package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.cfir.containingClassForStaticMemberAttr
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildEnum
import org.cangnova.cangjie.cfir.declarations.builder.buildEnumConstructor
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeAlias
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameterCopy
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirCjmpCandidateDiagnostic
import org.cangnova.cangjie.cfir.session.defaultCfirCjmpCommonSideFacts
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.CfirCjmpPreparedGroup
import org.cangnova.cangjie.cfir.session.CfirCjmpClassifierDomain
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.mpp.CjmpBindingRule
import org.cangnova.cangjie.resolve.calls.mpp.AbstractCjmpMatcher
import org.cangnova.cangjie.resolve.calls.mpp.CjmpCallableKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpClassLikeKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpCandidateTraversal
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchEvaluator
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchEvent
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchResult
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchPlan
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchingContext
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchingValueParameter
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMismatchKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpTypeCompatibility
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 真实 enum 与普通函数共用 callable 计划，保留 enum 完整类型等价、真实参数与占用顺序。 */
class CfirCjmpEnumConstructorPlanTest {
    private val session = CallResolutionTestFixtures.newTestSession()
    private val context = CfirCjmpMatchingContext(session)

    @Test
    fun nullarySpecificPlansCompeteWithoutOverwritingTheFirstImplementation() {
        val common = variant(owner(), "Empty")
        val first = variant(owner(), "Empty")
        val second = variant(owner(), "Empty")
        val firstPlan = plan(first, listOf(common))
        val secondPlan = plan(second, listOf(common))
        val result = CjmpMatchEvaluator.evaluate(listOf(firstPlan, secondPlan))

        assertSame(first.symbol, result.implementations[common.symbol])
        assertEquals(mapOf(first.symbol to true, second.symbol to false), result.matched)
        assertEquals(listOf(CjmpMatchResult.Matched, CjmpMatchResult.Mismatched(CjmpMismatchKind.SECOND_BINDING)), result.visits.map { it.result })
        assertEquals(listOf(first.symbol, second.symbol), result.visits.map { it.specific })
        assertEquals(listOf(common.symbol, common.symbol), result.visits.map { it.common })
        assertEquals(
            listOf(CjmpMatchEvent.Occupied<CfirBasedSymbol<*>, String>(second.symbol, common.symbol, first.symbol)),
            result.events,
        )
        assertEquals(1, result.acceptedBindings.size)
        assertEquals(setOf(common.symbol), result.doNotExport)
    }

    @Test
    fun payloadTypeMismatchIsVisitedBeforeTheFirstSuccessAndTheTailIsNotVisited() {
        val specific = variant(owner(), payload = listOf(ConePrimitiveType.BOOLEAN))
        val wrong = variant(owner(), payload = listOf(ConePrimitiveType.INT64))
        val matching = variant(owner(), payload = listOf(ConePrimitiveType.BOOLEAN))
        val unvisited = variant(owner(), payload = listOf(ConePrimitiveType.BOOLEAN))
        val plan = plan(specific, listOf(wrong, matching, unvisited))
        assertEquals(CjmpCandidateTraversal.FIRST_SUCCESS, plan.traversal)
        assertEquals(1, plan.initialDefaults.size)
        assertTrue(plan.initialDefaults.all { !it.hasInitial && it.desugaredDefaultSource == null })
        assertTrue(plan.candidates.all { candidate ->
            candidate.parameters.size == 1 && candidate.parameters.all { parameter ->
                parameter.namesMatch && !parameter.commonDefault.hasInitial && parameter.commonDefault.desugaredDefaultSource == null
            }
        }, "every real enum payload parameter has neutral facts")
        val result = CjmpMatchEvaluator.evaluate(listOf(plan))

        assertEquals(listOf(wrong.symbol, matching.symbol), result.visits.map { it.common })
        assertEquals(listOf(CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE), CjmpMatchResult.Matched), result.visits.map { it.result })
        assertEquals(mapOf(matching.symbol to specific.symbol), result.implementations)
        assertEquals(true, result.matched[specific.symbol])
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun outerMappingAndSignatureRemainFrozenAfterCfirTypeReferencesChange() {
        val commonOwner = owner(typeParameterNames = listOf("T"))
        val specificOwner = owner(typeParameterNames = listOf("U"))
        val commonParameter = commonOwner.typeParameters.single()
        val specificParameter = specificOwner.typeParameters.single()
        val mapping = linkedMapOf(commonParameter to specificParameter)
        val common = variant(commonOwner, payload = listOf(ExtendTestFixtures.typeParameterType(commonParameter)))
        val specific = variant(specificOwner, payload = listOf(ExtendTestFixtures.typeParameterType(specificParameter)))
        assertEquals(CjmpMatchResult.Matched, signature(specific, common, mapping))
        val plan = plan(specific, listOf(common), mapping)
        val expectedMapping = mapOf(commonParameter.symbol to specificParameter.symbol)
        val frozen = plan.candidates.single()
        val binding = frozen.binding as CjmpBindingRule.Callable<CfirBasedSymbol<*>, CfirTypeParameterSymbol>
        assertEquals(expectedMapping, binding.request.typeParameterMapping)

        mapping.clear()
        common.valueParameters.single().replaceReturnTypeRef(ConePrimitiveType.UNIT.toCfirResolvedTypeRef(null))
        specific.replaceReturnTypeRef(enumType(owner("Other")).toCfirResolvedTypeRef(null))
        assertEquals(CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE), signature(specific, common))
        assertEquals(CjmpMatchResult.Matched, frozen.signatureCompatibility)
        val result = CjmpMatchEvaluator.evaluate(listOf(plan))
        assertEquals(true, result.matched[specific.symbol])
        assertSame(specific.symbol, result.implementations[common.symbol])
        assertEquals(expectedMapping, result.acceptedBindings.single().typeParameterMapping)
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun equalPayloadDoesNotAllowDifferentEnumReturnDeclarations() {
        val common = variant(owner("E"), payload = listOf(ConePrimitiveType.INT64))
        val specific = variant(owner("Other"), payload = listOf(ConePrimitiveType.INT64))
        assertEquals(CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE), signature(specific, common))
    }

    @Test
    fun equalPayloadDoesNotAllowDifferentConcreteEnumReturnArguments() {
        val commonOwner = owner(typeParameterNames = listOf("T"))
        val specificOwner = owner(typeParameterNames = listOf("U"))
        val common = variant(commonOwner, payload = listOf(ConePrimitiveType.INT64))
        val specific = variant(specificOwner, payload = listOf(ConePrimitiveType.INT64))
        common.replaceReturnTypeRef(enumType(commonOwner, listOf(ConePrimitiveType.INT64)).toCfirResolvedTypeRef(null))
        specific.replaceReturnTypeRef(enumType(specificOwner, listOf(ConePrimitiveType.BOOLEAN)).toCfirResolvedTypeRef(null))

        assertEquals(CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE), signature(specific, common))
    }

    @Test
    fun nullaryConstructorNeedsNeitherAFunctionBodyNorReturnTypeComparison() {
        val common = variant(owner(), "Empty")
        val specific = variant(owner(), "Empty")
        specific.replaceReturnTypeRef(buildImplicitTypeRef())
        assertEquals(CjmpMatchResult.Matched, signature(specific, common))
        val plan = plan(specific, listOf(common))
        val binding = plan.candidates.single().binding as CjmpBindingRule.Callable<CfirBasedSymbol<*>, CfirTypeParameterSymbol>
        assertFalse(binding.request.requiresImplementation)
        assertTrue(plan.initialDefaults.isEmpty())
        assertTrue(plan.candidates.single().parameters.isEmpty())
        val result = CjmpMatchEvaluator.evaluate(listOf(plan))

        assertEquals(true, result.matched[specific.symbol])
        assertSame(specific.symbol, result.implementations[common.symbol])
        assertTrue(result.events.isEmpty(), "an enum variant has no ordinary function body requirement")
    }

    @Test
    fun cfirCallableKindsFollowEnumPayloadShapeAndFunctionDeclarationKind() {
        val enumOwner = owner()

        assertEquals(CjmpCallableKind.VARIABLE, CfirCjmpMatcher.callableKind(variant(enumOwner, "Empty")))
        assertEquals(
            CjmpCallableKind.FUNCTION,
            CfirCjmpMatcher.callableKind(variant(enumOwner, payload = listOf(ConePrimitiveType.INT64))),
        )
        assertEquals(CjmpCallableKind.FUNCTION, CfirCjmpMatcher.callableKind(function(enumOwner, listOf(ConePrimitiveType.INT64))))
    }

    @Test
    fun nominalTypeAliasKindDoesNotFallThroughToClass() {
        val classDeclaration = classDeclaration("Box")
        val aliasDeclaration = typeAlias("Box")

        assertEquals(CjmpClassLikeKind.CLASS, context.classLikeKind(classDeclaration.symbol))
        assertEquals(CjmpClassLikeKind.TYPE_ALIAS, context.classLikeKind(aliasDeclaration.symbol))
        assertEquals(
            CjmpMatchResult.Mismatched(CjmpMismatchKind.CLASS_KIND),
            AbstractCjmpMatcher.matchEligibility(aliasDeclaration.symbol, classDeclaration.symbol, context),
        )
    }

    @Test
    fun genericFunctionTypeParameterIsSubstitutedInsideNestedFunctionType() {
        val common = genericFunction("apply") { typeParameter ->
            ConeFunctionType(listOf(ExtendTestFixtures.typeParameterType(typeParameter)), ConePrimitiveType.UNIT)
        }
        val specific = genericFunction("apply") { typeParameter ->
            ConeFunctionType(listOf(ExtendTestFixtures.typeParameterType(typeParameter)), ConePrimitiveType.UNIT)
        }

        assertEquals(CjmpMatchResult.Matched, signature(specific, common))
        val result = CjmpMatchEvaluator.evaluate(listOf(plan(specific, listOf(common))))
        assertEquals(true, result.matched[specific.symbol])
        assertSame(specific.symbol, result.implementations[common.symbol])
    }

    @Test
    fun genericFunctionTypeParameterIsSubstitutedInsideTupleParameterType() {
        val common = genericFunction("pair") { typeParameter ->
            ConeTupleType(listOf(ExtendTestFixtures.typeParameterType(typeParameter), ConePrimitiveType.INT64))
        }
        val specific = genericFunction("pair") { typeParameter ->
            ConeTupleType(listOf(ExtendTestFixtures.typeParameterType(typeParameter), ConePrimitiveType.INT64))
        }

        assertEquals(CjmpMatchResult.Matched, signature(specific, common))
        val result = CjmpMatchEvaluator.evaluate(listOf(plan(specific, listOf(common))))
        assertEquals(true, result.matched[specific.symbol])
        assertSame(specific.symbol, result.implementations[common.symbol])
    }

    @Test
    fun genericEnumFunctionPayloadUsesTheOuterTypeParameterMapping() {
        val commonOwner = owner(typeParameterNames = listOf("T"))
        val specificOwner = owner(typeParameterNames = listOf("U"))
        val commonTypeParameter = commonOwner.typeParameters.single()
        val specificTypeParameter = specificOwner.typeParameters.single()
        val common = variant(
            commonOwner,
            payload = listOf(ConeFunctionType(listOf(ExtendTestFixtures.typeParameterType(commonTypeParameter)), ConePrimitiveType.UNIT)),
        )
        val specific = variant(
            specificOwner,
            payload = listOf(ConeFunctionType(listOf(ExtendTestFixtures.typeParameterType(specificTypeParameter)), ConePrimitiveType.UNIT)),
        )
        val mapping = mapOf(commonTypeParameter to specificTypeParameter)

        assertEquals(CjmpMatchResult.Matched, signature(specific, common, mapping))
        val result = CjmpMatchEvaluator.evaluate(listOf(plan(specific, listOf(common), mapping)))
        assertEquals(true, result.matched[specific.symbol])
        assertSame(specific.symbol, result.implementations[common.symbol])
    }

    @Test
    fun payloadMismatchReportsTheCompleteEnumFunctionTypeDifference() {
        val common = variant(owner(), payload = listOf(ConePrimitiveType.INT64))
        val specific = variant(owner(), payload = listOf(ConePrimitiveType.BOOLEAN))
        assertEquals(CjmpMatchResult.Mismatched(CjmpMismatchKind.FUNCTION_TYPE), signature(specific, common))
    }

    @Test
    fun unresolvedPayloadEnumReturnIsNotAnOrdinaryFunctionQuest() {
        val common = variant(owner(), payload = listOf(ConePrimitiveType.INT64))
        val specific = variant(owner(), payload = listOf(ConePrimitiveType.INT64))
        specific.replaceReturnTypeRef(buildImplicitTypeRef())
        assertEquals(CjmpMatchResult.TypeNotResolved, signature(specific, common))
    }

    @Test
    fun specificPayloadEnumMatchesARealCommonFunction() {
        val common = function(owner(), listOf(ConePrimitiveType.INT64))
        val specific = variant(owner(), payload = listOf(ConePrimitiveType.INT64))
        assertEquals(CjmpMatchResult.Matched, signature(specific, common))
        val result = CjmpMatchEvaluator.evaluate(listOf(plan(specific, listOf(common))))
        assertEquals(true, result.matched[specific.symbol])
        assertSame(specific.symbol, result.implementations[common.symbol])
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun specificFunctionMatchesARealCommonPayloadEnum() {
        val common = variant(owner(), payload = listOf(ConePrimitiveType.INT64))
        val specific = function(owner(), listOf(ConePrimitiveType.INT64))
        assertEquals(CjmpMatchResult.Matched, signature(specific, common))
        val result = CjmpMatchEvaluator.evaluate(listOf(plan(specific, listOf(common))))
        assertEquals(true, result.matched[specific.symbol])
        assertSame(specific.symbol, result.implementations[common.symbol])
        assertTrue(result.events.isEmpty())
    }

    @Test
    fun mixedPayloadCandidatesCheckOrdinaryNamedParameterShapeInBothDirections() {
        val pairs = listOf(
            variant(owner(), payload = listOf(ConePrimitiveType.INT64)) to function(owner(), listOf(ConePrimitiveType.INT64), named = true),
            function(owner(), listOf(ConePrimitiveType.INT64), named = true) to variant(owner(), payload = listOf(ConePrimitiveType.INT64)),
        )
        for ((specific, common) in pairs) {
            assertEquals(CjmpMatchResult.Matched, signature(specific, common))
            val result = CjmpMatchEvaluator.evaluate(listOf(plan(specific, listOf(common))))
            assertEquals(false, result.matched[specific.symbol])
            val event = result.events.single() as CjmpMatchEvent.ParameterMismatch<*, *>
            assertEquals(CjmpMismatchKind.PARAMETER_NAMES, event.mismatch.kind)
            assertEquals(0, event.mismatch.parameterIndex)
            assertTrue(result.implementations.isEmpty())
        }
    }

    @Test
    fun nullaryEnumAndFunctionRejectEligibilityBeforeReadingSignature() {
        val common = variant(owner(), "Empty")
        val specific = function(owner(), emptyList(), "Empty")
        val eligibilityContext = object : CjmpMatchingContext<CfirBasedSymbol<*>, CfirTypeParameterSymbol, ConeCangJieType> by context {
            override fun valueParameters(declaration: CfirBasedSymbol<*>): List<CjmpMatchingValueParameter<ConeCangJieType>> =
                error("rejected eligibility must not read parameter facts")

            override fun areReturnTypesCompatible(
                specific: CfirBasedSymbol<*>,
                common: CfirBasedSymbol<*>,
                typeParameterMapping: Map<CfirTypeParameterSymbol, CfirTypeParameterSymbol>,
            ): CjmpTypeCompatibility =
                error("rejected eligibility must not read return-type facts")
        }
        val prepared = AbstractCjmpMatcher.prepareCandidate(specific.symbol, common.symbol, eligibilityContext)
        assertEquals(CjmpMatchResult.Mismatched(CjmpMismatchKind.CALLABLE_KIND), prepared.eligibility)
        assertNull(prepared.signatureCompatibility)
        assertTrue(prepared.typeParameterMapping.isEmpty())
        assertFalse(prepared.requiresImplementation)
    }

    @Test
    fun commonSnapshotKeepsBothEnumAndMixedFunctionOccupationKinds() {
        val common = variant(owner(), payload = listOf(ConePrimitiveType.INT64))
        val winner = variant(owner(), payload = listOf(ConePrimitiveType.INT64))
        val rejectedEnum = variant(owner(), payload = listOf(ConePrimitiveType.INT64))
        val rejectedFunction = function(owner(), listOf(ConePrimitiveType.INT64))
        val family = familySnapshot(
            commonDeclarations = listOf(common),
            plans = listOf(
                plan(winner, listOf(common)),
                plan(rejectedEnum, listOf(common)),
                plan(rejectedFunction, listOf(common)),
            ),
        )

        assertEquals(listOf(rejectedEnum, rejectedFunction), defaultCfirCjmpCommonSideFacts.occupiedSpecificDeclarations(common, family))
        assertEquals(listOf(CfirCjmpCandidateDiagnostic.Occupied(common, winner)), family.candidateDiagnosticsFor(rejectedEnum))
        assertEquals(listOf(CfirCjmpCandidateDiagnostic.Occupied(common, winner)), family.candidateDiagnosticsFor(rejectedFunction))
        assertSame(common, family.commonFor(winner))
        assertNull(family.commonFor(rejectedEnum))
        assertNull(family.commonFor(rejectedFunction))

        assertSame(common, family.commonFor(winner))
        assertEquals(listOf(winner), family.specificBindingsFor(common))
        assertEquals(listOf(CfirCjmpCandidateDiagnostic.Occupied(common, winner)), family.candidateDiagnosticsFor(rejectedEnum))
        assertEquals(
            listOf(rejectedEnum, rejectedFunction),
            defaultCfirCjmpCommonSideFacts.occupiedSpecificDeclarations(common, family),
        )
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (family.specificDeclarations as MutableList<CfirDeclaration>).clear()
        }
    }

    @Test
    fun laterSuccessfulBindingDoesNotEraseTheEarlierCommonOccupationFact() {
        val occupiedCommon = variant(owner(), payload = listOf(ConePrimitiveType.INT64))
        val winner = variant(owner(), payload = listOf(ConePrimitiveType.INT64))
        val laterSpecific = function(owner(), listOf(ConePrimitiveType.INT64))
        val alternateCommon = function(owner(), listOf(ConePrimitiveType.INT64))
        val family = familySnapshot(
            commonDeclarations = listOf(occupiedCommon, alternateCommon),
            plans = listOf(
                plan(winner, listOf(occupiedCommon)),
                plan(laterSpecific, listOf(occupiedCommon, alternateCommon)),
            ),
        )

        assertSame(alternateCommon, family.commonFor(laterSpecific))
        assertEquals(listOf(CfirCjmpCandidateDiagnostic.Occupied(occupiedCommon, winner)), family.candidateDiagnosticsFor(laterSpecific))
        assertEquals(listOf(winner), family.specificBindingsFor(occupiedCommon))
        assertEquals(listOf(laterSpecific), family.specificBindingsFor(alternateCommon))
        assertEquals(listOf(laterSpecific), defaultCfirCjmpCommonSideFacts.occupiedSpecificDeclarations(occupiedCommon, family))
        assertTrue(defaultCfirCjmpCommonSideFacts.occupiedSpecificDeclarations(alternateCommon, family).isEmpty())
        assertTrue(family.mismatchKindsFor(laterSpecific).isEmpty(), "the final match may clear structural mismatch without clearing an emitted occupation")
    }

    @Test
    fun alreadyBoundSpecificDoesNotInventOccupationOfAnUnusedCommon() {
        val specific = variant(owner(), payload = listOf(ConePrimitiveType.INT64))
        val acceptedCommon = variant(owner(), payload = listOf(ConePrimitiveType.INT64))
        val unusedCommon = variant(owner(), payload = listOf(ConePrimitiveType.INT64))
        val family = familySnapshot(
            commonDeclarations = listOf(acceptedCommon, unusedCommon),
            plans = listOf(plan(specific, listOf(acceptedCommon, unusedCommon))),
        )

        assertSame(acceptedCommon, family.commonFor(specific))
        assertTrue(family.specificBindingsFor(unusedCommon).isEmpty())
        assertTrue(family.candidateDiagnosticsFor(specific).isEmpty())
        assertTrue(defaultCfirCjmpCommonSideFacts.occupiedSpecificDeclarations(unusedCommon, family).isEmpty())
    }

    private fun signature(
        specific: CfirCallableDeclaration,
        common: CfirCallableDeclaration,
        mapping: Map<CfirTypeParameter, CfirTypeParameter> = emptyMap(),
    ): CjmpMatchResult = AbstractCjmpMatcher.matchSignature(
        specific.symbol, common.symbol, context, context.parentTypeParameterMapping(mapping),
    )

    private fun plan(
        specific: CfirCallableDeclaration,
        candidates: List<CfirCallableDeclaration>,
        mapping: Map<CfirTypeParameter, CfirTypeParameter> = emptyMap(),
    ): CjmpMatchPlan.Declaration<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Nothing> =
        CfirCjmpResolver.buildCallableMatchPlan<Nothing>(
            specific = specific,
            specificDefaults = cfirCjmpDefaultStates(specific),
            candidates = candidates.map { common ->
                CfirCjmpCallableCandidate(
                    common = common,
                    commonDefaults = cfirCjmpDefaultStates(common),
                    beforeSignatureDiagnostics = emptyList(),
                    afterParameterDiagnostics = emptyList(),
                    afterParameters = CjmpMatchResult.Matched,
                    beforeBindingDiagnostics = emptyList(),
                )
            },
            session = session,
            traversal = CjmpCandidateTraversal.FIRST_SUCCESS,
            parentTypeParameterMapping = mapping,
        )

    private fun familySnapshot(
        commonDeclarations: List<CfirDeclaration>,
        plans: List<CjmpMatchPlan.Declaration<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Nothing>>,
    ): CfirCjmpFamilySnapshot {
        val facts = plans.map { plan ->
            CfirCjmpDeclarationMatchFacts(plan.specific, listOf(plan.specific), listOf(plan), null, emptyList())
        }
        val orderedDeclarations = commonDeclarations.map { it.symbol } + plans.map { it.specific }
        return CfirCjmpPreparedGroup(
            packageFqName = FqName("enum_plan"),
            roots = facts,
            topLevelOrder = orderedDeclarations,
            originalChildren = emptyMap(),
            commonDeclarations = commonDeclarations.map { it.symbol },
            containers = emptyList(),
            extensions = emptyList(),
            commonExtendOrder = emptyList(),
            originalDoNotExport = emptySet(),
            classifierDomain = CfirCjmpClassifierDomain.capture(session, orderedDeclarations.map { it.cfir }, sourceFiles = emptyList()),
        ).evaluate()
    }

    /** 混合候选是真实普通成员函数，保持 owner、参数归属和返回 enum 类型。 */
    private fun function(
        owner: CfirEnum,
        payload: List<ConeCangJieType>,
        name: String = "Value",
        named: Boolean = false,
    ): CfirNamedFunction {
        val functionSymbol = CfirNamedFunctionSymbol(CallableId(owner.symbol.classId, Name.identifier(name)))
        return buildNamedFunction {
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = CfirDeclarationStatusImpl()
            symbol = functionSymbol
            this.name = Name.identifier(name)
            returnTypeRef = enumType(owner).toCfirResolvedTypeRef(null)
            valueParameters += payload.mapIndexed { index, type ->
                val parameter = CallResolutionTestFixtures.buildValueParameter(session, "p$index", type, containingDeclarationSymbol = functionSymbol)
                buildValueParameterCopy(parameter) {
                    symbol = CfirValueParameterSymbol(parameter.symbol.callableId)
                    isNamed = named
                }
            }
            isMut = false
        }.also { it.containingClassForStaticMemberAttr = owner.symbol.toLookupTag() }
    }

    private fun genericFunction(
        name: String,
        parameterType: (CfirTypeParameter) -> ConeCangJieType,
    ): CfirNamedFunction {
        val functionSymbol = CfirNamedFunctionSymbol(CallableId(FqName("enum_plan"), Name.identifier(name)))
        val typeParameter = buildTypeParameter {
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            containingDeclarationSymbol = functionSymbol
            symbol = CfirTypeParameterSymbol()
            this.name = Name.identifier("T")
        }
        val valueParameter = CallResolutionTestFixtures.buildValueParameter(
            session = session,
            name = "p",
            type = parameterType(typeParameter),
            containingDeclarationSymbol = functionSymbol,
        )
        return buildNamedFunction {
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = CfirDeclarationStatusImpl()
            symbol = functionSymbol
            this.name = Name.identifier(name)
            returnTypeRef = ConePrimitiveType.UNIT.toCfirResolvedTypeRef(null)
            typeParameters += typeParameter
            valueParameters += valueParameter
            isMut = false
        }
    }

    /** 使用真实声明 builder 为外围类型参数建立独立符号身份。 */
    private fun owner(name: String = "E", typeParameterNames: List<String> = emptyList()): CfirEnum {
        val ownerSymbol = CfirEnumSymbol(ClassId(FqName("enum_plan"), Name.identifier(name)))
        val parameters = typeParameterNames.map { parameterName ->
            buildTypeParameter {
                moduleData = session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes.EMPTY
                containingDeclarationSymbol = ownerSymbol
                symbol = CfirTypeParameterSymbol()
                this.name = Name.identifier(parameterName)
            }
        }
        return buildEnum {
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            scopeProvider = CfirCangJieScopeProvider()
            status = CfirDeclarationStatusImpl()
            symbol = ownerSymbol
            this.name = Name.identifier(name)
            typeParameters += parameters
            isRefEnum = false
            isNonExhaustive = false
        }
    }

    private fun classDeclaration(name: String): CfirClass {
        val classId = ClassId(FqName("enum_plan"), Name.identifier(name))
        val symbol = CfirClassSymbol(classId)
        return buildClass {
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            scopeProvider = CfirCangJieScopeProvider()
            status = CfirDeclarationStatusImpl()
            this.symbol = symbol
            this.name = Name.identifier(name)
        }
    }

    private fun typeAlias(name: String): CfirTypeAlias {
        val classId = ClassId(FqName("enum_plan"), Name.identifier(name))
        val symbol = CfirTypeAliasSymbol(classId)
        return buildTypeAlias {
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            scopeProvider = CfirCangJieScopeProvider()
            status = CfirDeclarationStatusImpl()
            this.symbol = symbol
            this.name = Name.identifier(name)
            expandedTypeRef = ConePrimitiveType.INT64.toCfirResolvedTypeRef(null)
        }
    }

    private fun enumType(
        owner: CfirEnum,
        arguments: List<ConeCangJieType> = owner.typeParameters.map(ExtendTestFixtures::typeParameterType),
    ): ConeEnumType = ConeEnumType(owner.symbol.toLookupTag(), arguments)

    private fun variant(owner: CfirEnum, name: String = "Value", payload: List<ConeCangJieType> = emptyList()): CfirEnumConstructor {
        val variantSymbol = CfirEnumConstructorSymbol(CallableId(owner.symbol.classId, Name.identifier(name)))
        return buildEnumConstructor {
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = CfirDeclarationStatusImpl()
            symbol = variantSymbol
            this.name = Name.identifier(name)
            returnTypeRef = enumType(owner).toCfirResolvedTypeRef(null)
            valueParameters += payload.mapIndexed { index, type ->
                CallResolutionTestFixtures.buildValueParameter(session, "p$index", type, containingDeclarationSymbol = variantSymbol)
            }
        }.also { it.containingClassForStaticMemberAttr = owner.symbol.toLookupTag() }
    }
}
