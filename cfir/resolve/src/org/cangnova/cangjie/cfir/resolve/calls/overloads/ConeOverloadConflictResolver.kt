package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.inference.InferenceComponents
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.cangnova.cangjie.resolve.calls.results.*
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.TypeParameterMarker
import org.cangnova.cangjie.type.model.TypeSubstitutorMarker
import org.cangnova.cangjie.type.model.TypeSystemInferenceExtensionContext

typealias CandidateSignature = FlatSignature<Candidate>

class ConeOverloadConflictResolver(
    private val specificityComparator: TypeSpecificityComparator,
    private val inferenceComponents: InferenceComponents,
    @Suppress("unused") private val transformerComponents: BodyResolveComponents,
) : ConeCallConflictResolver() {

    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
    ): Set<Candidate> = chooseMaximallySpecificCandidates(
        candidates,
        // The local CFIR model does not yet expose a dedicated callable-reference call-site node.
        // Kotlin FIR disables generic discrimination for callable references, so we derive the same
        // distinction from the candidate payload that is only initialized for callable references.
        discriminateGenerics = candidates.first().resultingTypeForCallableReference == null,
    )

    /**
     * Partial mirror of Kotlin FIR's `ConeOverloadConflictResolver.chooseMaximallySpecificCandidates`.
     *
     * The framework shape is intentionally kept aligned with upstream FIR. The only dropped pieces are
     * branches that the current Cangjie front-end does not model yet:
     * - context receivers
     * - property-for-invoke common receiver candidates
     * - callable-reference postponed atoms
     * - low-priority SAM diagnostics stage metadata
     */
    private fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
    ): Set<Candidate> {
        if (candidates.size == 1) return candidates

        val fixedCandidates = chooseCandidatesWithMostSpecificInvokeReceiver(candidates)
        val candidatesWithoutOverrides = filterOverrides(fixedCandidates)

        return chooseMaximallySpecificCandidates(
            candidatesWithoutOverrides,
            DiscriminationFlags(
                lowPrioritySAMs = true,
                adaptationsInPostponedAtoms = true,
                generics = discriminateGenerics,
                SAMs = true,
                suspendConversions = true,
                byUnwrappedSmartCastOrigin = true,
            )
        )
    }

    private fun filterOverrides(candidateSet: Set<Candidate>): Set<Candidate> {
        if (candidateSet.size <= 1) return candidateSet

        val result = linkedSetOf<Candidate>()

        outerLoop@ for (candidate in candidateSet) {
            val iterator = result.iterator()
            while (iterator.hasNext()) {
                val other = iterator.next()
                if (candidate.overrides(other)) {
                    iterator.remove()
                } else if (other.overrides(candidate)) {
                    continue@outerLoop
                }
            }

            result += candidate
        }

        require(result.isNotEmpty()) { "All candidates filtered out from $candidateSet" }
        return result
    }

    private fun Candidate.overrides(other: Candidate): Boolean {
        val candidateSymbol = symbol as? CfirCallableSymbol<*> ?: return false
        val otherSymbol = other.symbol as? CfirCallableSymbol<*> ?: return false

        if (candidateSymbol == otherSymbol) return true

        val scope = originScope as? CfirTypeScope ?: return false

        return when (candidateSymbol) {
            // 仓颉的 override 决议只涉及 named function：
            // constructor / enum constructor / init / property accessor 不参与普通重写判定，
            // 按 CfirNamedFunctionSymbol 窄化即可满足 CfirTypeScope API。
            is CfirNamedFunctionSymbol -> overrides(
                MemberWithBaseScope(candidateSymbol, scope),
                otherSymbol,
                ProcessAllOverridden<CfirNamedFunctionSymbol> { baseScope, symbol, processor ->
                    baseScope.processDirectOverriddenFunctionsWithBaseScope(symbol, processor)
                },
            )

            is CfirPropertySymbol -> overrides(
                MemberWithBaseScope(candidateSymbol, scope),
                otherSymbol,
                ProcessAllOverridden<CfirPropertySymbol> { baseScope, symbol, processor ->
                    baseScope.processDirectOverriddenPropertiesWithBaseScope(symbol, processor)
                },
            )

            else -> false
        }
    }

    private fun chooseCandidatesWithMostSpecificInvokeReceiver(candidates: Set<Candidate>): Set<Candidate> {
        // Kotlin FIR has a dedicated `candidateForCommonInvokeReceiver` slot for property+invoke groups.
        // The current CFIR call model does not carry that structure yet, so this hook is intentionally
        // kept as an identity step to preserve upstream control flow.
        return candidates
    }

    private data class DiscriminationFlags(
        val lowPrioritySAMs: Boolean,
        val adaptationsInPostponedAtoms: Boolean,
        val generics: Boolean,
        val SAMs: Boolean,
        val suspendConversions: Boolean,
        val byUnwrappedSmartCastOrigin: Boolean,
    )

    private fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminationFlags: DiscriminationFlags,
    ): Set<Candidate> {
        if (discriminationFlags.lowPrioritySAMs) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.shouldHaveLowPriorityDueToSAM() },
                { discriminationFlags.copy(lowPrioritySAMs = false) },
            )?.let { return it }
        }

        if (discriminationFlags.adaptationsInPostponedAtoms) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.hasPostponedAtomWithAdaptation() },
                { discriminationFlags.copy(adaptationsInPostponedAtoms = false) },
            )?.let { return it }
        }

        findMaximallySpecificCall(candidates, discriminateGenerics = false)?.let { return setOf(it) }

        if (discriminationFlags.generics) {
            findMaximallySpecificCall(candidates, discriminateGenerics = true)?.let { return setOf(it) }
        }

        if (discriminationFlags.SAMs) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.usesSamConversionOrSamConstructor },
                { discriminationFlags.copy(SAMs = false) },
            )?.let { return it }
        }

        if (discriminationFlags.suspendConversions) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.usesFunctionKindConversion },
                { discriminationFlags.copy(suspendConversions = false) },
            )?.let { return it }
        }

        if (discriminationFlags.byUnwrappedSmartCastOrigin) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.isFromOriginalTypeInPresenceOfSmartCast },
                { discriminationFlags.copy(byUnwrappedSmartCastOrigin = false) },
            )?.let { return it }
        }

        val filteredSamCandidates = candidates.filterTo(linkedSetOf()) { it.usesSamConversionOrSamConstructor }
        if (filteredSamCandidates.isNotEmpty()) {
            findMaximallySpecificCall(
                candidates,
                discriminateGenerics = false,
                useOriginalSamTypes = true,
            )?.let { return setOf(it) }
        }

        chooseByCangjieSpecificity(candidates)?.let { return it }

        return candidates
    }

    private inline fun filterCandidatesByDiscriminationFlag(
        candidates: Set<Candidate>,
        filter: (Candidate) -> Boolean,
        newFlags: () -> DiscriminationFlags,
    ): Set<Candidate>? {
        val filtered = candidates.filterTo(linkedSetOf()) { filter(it) }
        return when (filtered.size) {
            1 -> filtered
            0, candidates.size -> null
            else -> chooseMaximallySpecificCandidates(filtered, newFlags())
        }
    }

    private fun chooseByCangjieSpecificity(candidates: Set<Candidate>): Set<Candidate>? {
        preferCandidates(candidates) { !it.usedExtendParticipation }?.let { return it }
        preferCandidates(candidates) { !it.usedQuestFallback }?.let { return it }
        preferCandidates(candidates) { !it.usedIdealNumericCompatibility }?.let { return it }
        return null
    }

    private inline fun preferCandidates(
        candidates: Set<Candidate>,
        predicate: (Candidate) -> Boolean,
    ): Set<Candidate>? {
        val preferred = candidates.filterTo(linkedSetOf()) { predicate(it) }
        return when (preferred.size) {
            0, candidates.size -> null
            else -> preferred
        }
    }

    private fun Candidate.shouldHaveLowPriorityDueToSAM(): Boolean {
        // Kotlin FIR threads this signal from dedicated resolution stages.
        // The current Cangjie pipeline keeps SAM conversion data but not the low-priority stage marker.
        return false
    }

    private fun Candidate.hasPostponedAtomWithAdaptation(): Boolean {
        // Callable-reference postponed atoms have not been introduced in the local CFIR atom hierarchy yet.
        return false
    }

    private fun findMaximallySpecificCall(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
        useOriginalSamTypes: Boolean = false,
    ): Candidate? {
        if (candidates.size <= 1) return candidates.singleOrNull()

        val candidateSignatures = candidates.map(::createFlatSignature)
        val bestCandidatesByParameterTypes = candidateSignatures.filter { signature ->
            candidateSignatures.all { other ->
                signature === other || isEquallyOrMoreSpecificCallWithArgumentMapping(
                    signature,
                    other,
                    discriminateGenerics,
                    useOriginalSamTypes,
                )
            }
        }

        return bestCandidatesByParameterTypes.exactMaxWith()?.origin
    }

    private fun isEquallyOrMoreSpecificCallWithArgumentMapping(
        call1: CandidateSignature,
        call2: CandidateSignature,
        discriminateGenerics: Boolean,
        useOriginalSamTypes: Boolean = false,
    ): Boolean {
        return compareCallsByUsedArguments(call1, call2, discriminateGenerics, useOriginalSamTypes)
    }

    private fun List<CandidateSignature>.exactMaxWith(): CandidateSignature? {
        var result: CandidateSignature? = null
        for (candidate in this) {
            if (result == null || checkExpectAndEquallyOrMoreSpecificShape(candidate, result)) {
                result = candidate
            }
        }

        if (result == null) return null
        if (any { it != result && checkExpectAndEquallyOrMoreSpecificShape(it, result) }) {
            return null
        }

        return result
    }

    private fun checkExpectAndEquallyOrMoreSpecificShape(
        call1: CandidateSignature,
        call2: CandidateSignature,
    ): Boolean {
        val hasVarargs1 = call1.hasVarargs
        val hasVarargs2 = call2.hasVarargs
        if (hasVarargs1 && !hasVarargs2) return false
        if (!hasVarargs1 && hasVarargs2) return true

        if (call1.numDefaults > call2.numDefaults) return false

        return true
    }

    private fun compareCallsByUsedArguments(
        call1: CandidateSignature,
        call2: CandidateSignature,
        discriminateGenerics: Boolean,
        useOriginalSamTypes: Boolean,
    ): Boolean {
        if (discriminateGenerics) {
            val isGeneric1 = call1.isGeneric
            val isGeneric2 = call2.isGeneric

            when {
                !isGeneric1 && isGeneric2 -> return true
                isGeneric1 -> return false
            }
        }

        return createEmptyConstraintSystem().isSignatureEquallyOrMoreSpecific(
            call1,
            call2,
            SpecificityComparisonWithNumerics,
            specificityComparator,
            useOriginalSamTypes,
        )
    }

    @Suppress("PrivatePropertyName")
    private val SpecificityComparisonWithNumerics = object : SpecificityComparisonCallbacks {
        override fun isNonSubtypeEquallyOrMoreSpecific(
            specific: CangJieTypeMarker,
            general: CangJieTypeMarker,
        ): Boolean {
            val specificType = specific as? ConeCangJieType ?: return false
            val generalType = general as? ConeCangJieType ?: return false
            val specificPrimitive = specificType.fullyExpandedType() as? ConePrimitiveType ?: return false
            val generalPrimitive = generalType.fullyExpandedType() as? ConePrimitiveType ?: return false

            return isPrimitiveEquallyOrMoreSpecific(specificPrimitive.kind, generalPrimitive.kind)
        }
    }

    private fun createFlatSignature(call: Candidate): FlatSignature<Candidate> {
        if (!call.symbol.isBound) {
            return FlatSignature(
                origin = call,
                typeParameters = emptyList(),
                valueParameterTypes = emptyList<TypeWithConversion>(),
                hasExtensionReceiver = false,
                contextReceiverCount = 0,
                hasVarargs = false,
                numDefaults = call.numDefaults,
                isExpect = false,
                isSyntheticMember = false,
            )
        }

        return when (val declaration = call.symbol.cfir) {
            is CfirFunction -> createFlatSignature(call, declaration)
            is CfirConstructor -> createFlatSignature(call, declaration)
            is CfirEnumConstructor -> createFlatSignature(call, declaration)
            is CfirProperty -> createFlatSignature(call, declaration)
            is CfirVariable -> createFlatSignature(call, declaration)
            is CfirClassLikeDeclaration -> createFlatSignature(call, declaration)
            else -> error("Unsupported declaration for overload conflict resolution: ${declaration::class.java.name}")
        }
    }

    private fun createFlatSignature(call: Candidate, declaration: CfirFunction): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = declaration.typeParameters.toTypeParameterMarkers(),
            valueParameterTypes = computeSignatureTypes(call, declaration),
            hasExtensionReceiver = false,
            contextReceiverCount = 0,
            hasVarargs = false,
            numDefaults = call.numDefaults,
            isExpect = false,
            isSyntheticMember = declaration.origin is CfirDeclarationOrigin.Synthetic,
        )
    }

    private fun createFlatSignature(call: Candidate, declaration: CfirConstructor): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = declaration.typeParameters.toTypeParameterMarkers(),
            valueParameterTypes = computeSignatureTypes(call, declaration),
            hasExtensionReceiver = false,
            contextReceiverCount = 0,
            hasVarargs = false,
            numDefaults = call.numDefaults,
            isExpect = false,
            isSyntheticMember = declaration.origin is CfirDeclarationOrigin.Synthetic,
        )
    }

    private fun createFlatSignature(call: Candidate, declaration: CfirEnumConstructor): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = declaration.typeParameters.toTypeParameterMarkers(),
            valueParameterTypes = computeSignatureTypes(call, declaration),
            hasExtensionReceiver = false,
            contextReceiverCount = 0,
            hasVarargs = false,
            numDefaults = call.numDefaults,
            isExpect = false,
            isSyntheticMember = declaration.origin is CfirDeclarationOrigin.Synthetic,
        )
    }

    private fun createFlatSignature(call: Candidate, declaration: CfirProperty): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = declaration.typeParameters.toTypeParameterMarkers(),
            valueParameterTypes = computeSignatureTypes(call, declaration),
            hasExtensionReceiver = false,
            contextReceiverCount = 0,
            hasVarargs = false,
            numDefaults = call.numDefaults,
            isExpect = false,
            isSyntheticMember = declaration.origin is CfirDeclarationOrigin.Synthetic,
        )
    }

    private fun createFlatSignature(call: Candidate, declaration: CfirVariable): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = when (declaration) {
                is CfirFieldVariable -> declaration.typeParameters.toTypeParameterMarkers()
                is CfirPatternBindingVariable -> declaration.typeParameters.toTypeParameterMarkers()
                is org.cangnova.cangjie.cfir.declarations.CfirPatternVariable -> declaration.typeParameters.toTypeParameterMarkers()
                else -> emptyList()
            },
            valueParameterTypes = computeSignatureTypes(call, declaration),
            hasExtensionReceiver = false,
            contextReceiverCount = 0,
            hasVarargs = false,
            numDefaults = call.numDefaults,
            isExpect = false,
            isSyntheticMember = declaration.origin is CfirDeclarationOrigin.Synthetic,
        )
    }

    private fun createFlatSignature(call: Candidate, declaration: CfirClassLikeDeclaration): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = declaration.typeParameters().toTypeParameterMarkers(),
            valueParameterTypes = emptyList<TypeWithConversion>(),
            hasExtensionReceiver = false,
            contextReceiverCount = 0,
            hasVarargs = false,
            numDefaults = 0,
            isExpect = false,
            isSyntheticMember = declaration.origin is CfirDeclarationOrigin.Synthetic,
        )
    }

    private fun computeSignatureTypes(
        call: Candidate,
        called: CfirCallableDeclaration,
    ): List<TypeWithConversion> {
        return buildList {
            val session = inferenceComponents.session
            val typeForCallableReference = call.resultingTypeForCallableReference
            if (typeForCallableReference != null) {
                typeForCallableReference.typeArguments
                    .dropLast(1)
                    .mapTo(this) { argument ->
                        TypeWithConversion(argument.type.prepareType(session, call))
                    }
            } else if (call.argumentMappingInitialized) {
                call.argumentMapping.mapTo(this) { (argument, parameter) ->
                    parameter.toTypeWithConversion(argument, session, call)
                }
            } else {
                declaredParametersFor(called).mapTo(this) { parameter ->
                    val parameterType = parameter.returnTypeRef.coneTypeOrNull()?.prepareType(session, call)
                    TypeWithConversion(parameterType)
                }
            }
        }
    }

    private fun declaredParametersFor(called: CfirCallableDeclaration): List<CfirValueParameter> {
        return when (called) {
            is CfirFunction -> called.valueParameters
            is CfirConstructor -> called.valueParameters
            is CfirEnumConstructor -> called.valueParameters
            else -> emptyList()
        }
    }

    private fun CfirValueParameter.toTypeWithConversion(
        argument: org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom,
        session: org.cangnova.cangjie.cfir.session.CfirSession,
        call: Candidate,
    ): TypeWithConversion {
        val argumentType = returnTypeRef.coneTypeOrNull()?.prepareType(session, call)
        val functionTypeForSam = toFunctionTypeForSamOrNull(argument, call)?.prepareType(session, call)
        return if (functionTypeForSam == null) {
            TypeWithConversion(argumentType)
        } else {
            TypeWithConversion(functionTypeForSam, argumentType)
        }
    }

    private fun CfirValueParameter.toFunctionTypeForSamOrNull(
        argument: org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom,
        call: Candidate,
    ): ConeCangJieType? {
        val functionTypesOfSamConversions = call.samConversionInfosOfArguments ?: return null
        return functionTypesOfSamConversions[argument.expression]?.functionalType
    }

    private fun ConeCangJieType.prepareType(
        session: org.cangnova.cangjie.cfir.session.CfirSession,
        candidate: Candidate,
    ): ConeCangJieType {
        val expanded = fullyExpandedType()
        if (!candidate.system.usesOuterCs) return expanded

        val substitutor = candidate.system.buildNotFixedVariablesToStubTypesSubstitutor()
        return with(session.typeContext) {
            substitutor.safeSubstitute(expanded) as ConeCangJieType
        }
    }

    private fun ConeCangJieType.fullyExpandedType(): ConeCangJieType {
        return when (this) {
            is ConeTypeAliasType -> expandedType?.fullyExpandedType() ?: this
            else -> this
        }
    }

    private fun List<CfirTypeParameter>.toTypeParameterMarkers(): List<TypeParameterMarker> {
        return mapNotNull { (it.symbol as? CfirTypeParameterSymbol)?.toLookupTag() as? TypeParameterMarker }
    }

    private fun CfirClassLikeDeclaration.typeParameters(): List<CfirTypeParameter> {
        return when (this) {
            is CfirClass -> typeParameters
            is org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration -> emptyList()
            is CfirInterface -> typeParameters
            is CfirStruct -> typeParameters
            is CfirEnum -> typeParameters
            is CfirTypeAlias -> typeParameters
            else -> emptyList()
        }
    }

    private fun createEmptyConstraintSystem(): SimpleConstraintSystem {
        return ConeSimpleConstraintSystemImpl(inferenceComponents.createConstraintSystem(), inferenceComponents)
    }

    private fun isPrimitiveEquallyOrMoreSpecific(
        specific: PrimitiveTypeKind,
        general: PrimitiveTypeKind,
    ): Boolean {
        if (specific == general) return true

        return when {
            specific in SIGNED_NUMERIC_SPECIFICITY && general in SIGNED_NUMERIC_SPECIFICITY ->
                SIGNED_NUMERIC_SPECIFICITY.getValue(specific) >= SIGNED_NUMERIC_SPECIFICITY.getValue(general)

            specific in UNSIGNED_NUMERIC_SPECIFICITY && general in UNSIGNED_NUMERIC_SPECIFICITY ->
                UNSIGNED_NUMERIC_SPECIFICITY.getValue(specific) >= UNSIGNED_NUMERIC_SPECIFICITY.getValue(general)

            specific in FLOAT_NUMERIC_SPECIFICITY && general in FLOAT_NUMERIC_SPECIFICITY ->
                FLOAT_NUMERIC_SPECIFICITY.getValue(specific) >= FLOAT_NUMERIC_SPECIFICITY.getValue(general)

            else -> false
        }
    }

    private data class MemberWithBaseScope<S : CfirCallableSymbol<*>>(
        val symbol: S,
        val scope: CfirTypeScope,
    )

    private fun interface ProcessAllOverridden<S : CfirCallableSymbol<*>> {
        fun process(
            scope: CfirTypeScope,
            symbol: S,
            processor: (S, CfirTypeScope) -> ProcessorAction,
        ): ProcessorAction
    }

    private fun <S : CfirCallableSymbol<*>> overrides(
        member: MemberWithBaseScope<S>,
        target: CfirCallableSymbol<*>,
        overriddenProducer: ProcessAllOverridden<S>,
    ): Boolean {
        val visited = linkedSetOf<S>()

        fun visit(current: MemberWithBaseScope<S>): Boolean {
            if (!visited.add(current.symbol)) return false

            var found = false
            overriddenProducer.process(current.scope, current.symbol) { overridden, baseScope ->
                when {
                    overridden == target -> {
                        found = true
                        ProcessorAction.STOP
                    }

                    visit(MemberWithBaseScope(overridden, baseScope)) -> {
                        found = true
                        ProcessorAction.STOP
                    }

                    else -> ProcessorAction.NEXT
                }
            }
            return found
        }

        return visit(member)
    }

    companion object {
        private val SIGNED_NUMERIC_SPECIFICITY: Map<PrimitiveTypeKind, Int> = mapOf(
            PrimitiveTypeKind.INT8 to 0,
            PrimitiveTypeKind.INT16 to 1,
            PrimitiveTypeKind.INT32 to 2,
            PrimitiveTypeKind.INT64 to 3,
            PrimitiveTypeKind.INT_NATIVE to 4,
        )

        private val UNSIGNED_NUMERIC_SPECIFICITY: Map<PrimitiveTypeKind, Int> = mapOf(
            PrimitiveTypeKind.UINT8 to 0,
            PrimitiveTypeKind.UINT16 to 1,
            PrimitiveTypeKind.UINT32 to 2,
            PrimitiveTypeKind.UINT64 to 3,
            PrimitiveTypeKind.UINT_NATIVE to 4,
        )

        private val FLOAT_NUMERIC_SPECIFICITY: Map<PrimitiveTypeKind, Int> = mapOf(
            PrimitiveTypeKind.FLOAT16 to 0,
            PrimitiveTypeKind.FLOAT32 to 1,
            PrimitiveTypeKind.FLOAT64 to 2,
        )
    }
}

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.coneTypeOrNull(): ConeCangJieType? {
    return (this as? org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef)?.coneType
}

private class ConeSimpleConstraintSystemImpl(
    private val system: ConstraintSystemImpl,
    private val inferenceComponents: InferenceComponents,
) : SimpleConstraintSystem {
    override fun registerTypeVariables(typeParameters: Collection<TypeParameterMarker>): TypeSubstitutorMarker {
        val builder = system.getBuilder()
        val substitutionMap = linkedMapOf<org.cangnova.cangjie.type.model.TypeConstructorMarker, ConeCangJieType>()

        for (typeParameter in typeParameters) {
            require(typeParameter is ConeTypeParameterLookupTag)
            val variable = ConeTypeParameterBasedTypeVariable(typeParameter.typeParameterSymbol)
            builder.registerVariable(variable)
            substitutionMap[typeParameter] = variable.defaultType
        }

        val substitutor = createTypeSubstitutorByTypeConstructor(
            map = substitutionMap,
            context = inferenceComponents.session.typeContext,
            approximateIntegerLiterals = false,
        )

        for (typeParameter in typeParameters) {
            require(typeParameter is ConeTypeParameterLookupTag)
            val variableType = substitutionMap[typeParameter]
                ?: error("Missing substituted variable for $typeParameter")
            for (upperBound in typeParameter.typeParameterSymbol.resolvedBounds) {
                addSubtypeConstraint(
                    variableType,
                    with(inferenceComponents.session.typeContext) {
                        substitutor.safeSubstitute(upperBound.coneType) as ConeCangJieType
                    },
                )
            }
        }

        return substitutor
    }

    override fun addSubtypeConstraint(subType: CangJieTypeMarker, superType: CangJieTypeMarker) {
        system.addSubtypeConstraint(subType, superType, SimpleConstraintSystemConstraintPosition)
    }

    override fun hasContradiction(): Boolean = system.hasContradiction



    override val context: TypeSystemInferenceExtensionContext
        get() = system

    override val constraintSystemMarker: org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemMarker
        get() = system
}
