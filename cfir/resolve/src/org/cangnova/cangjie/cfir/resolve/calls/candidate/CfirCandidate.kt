package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.constraints.CfirConstraintIssue
import org.cangnova.cangjie.cfir.constraints.CfirConstraintSystem
import org.cangnova.cangjie.cfir.constraints.CfirTypeSubstitutor
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.constraints.ExplicitReceiverKind
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.CandidateApplicability
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.semantics.isSuccess
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType

class CfirCandidate(
    symbol: CfirSymbol<*>,
    override var dispatchReceiver: CfirResolutionAtom?,
    val givenExtensionReceiver: CfirResolutionAtom?,
    override val explicitReceiverKind: ExplicitReceiverKind,
    override val callInfo: CfirCallInfo,
    val originScope: CfirScope?,
    private val resolutionContext: CfirResolutionContext,
    var constraintSystem: CfirConstraintSystem?,
) : AbstractCallCandidate<CfirResolutionAtom>() {

    override var symbol: CfirSymbol<*> = symbol
        private set

    fun updateSymbol(symbol: CfirSymbol<*>) {
        this.symbol = symbol
    }

    override val system: CfirConstraintSystem
        get() = requireNotNull(constraintSystem) { "Constraint system is not initialized yet" }

    override val errors: List<CfirConstraintIssue>
        get() = constraintSystem?.errors ?: emptyList()

    var substitutor: CfirTypeSubstitutor = CfirTypeSubstitutor.Empty
        private set

    var freshVariables: List<CfirTypeVariable> = emptyList()
        private set

    fun initializeSubstitutorAndVariables(
        substitutor: CfirTypeSubstitutor,
        freshVariables: List<CfirTypeVariable>,
    ) {
        this.substitutor = substitutor
        this.freshVariables = freshVariables
    }

    fun updateSubstitutor(substitutor: CfirTypeSubstitutor) {
        this.substitutor = substitutor
    }

    var numDefaults: Int = 0

    private var _arguments: List<CfirResolutionAtom>? = null
    val arguments: List<CfirResolutionAtom>
        get() = _arguments ?: error("Argument list is not initialized yet")

    private var _argumentMapping: LinkedHashMap<CfirResolutionAtom, CfirValueParameter>? = null
    override val argumentMappingInitialized: Boolean
        get() = _argumentMapping != null
    override val argumentMapping: LinkedHashMap<CfirResolutionAtom, CfirValueParameter>
        get() = _argumentMapping ?: error("Argument mapping is not initialized yet")

    fun initializeArgumentMapping(
        arguments: List<CfirResolutionAtom>,
        argumentMapping: LinkedHashMap<CfirResolutionAtom, CfirValueParameter>,
    ) {
        _arguments = arguments
        _argumentMapping = argumentMapping
    }

    fun updateArgumentMapping(argumentMapping: LinkedHashMap<CfirResolutionAtom, CfirValueParameter>) {
        _argumentMapping = argumentMapping
    }

    fun declaredParametersForMapping(): List<CfirValueParameter> = argumentMapping.values.toList()

    fun replaceArgumentPrefix(newArgumentPrefix: List<CfirResolutionAtom>) {
        val remainingArguments = arguments.subList(newArgumentPrefix.size, arguments.size)
        val newArgumentMapping = LinkedHashMap<CfirResolutionAtom, CfirValueParameter>()
        for ((oldArgument, newArgument) in arguments.zip(newArgumentPrefix)) {
            newArgumentMapping[newArgument] = argumentMapping.getValue(oldArgument)
        }
        for (argument in remainingArguments) {
            argumentMapping[argument]?.let { newArgumentMapping[argument] = it }
        }
        _arguments = newArgumentPrefix + remainingArguments
        _argumentMapping = newArgumentMapping
    }

    private var _updatedArguments: MutableMap<CfirElement, CfirExpression>? = null

    fun setUpdatedArgumentFromContextSensitiveResolution(old: CfirExpression, new: CfirExpression) {
        setUpdatedArgument(old, new)
    }

    fun setUpdatedCollectionLiteral(old: CfirExpression, new: CfirExpression) {
        setUpdatedArgument(old, new)
    }

    val argumentReplacements: Map<CfirElement, CfirExpression>?
        get() = _updatedArguments

    private fun setUpdatedArgument(old: CfirExpression, new: CfirExpression) {
        if (_updatedArguments == null) {
            _updatedArguments = mutableMapOf()
        }
        _updatedArguments!![old] = new
    }

    override var chosenExtensionReceiver: CfirResolutionAtom? = givenExtensionReceiver
    override var contextArguments: List<CfirResolutionAtom>? = null

    private val _diagnostics: MutableList<ResolutionDiagnostic> = mutableListOf()
    override val diagnostics: List<ResolutionDiagnostic>
        get() = _diagnostics

    var lowestApplicability: CandidateApplicability = CandidateApplicability.RESOLVED
        private set

    override val applicability: CandidateApplicability
        get() = lowestApplicability

    fun addDiagnostic(diagnostic: ResolutionDiagnostic) {
        _diagnostics += diagnostic
        if (diagnostic.applicability < lowestApplicability) {
            lowestApplicability = diagnostic.applicability
        }
    }

    val isSuccessful: Boolean
        get() = _diagnostics.all { it.applicability.isSuccess } && !(constraintSystem?.hasErrors ?: false)

    var hasVisibleBackingField: Boolean = false

    var usedQuestFallback: Boolean = false
        private set

    var usedIdealNumericCompatibility: Boolean = false
        private set

    var usedExtendParticipation: Boolean = false
        private set

    fun markUsedQuestFallback() {
        usedQuestFallback = true
    }

    fun markUsedIdealNumericCompatibility() {
        usedIdealNumericCompatibility = true
    }

    fun markUsedExtendParticipation() {
        usedExtendParticipation = true
    }

    var passedStages: Int = 0

    private var sourcesWereUpdated = false

    fun updateSourcesOfReceivers() {
        if (sourcesWereUpdated) return
        sourcesWereUpdated = true
        dispatchReceiver = dispatchReceiver?.normalizeReceiverAtom()
        chosenExtensionReceiver = chosenExtensionReceiver?.normalizeReceiverAtom()
        contextArguments = contextArguments?.map { it.normalizeReceiverAtom() }
    }

    private fun CfirResolutionAtom.normalizeReceiverAtom(): CfirResolutionAtom {
        return this
    }

    /**
     * 从符号签名提取原始返回类型（未代入的）。
     * 对于普通函数/构造器，返回声明的返回类型；
     * 对于枚举构造器，返回带类型参数占位符的枚举类型。
     */
    fun signatureReturnType(): ConeCangJieType? {
        if (!symbol.isBound) return null
        return when (val decl = symbol.cfir) {
            is CfirFunction -> (decl.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            is CfirConstructor -> (decl.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            is CfirEnumConstructor -> extractEnumReturnType(decl)
            else -> null
        }
    }

    /**
     * 应用 substitutor 后的最终返回类型。
     * 由 CfirInferTypeArguments 阶段更新 substitutor 后，此方法返回具体类型。
     */
    fun substitutedReturnType(): ConeCangJieType? {
        val sigType = signatureReturnType() ?: return null
        return substitutor.substituteOrSelf(sigType)
    }

    private fun extractEnumReturnType(decl: CfirEnumConstructor): ConeCangJieType? {
        val enumSymbol = decl.symbol as? CfirEnumConstructorSymbol ?: return null
        val session = callInfo.session
        val classId = session.symbolProvider.getEnumConstructorOwnerClassId(enumSymbol)
            ?: session.cfirProvider.getEnumConstructorOwnerClassId(enumSymbol)
            ?: return null
        val effectiveTypeParams = decl.typeParameters.ifEmpty {
            session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir?.typeParameters
                ?: decl.typeParameters
        }
        val typeArguments = effectiveTypeParams.map {
            ConeTypeParameterType(ConeTypeParameterLookupTag(it.name.asString()))
        }
        return ConeEnumType(ConeClassLookupTagImpl(classId), typeArguments)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CfirCandidate
        return symbol == other.symbol
    }

    override fun hashCode(): Int = symbol.hashCode()

    override fun toString(): String {
        val okOrFail = if (isSuccessful) "OK" else "FAIL"
        val step = "$passedStages/${callInfo.callKind.resolutionSequence.size}"
        return "$okOrFail($step): $symbol"
    }
}
