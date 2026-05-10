package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.resolve.CfirSamResolver
import org.cangnova.cangjie.cfir.resolve.calls.CallableReferenceAdaptation
import org.cangnova.cangjie.cfir.resolve.calls.ConePostponedResolvedAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.stages.TypeArgumentMapping
import org.cangnova.cangjie.cfir.resolve.inference.InferenceComponents
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.semantics.isSuccess
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability

class Candidate(
    symbol: CfirBasedSymbol<*>,
    // Here we may have an ExpressionReceiverValue
    // - in case a use-site receiver is explicit
    // - in some cases with static entities, no matter is a use-site receiver explicit or not
    // OR we may have here a kind of ImplicitReceiverValue (non-statics only)
    override var dispatchReceiver: ConeResolutionAtom?,
    val givenExtensionReceiver: ConeResolutionAtom?,
    override val explicitReceiverKind: ExplicitReceiverKind,
    private val constraintSystemFactory: InferenceComponents.ConstraintSystemFactory,
    private val baseSystem: ConstraintStorage,
    override val callInfo: CallInfo,
    val originScope: CfirScope?,
    val isFromCompanionObjectTypeScope: Boolean = false,
    // It's only true if we're in the member scope of smart cast receiver and this particular candidate came from original type
    val isFromOriginalTypeInPresenceOfSmartCast: Boolean = false,
    bodyResolveContext: BodyResolveContext,
) : AbstractCallCandidate<ConeResolutionAtom>() {

    // ---------------------------------------- Symbol ----------------------------------------

    override var symbol: CfirBasedSymbol<*> = symbol
        private set

    @UpdatingCandidateInvariants
    fun updateSymbol(symbol: CfirBasedSymbol<*>) {
        this.symbol = symbol
    }

    // ---------------------------------------- Constraint system ----------------------------------------

    override val usedOuterCs: Boolean get() = system.usesOuterCs

    private var systemInitialized: Boolean = false
    override val system: ConstraintSystemImpl by lazy(LazyThreadSafetyMode.NONE) {
        val system = constraintSystemFactory.createConstraintSystem()

        val baseCSFromInferenceSession = if (baseSystem.usesOuterCs) {
            null
        } else {
            bodyResolveContext.inferenceSession.baseConstraintStorageForCandidate(this, bodyResolveContext)
        }
        if (baseCSFromInferenceSession != null) {
            system.setBaseSystem(baseCSFromInferenceSession)
            system.addOtherSystem(baseSystem)
        } else {
            system.setBaseSystem(baseSystem)
        }

        systemInitialized = true
        system
    }

    override val errors: List<ConstraintSystemError>
        get() = system.errors

    /**
     * Substitutor from declared type parameters to type variables created for that candidate
     */
    lateinit var substitutor: ConeSubstitutor
        private set
    lateinit var freshVariables: List<ConeTypeVariable>
        private set

    fun initializeSubstitutorAndVariables(substitutor: ConeSubstitutor, freshVariables: List<ConeTypeVariable>) {
        this.substitutor = substitutor
        this.freshVariables = freshVariables
    }

    @UpdatingCandidateInvariants
    fun updateSubstitutor(substitutor: ConeSubstitutor) {
        this.substitutor = substitutor
    }

    // ---------------------------------------- Conversions ----------------------------------------

    var resultingTypeForCallableReference: ConeCangJieType? = null
        private set

    internal var callableReferenceAdaptation: CallableReferenceAdaptation? = null
        private set

    internal fun initializeCallableReferenceAdaptation(
        callableReferenceAdaptation: CallableReferenceAdaptation?,
        resultingTypeForCallableReference: ConeCangJieType
    ) {
        require(this.callableReferenceAdaptation == null) { "callableReferenceAdaptation already initialized" }
        this.callableReferenceAdaptation = callableReferenceAdaptation
        this.resultingTypeForCallableReference = resultingTypeForCallableReference
        if (callableReferenceAdaptation != null) {
            numDefaults = callableReferenceAdaptation.defaults
        }
    }

    /**
     * Expressions in this set are arguments of the call that have function kind conversion applied (e.g., suspend conversion).
     */
    var argumentsWithFunctionKindConversion: HashSet<CfirExpression>? = null
        private set

    fun addFunctionKindConversionOfArgument(element: CfirExpression) {
        val set = argumentsWithFunctionKindConversion ?: HashSet<CfirExpression>().also { argumentsWithFunctionKindConversion = it }
        set += element
    }

    var samConversionInfosOfArguments: HashMap<CfirExpression, CfirSamResolver.SamConversionInfo>? = null
        private set

    fun setSamConversionOfArgument(expression: CfirExpression, conversionInfo: CfirSamResolver.SamConversionInfo) {
        val map = samConversionInfosOfArguments
            ?: hashMapOf<CfirExpression, CfirSamResolver.SamConversionInfo>().also { samConversionInfosOfArguments = it }
        map[expression] = conversionInfo
    }

    // Computed getters

    val usesSamConversion: Boolean
        get() = samConversionInfosOfArguments != null

    val usesSamConversionOrSamConstructor: Boolean
        get() = usesSamConversion || symbol.origin == CfirDeclarationOrigin.SamConstructor

    val usesFunctionKindConversion: Boolean
        get() = argumentsWithFunctionKindConversion != null || callableReferenceAdaptation?.hasFunctionKindConversion() == true

    // ---------------------------------------- Argument mapping ----------------------------------------

    private var _arguments: List<ConeResolutionAtom>? = null
    val arguments: List<ConeResolutionAtom>
        get() = _arguments ?: error("Argument list is not initialized yet")

    private var _argumentMapping: LinkedHashMap<ConeResolutionAtom, CfirValueParameter>? = null
    override val argumentMappingInitialized: Boolean
        get() = _argumentMapping != null
    override val argumentMapping: LinkedHashMap<ConeResolutionAtom, CfirValueParameter>
        get() = _argumentMapping ?: error("Argument mapping is not initialized yet")

    fun initializeArgumentMapping(
        arguments: List<ConeResolutionAtom>,
        argumentMapping: LinkedHashMap<ConeResolutionAtom, CfirValueParameter>,
    ) {
        require(_argumentMapping == null) { "Argument mapping already initialized" }
        _argumentMapping = argumentMapping
        _arguments = arguments
    }

    @UpdatingCandidateInvariants
    fun updateArgumentMapping(argumentMapping: LinkedHashMap<ConeResolutionAtom, CfirValueParameter>) {
        _argumentMapping = argumentMapping
    }

    /**
     * The arguments of a contextual implicit `invoke` candidate contain stub expressions for the implicitly passed
     * context arguments between the [MapArguments] and [CheckContextArguments] stages.
     *
     * These expressions are always the first in the [arguments] list.
     *
     * This function replaces these stub arguments with the given [newArgumentPrefix] and updates the [argumentMapping] accordingly.
     */
    @UpdatingCandidateInvariants
    fun replaceArgumentPrefix(newArgumentPrefix: List<ConeResolutionAtom>) {
        val remainingArguments = arguments.subList(newArgumentPrefix.size, arguments.size)

        val newArgumentMapping = LinkedHashMap<ConeResolutionAtom, CfirValueParameter>()
        for ((oldArgument, newArgument) in arguments.zip(newArgumentPrefix)) {
            newArgumentMapping[newArgument] = argumentMapping.getValue(oldArgument)
        }

        for (argument in remainingArguments) {
            argumentMapping[argument]?.let { newArgumentMapping[argument] = it }
        }

        val newArguments = newArgumentPrefix + remainingArguments

        _arguments = newArguments
        _argumentMapping = newArgumentMapping
    }

    var numDefaults: Int = 0
    var usedQuestFallback: Boolean = false
    var usedIdealNumericCompatibility: Boolean = false
    var usedExtendParticipation: Boolean = false

    // ---------------------------------------- Type argument mapping ----------------------------------------

    lateinit var typeArgumentMapping: TypeArgumentMapping

    // ---------------------------------------- Postponed atoms ----------------------------------------

    val postponedAtoms: MutableList<ConePostponedResolvedAtom> = mutableListOf()

    fun addPostponedAtom(atom: ConePostponedResolvedAtom) {
        postponedAtoms += atom
    }

    // ------------------------ Context-sensitively resolved arguments ------------------------------------

    private var _updatedArguments: MutableMap<CfirElement, CfirExpression>? =
        null

    private fun setUpdatedArgument(old: CfirExpression, new: CfirExpression) {
        if (_updatedArguments == null) {
            _updatedArguments = mutableMapOf()
        }

        val existingValue = _updatedArguments!!.put(old, new)
        check(existingValue == null) {
            "We shouldn't put the value for $old twice"
        }
    }

    fun setUpdatedArgumentFromContextSensitiveResolution(old: CfirNamedAccessExpression, new: CfirExpression) {
        setUpdatedArgument(old, new)
    }

    fun setUpdatedCollectionLiteral(old: CfirArrayLiteral, new: CfirExpression) {
        setUpdatedArgument(old, new)
    }

    val argumentReplacements: Map<CfirElement, CfirExpression>?
        get() = _updatedArguments

    // ---------------------------------------- PCLA-related parts ----------------------------------------

    val postponedPCLACalls: MutableList<ConeResolutionAtom> = mutableListOf()
    val lambdasAnalyzedWithPCLA: MutableList<CfirDeclaration> = mutableListOf()

    // Retained as an upstream-aligned callback seam for delegated-property/PCLA completion-result writing.
    // In the current local direct chain there is no CfirDelegatedPropertyInferenceSession or writer-mode
    // call site that populates this list, so these callbacks remain structurally available but unreachable.
    val onPCLACompletionResultsWritingCallbacks: MutableList<(ConeSubstitutor) -> Unit> = mutableListOf()

    // ---------------------------------------- Applicability ----------------------------------------

    var lowestApplicability: CandidateApplicability = CandidateApplicability.RESOLVED
        private set

    override val applicability: CandidateApplicability
        get() = lowestApplicability

    override val diagnostics: MutableList<ResolutionDiagnostic> = mutableListOf()

    fun addDiagnostic(diagnostic: ResolutionDiagnostic) {
        diagnostics += diagnostic
        if (diagnostic.applicability < lowestApplicability) {
            lowestApplicability = diagnostic.applicability
        }
    }

    /**
     * Note that [lowestApplicability]`.isSuccess == true` doesn't imply [isSuccessful].
     *
     * This is because [lowestApplicability] is equal to the lowest [ResolutionDiagnostic.applicability] of all [diagnostics],
     * but in presence of more than one diagnostic, the lowest one can be successful while a higher one isn't, e.g., the combination
     * of [CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY] and [CandidateApplicability.RESOLVED_WITH_ERROR].
     *
     * Also see [org.jetbrains.kotlin.fir.resolve.transformers.CfirCallCompletionResultsWriterTransformer.toResolvedReference]
     * as it contains conditions that rely on subtle differences between the implementation of this property and
     * [org.jetbrains.kotlin.resolve.calls.tower.isSuccess].
     */
    val isSuccessful: Boolean
        get() = diagnostics.all { it.isSuccess } && (!systemInitialized || !system.hasContradiction)

    // ---------------------------------------- Receivers ----------------------------------------

    override var chosenExtensionReceiver: ConeResolutionAtom? = givenExtensionReceiver

    override var contextArguments: List<ConeResolutionAtom>? = null

    /**
     * In case `f: context(C..) (V) -> ..`, `f(e..)`, context values are still being introduced as a prefix of
     * regular arguments for `invoke` function.
     */
    var expectedContextParameterCountForInvoke: Int? = null

    fun dispatchReceiverExpression(): CfirExpression? {
        return dispatchReceiver?.expression
    }

    fun chosenExtensionReceiverExpression(): CfirExpression? {
        return chosenExtensionReceiver?.expression
    }

    fun contextArguments(): List<CfirExpression> {
        return contextArguments?.map { it.expression } ?: emptyList()
    }

    private var sourcesWereUpdated = false

    // In case of implicit receivers we want to update corresponding sources to generate correct offset. This method must be called only
    // once when candidate was selected and confirmed to be correct one.
    fun updateSourcesOfReceivers() {
        require(!sourcesWereUpdated)
        sourcesWereUpdated = true
    }

    // This thing is mostly for a common fast-path optimization and should not affect the semantics once it's set to `true`
    var wasExpectedTypeAddedAsEqualityForSyntheticCall: Boolean = false
        private set

    fun markWasExpectedTypeAddedAsEqualityForSyntheticCall() {
        wasExpectedTypeAddedAsEqualityForSyntheticCall = true
    }

    // ---------------------------------------- Backing field ----------------------------------------

    var hasVisibleBackingField: Boolean = false

    // ---------------------------------------- Util ----------------------------------------

    var passedStages: Int = 0

    val constraintSystem: ConstraintSystemImpl
        get() = system

    private var cachedSyntheticCallableValueParameters: List<CfirValueParameter>? = null

    fun declaredParametersForMapping(): List<CfirValueParameter> {
        if (!symbol.isBound) return emptyList()
        return when (val declaration = symbol.cfir) {
            is CfirFunction -> declaration.valueParameters
            is CfirConstructor -> declaration.valueParameters
            is CfirEnumConstructor -> declaration.valueParameters
            is CfirVariable -> callableValueParametersForMapping(declaration)
            else -> emptyList()
        }
    }

    fun substitutedReturnType(): ConeCangJieType {
        val declared = when (val declaration = symbol.cfir) {
            is CfirFunction -> (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            is CfirConstructor -> (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            is CfirEnumConstructor -> enumConstructorOwnerType(declaration)
                ?: (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            is CfirVariable -> callableValueReturnType(declaration)
            else -> null
        } ?: return ConeErrorType(ConeSimpleDiagnostic("Unresolved return type"))

        return if (::substitutor.isInitialized) substitutor.substituteOrSelf(declared) else declared
    }

    private fun enumConstructorOwnerType(declaration: CfirEnumConstructor): ConeCangJieType? {
        val enumConstructorSymbol = symbol as? CfirEnumConstructorSymbol ?: return null
        val ownerClassId = callInfo.session.cfirProvider.getContainingClass(enumConstructorSymbol)?.classId
            ?: return null
        val ownerTypeArguments = enumConstructorTypeParameters(declaration).map { typeParameter ->
            ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
        }
        return ConeEnumType(ownerClassId.toLookupTag(), ownerTypeArguments)
    }

    private fun enumConstructorTypeParameters(declaration: CfirEnumConstructor): List<CfirTypeParameter> {
        if (declaration.typeParameters.isNotEmpty()) return declaration.typeParameters
        val enumConstructorSymbol = symbol as? CfirEnumConstructorSymbol ?: return emptyList()
        val ownerClassId = callInfo.session.cfirProvider.getContainingClass(enumConstructorSymbol)?.classId
            ?: return emptyList()
        val ownerDeclaration = callInfo.session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir
            ?: return emptyList()
        return when (ownerDeclaration) {
            is CfirClass -> ownerDeclaration.typeParameters
            is CfirInterface -> ownerDeclaration.typeParameters
            is CfirStruct -> ownerDeclaration.typeParameters
            is CfirEnum -> ownerDeclaration.typeParameters
            else -> emptyList()
        }
    }

    private fun callableValueReturnType(declaration: CfirVariable): ConeCangJieType? {
        val variableType = (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return null
        val functionType = variableType as? ConeFunctionType ?: return null
        return functionType.returnType
    }

    private fun callableValueParametersForMapping(declaration: CfirVariable): List<CfirValueParameter> {
        cachedSyntheticCallableValueParameters?.let { return it }

        val variableType = (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType
        val functionType = variableType as? ConeFunctionType
        if (functionType == null || functionType.parameterTypes.isEmpty()) {
            cachedSyntheticCallableValueParameters = emptyList()
            return emptyList()
        }

        val syntheticParameters = functionType.parameterTypes.mapIndexed { index, parameterType ->
            val parameterName = Name.identifier("callableValueArg$index")
            val parameterSymbol = CfirValueParameterSymbol(CallableId(parameterName))
            buildValueParameter {
                source = declaration.source
                moduleData = declaration.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.Error
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                isNamed = false
                dispatchReceiverType = null
                symbol = parameterSymbol
                containingDeclarationSymbol = symbol
                status = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
                returnTypeRef = buildResolvedTypeRef {
                    source = declaration.returnTypeRef.source
                    coneType = parameterType
                }
                name = parameterName
                defaultValue = null
            }
        }

        cachedSyntheticCallableValueParameters = syntheticParameters
        return syntheticParameters
    }


    /**
     * Please avoid updating symbol in the candidate whenever it's possible.
     * The only case when currently it seems to be unavoidable is at
     * [org.jetbrains.kotlin.fir.resolve.transformers.CfirCallCompletionResultsWriterTransformer.refineSubstitutedMemberIfReceiverContainsTypeVariable]
     */
    @RequiresOptIn
    annotation class UpdatingCandidateInvariants

    // ---------------------------------------- hashcode/equals/toString ----------------------------------------

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Candidate

        if (symbol != other.symbol) return false

        return true
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun toString(): String {
        val okOrFail = if (isSuccessful) "OK" else "FAIL"
        val step = "$passedStages/${callInfo.callKind.resolutionSequence.size}"
        return "$okOrFail($step): $symbol"
    }
}
