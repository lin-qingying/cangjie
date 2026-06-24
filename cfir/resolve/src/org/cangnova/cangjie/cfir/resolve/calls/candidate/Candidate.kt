/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.resolve.CfirSamResolver
import org.cangnova.cangjie.cfir.resolve.calls.CallableReferenceAdaptation
import org.cangnova.cangjie.cfir.resolve.calls.ConePostponedResolvedAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.stages.TypeArgumentMapping
import org.cangnova.cangjie.cfir.resolve.inference.InferenceComponents
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.semantics.isSuccess
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.impl.ResolvedImplicitTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability

/**
 * 单个调用解析候选。
 *
 * Candidate 持有候选符号、receiver、实参映射、约束系统、适用性诊断、变参信息、
 * callable reference / SAM / PCLA 等完成阶段状态，是 tower resolve 到 call completion 的核心状态对象。
 */
class Candidate(
    symbol: CfirBasedSymbol<*>,
    // Here we may have an ExpressionReceiverValue
    // - in case a use-site receiver is explicit
    // - in some cases with static entities, no matter is a use-site receiver explicit or not
    // OR we may have here a kind of ImplicitReceiverValue (non-statics only)
    /** 候选使用的 dispatch receiver atom。 */
    override var dispatchReceiver: ConeResolutionAtom?,
    /** 调用点给出的 extension receiver atom。 */
    val givenExtensionReceiver: ConeResolutionAtom?,
    /** 显式 receiver 的来源种类。 */
    override val explicitReceiverKind: ExplicitReceiverKind,
    /** 创建候选约束系统的工厂。 */
    private val constraintSystemFactory: InferenceComponents.ConstraintSystemFactory,
    /** tower resolve 传入的基础约束系统。 */
    private val baseSystem: ConstraintStorage,
    /** 当前候选所属调用的信息。 */
    override val callInfo: CallInfo,
    /** 产生该候选的 scope。 */
    val originScope: CfirScope?,
    /** 候选是否来自 companion object 类型 scope。 */
    val isFromCompanionObjectTypeScope: Boolean = false,
    // It's only true if we're in the member scope of smart cast receiver and this particular candidate came from original type
    /** smart-cast receiver 存在时，候选是否来自原始类型而非 smart-cast 类型。 */
    val isFromOriginalTypeInPresenceOfSmartCast: Boolean = false,
    bodyResolveContext: BodyResolveContext,
) : AbstractCallCandidate<ConeResolutionAtom>() {

    // ---------------------------------------- Symbol ----------------------------------------

    /** 当前候选绑定的 CFIR 符号；候选替换或 fake override 规整时可通过受控 API 更新。 */
    override var symbol: CfirBasedSymbol<*> = symbol
        private set

    /**
     * 更新候选符号。
     */
    @UpdatingCandidateInvariants
    fun updateSymbol(symbol: CfirBasedSymbol<*>) {
        this.symbol = symbol
    }

    // ---------------------------------------- Constraint system ----------------------------------------

    /** 候选约束系统是否依赖外层约束系统。 */
    override val usedOuterCs: Boolean get() = system.usesOuterCs

    /** 约束系统是否已经按需初始化。 */
    private var systemInitialized: Boolean = false
    /** 当前候选专属的约束系统，首次访问时根据 base system 和推断 session 初始化。 */
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

    /** 当前候选约束系统中已经记录的约束错误。 */
    override val errors: List<ConstraintSystemError>
        get() = system.errors

    /**
     * Substitutor from declared type parameters to type variables created for that candidate
     */
    lateinit var substitutor: ConeSubstitutor
        private set
    /** 为候选声明类型参数创建的 fresh type variable 列表。 */
    lateinit var freshVariables: List<ConeTypeVariable>
        private set

    /**
     * 初始化声明类型参数到 fresh type variable 的 substitutor 和变量列表。
     */
    fun initializeSubstitutorAndVariables(substitutor: ConeSubstitutor, freshVariables: List<ConeTypeVariable>) {
        this.substitutor = substitutor
        this.freshVariables = freshVariables
    }

    /**
     * 更新候选 substitutor。
     */
    @UpdatingCandidateInvariants
    fun updateSubstitutor(substitutor: ConeSubstitutor) {
        this.substitutor = substitutor
    }

    // ---------------------------------------- Conversions ----------------------------------------

    /** callable reference 解析出的最终函数类型。 */
    var resultingTypeForCallableReference: ConeCangJieType? = null
        private set

    /** callable reference 的 adaptation 信息。 */
    internal var callableReferenceAdaptation: CallableReferenceAdaptation? = null
        private set

    /**
     * 初始化 callable reference adaptation 与结果类型。
     */
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

    /**
     * 标记指定实参发生函数种类转换。
     */
    fun addFunctionKindConversionOfArgument(element: CfirExpression) {
        val set = argumentsWithFunctionKindConversion ?: HashSet<CfirExpression>().also { argumentsWithFunctionKindConversion = it }
        set += element
    }

    /** 实参到 SAM 转换信息的映射。 */
    var samConversionInfosOfArguments: HashMap<CfirExpression, CfirSamResolver.SamConversionInfo>? = null
        private set

    /**
     * 记录指定实参的 SAM 转换信息。
     */
    fun setSamConversionOfArgument(expression: CfirExpression, conversionInfo: CfirSamResolver.SamConversionInfo) {
        val map = samConversionInfosOfArguments
            ?: hashMapOf<CfirExpression, CfirSamResolver.SamConversionInfo>().also { samConversionInfosOfArguments = it }
        map[expression] = conversionInfo
    }

    // Computed getters

    /** 候选是否使用了 SAM 转换。 */
    val usesSamConversion: Boolean
        get() = samConversionInfosOfArguments != null

    /** 候选是否使用 SAM 转换或 SAM 构造器。 */
    val usesSamConversionOrSamConstructor: Boolean
        get() = usesSamConversion || symbol.origin == CfirDeclarationOrigin.SamConstructor

    /** 候选是否使用函数种类转换。 */
    val usesFunctionKindConversion: Boolean
        get() = argumentsWithFunctionKindConversion != null || callableReferenceAdaptation?.hasFunctionKindConversion() == true

    // ---------------------------------------- Argument mapping ----------------------------------------

    /** 候选原始实参 atom 列表。 */
    private var _arguments: List<ConeResolutionAtom>? = null
    /** 候选原始实参 atom 列表。 */
    val arguments: List<ConeResolutionAtom>
        get() = _arguments ?: error("Argument list is not initialized yet")

    /** 候选实参到值参数的映射。 */
    private var _argumentMapping: LinkedHashMap<ConeResolutionAtom, CfirValueParameter>? = null
    /** 实参映射是否已经初始化。 */
    override val argumentMappingInitialized: Boolean
        get() = _argumentMapping != null
    /** 候选实参到值参数的映射。 */
    override val argumentMapping: LinkedHashMap<ConeResolutionAtom, CfirValueParameter>
        get() = _argumentMapping ?: error("Argument mapping is not initialized yet")

    /** 当前候选普通变参形参。 */
    private var _variadicParameter: CfirValueParameter? = null
    /** 普通变参元素类型。 */
    private var _variadicElementType: ConeCangJieType? = null
    /** 固定位置实参数量。 */
    private var _variadicFixedPositionalArity: Int? = null
    /** 被标记为变参实参的 atom 到期望元素类型映射。 */
    private var _variadicArgumentExpectedTypes: LinkedHashMap<ConeResolutionAtom, ConeCangJieType>? = null

    /** 候选是否使用普通变参调用。 */
    val usesVariadicCall: Boolean
        get() = _variadicArgumentExpectedTypes?.isNotEmpty() == true ||
                (_variadicParameter != null && _variadicFixedPositionalArity == arguments.size)

    /**
     * 初始化候选实参列表和实参映射。
     */
    fun initializeArgumentMapping(
        arguments: List<ConeResolutionAtom>,
        argumentMapping: LinkedHashMap<ConeResolutionAtom, CfirValueParameter>,
    ) {
        require(_argumentMapping == null) { "Argument mapping already initialized" }
        _argumentMapping = argumentMapping
        _arguments = arguments
    }

    /**
     * 更新候选实参映射。
     */
    @UpdatingCandidateInvariants
    fun updateArgumentMapping(argumentMapping: LinkedHashMap<ConeResolutionAtom, CfirValueParameter>) {
        _argumentMapping = argumentMapping
        _variadicArgumentExpectedTypes?.keys?.retainAll(argumentMapping.keys)
    }

    /**
     * 初始化普通变参调用信息。
     */
    fun initializeVariadicCallInfo(
        parameter: CfirValueParameter,
        elementType: ConeCangJieType,
        fixedPositionalArity: Int,
    ) {
        require(_variadicParameter == null || _variadicParameter == parameter) {
            "Variadic call info already initialized"
        }
        _variadicParameter = parameter
        _variadicElementType = elementType
        _variadicFixedPositionalArity = fixedPositionalArity
    }

    /**
     * 判断 atom 是否可以作为变参实参。
     */
    fun canUseVariadicArgument(atom: ConeResolutionAtom): Boolean =
        _variadicParameter != null && argumentMapping[atom] == _variadicParameter

    /**
     * 标记 atom 为变参实参，并返回元素期望类型。
     */
    fun markVariadicArgument(atom: ConeResolutionAtom): ConeCangJieType? {
        val elementType = _variadicElementType ?: return null
        val expectedTypes = _variadicArgumentExpectedTypes
            ?: LinkedHashMap<ConeResolutionAtom, ConeCangJieType>().also {
                _variadicArgumentExpectedTypes = it
            }
        expectedTypes[atom] = elementType
        return elementType
    }

    /**
     * 标记空变参调用。
     */
    fun markEmptyVariadicCall() {
        require(_variadicParameter != null) { "Variadic call info is not initialized" }
    }

    /**
     * 查询变参实参的期望类型。
     */
    fun variadicExpectedTypeForArgument(atom: ConeResolutionAtom): ConeCangJieType? =
        _variadicArgumentExpectedTypes?.get(atom)

    /**
     * 判断指定参数是否为当前候选的变参形参。
     */
    fun hasVariadicParameter(parameter: CfirValueParameter): Boolean =
        _variadicParameter == parameter

    /** 固定位置实参数量。 */
    val variadicFixedPositionalArity: Int?
        get() = _variadicFixedPositionalArity

    /**
     * 实参前缀替换后同步变参实参期望类型映射。
     */
    private fun remapVariadicArgumentExpectedTypes(
        oldToNewArguments: Map<ConeResolutionAtom, ConeResolutionAtom>,
        remainingArguments: List<ConeResolutionAtom>,
    ) {
        val oldExpectedTypes = _variadicArgumentExpectedTypes ?: return
        val newExpectedTypes = LinkedHashMap<ConeResolutionAtom, ConeCangJieType>()
        for ((oldArgument, newArgument) in oldToNewArguments) {
            oldExpectedTypes[oldArgument]?.let { newExpectedTypes[newArgument] = it }
        }
        for (argument in remainingArguments) {
            oldExpectedTypes[argument]?.let { newExpectedTypes[argument] = it }
        }
        _variadicArgumentExpectedTypes = newExpectedTypes.takeIf { it.isNotEmpty() }
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
        val oldToNewArguments = LinkedHashMap<ConeResolutionAtom, ConeResolutionAtom>()
        for ((oldArgument, newArgument) in arguments.zip(newArgumentPrefix)) {
            newArgumentMapping[newArgument] = argumentMapping.getValue(oldArgument)
            oldToNewArguments[oldArgument] = newArgument
        }

        for (argument in remainingArguments) {
            argumentMapping[argument]?.let { newArgumentMapping[argument] = it }
        }

        val newArguments = newArgumentPrefix + remainingArguments

        _arguments = newArguments
        _argumentMapping = newArgumentMapping
        remapVariadicArgumentExpectedTypes(oldToNewArguments, remainingArguments)
    }

    /** 调用中使用默认参数的数量。 */
    var numDefaults: Int = 0
    /** 候选是否使用 quest fallback。 */
    var usedQuestFallback: Boolean = false
    /** 候选是否使用理想数值兼容。 */
    var usedIdealNumericCompatibility: Boolean = false
    /** 候选是否通过 extend 参与解析。 */
    var usedExtendParticipation: Boolean = false

    /** 当前调用实际采用的仓颉变参形参。 */
    private var _cangjieVariadicParameterForCall: CfirValueParameter? = null

    /**
     * 本次调用实际采用的仓颉变参形参。
     *
     * 仓颉变参由调用解析按 `Array<T>` 形状解糖产生，不能只看声明形状；
     * 普通数组实参匹配同一个声明时不应被标记为变参调用。
     */
    val cangjieVariadicParameterForCall: CfirValueParameter?
        get() = _cangjieVariadicParameterForCall

    /** 候选是否使用仓颉变参调用。 */
    val usesCangjieVariadicCall: Boolean
        get() = _cangjieVariadicParameterForCall != null

    /**
     * 初始化当前调用实际采用的仓颉变参形参。
     */
    fun initializeCangjieVariadicParameterForCall(parameter: CfirValueParameter?) {
        require(_cangjieVariadicParameterForCall == null) {
            "Cangjie variadic parameter for call already initialized"
        }
        _cangjieVariadicParameterForCall = parameter
    }

    // ---------------------------------------- Type argument mapping ----------------------------------------

    /** 显式/隐式类型实参映射。 */
    lateinit var typeArgumentMapping: TypeArgumentMapping

    // ---------------------------------------- Postponed atoms ----------------------------------------

    /** 需要调用完成阶段分析的 postponed atom 列表。 */
    val postponedAtoms: MutableList<ConePostponedResolvedAtom> = mutableListOf()

    /**
     * 追加 postponed atom。
     */
    fun addPostponedAtom(atom: ConePostponedResolvedAtom) {
        postponedAtoms += atom
    }

    // ------------------------ Context-sensitively resolved arguments ------------------------------------

    /** 上下文敏感解析或集合字面量解析产生的实参替换映射。 */
    private var _updatedArguments: MutableMap<CfirElement, CfirExpression>? =
        null

    /**
     * 注册一个实参替换。
     */
    private fun setUpdatedArgument(old: CfirExpression, new: CfirExpression) {
        if (_updatedArguments == null) {
            _updatedArguments = mutableMapOf()
        }

        val existingValue = _updatedArguments!!.put(old, new)
        check(existingValue == null) {
            "We shouldn't put the value for $old twice"
        }
    }

    /**
     * 记录上下文敏感解析后的简单名替换。
     */
    fun setUpdatedArgumentFromContextSensitiveResolution(old: CfirNamedAccessExpression, new: CfirExpression) {
        setUpdatedArgument(old, new)
    }

    /**
     * 记录集合字面量解析后的替换。
     */
    fun setUpdatedCollectionLiteral(old: CfirArrayLiteral, new: CfirExpression) {
        setUpdatedArgument(old, new)
    }

    /** 实参替换映射。 */
    val argumentReplacements: Map<CfirElement, CfirExpression>?
        get() = _updatedArguments

    // ---------------------------------------- PCLA-related parts ----------------------------------------

    /** PCLA completion 后需要写回的 postponed 调用。 */
    val postponedPCLACalls: MutableList<ConeResolutionAtom> = mutableListOf()
    /** 已经通过 PCLA 分析过的 lambda 声明。 */
    val lambdasAnalyzedWithPCLA: MutableList<CfirDeclaration> = mutableListOf()

    // Retained as an upstream-aligned callback seam for delegated-property/PCLA completion-result writing.
    // In the current local direct chain there is no CfirDelegatedPropertyInferenceSession or writer-mode
    // call site that populates this list, so these callbacks remain structurally available but unreachable.
    /** PCLA 完成结果写回回调。 */
    val onPCLACompletionResultsWritingCallbacks: MutableList<(ConeSubstitutor) -> Unit> = mutableListOf()

    // ---------------------------------------- Applicability ----------------------------------------

    /** 当前候选所有诊断中的最低适用性。 */
    var lowestApplicability: CandidateApplicability = CandidateApplicability.RESOLVED
        private set

    /** 对外暴露的候选适用性，等于当前最低诊断适用性。 */
    override val applicability: CandidateApplicability
        get() = lowestApplicability

    /** 候选在各 resolution stage 中收集到的诊断。 */
    override val diagnostics: MutableList<ResolutionDiagnostic> = mutableListOf()

    /**
     * 添加候选诊断并更新最低适用性。
     */
    fun addDiagnostic(diagnostic: ResolutionDiagnostic) {
        diagnostics += diagnostic
        if (diagnostic.applicability < lowestApplicability) {
            lowestApplicability = diagnostic.applicability
        }
    }

    /**
     * 恢复候选最低适用性。
     */
    internal fun restoreLowestApplicability(applicability: CandidateApplicability) {
        lowestApplicability = applicability
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

    /** 最终选择的 extension receiver atom，初始值来自调用点给定 receiver。 */
    override var chosenExtensionReceiver: ConeResolutionAtom? = givenExtensionReceiver

    /** 上下文参数对应的 receiver/argument atom 列表。 */
    override var contextArguments: List<ConeResolutionAtom>? = null

    /**
     * In case `f: context(C..) (V) -> ..`, `f(e..)`, context values are still being introduced as a prefix of
     * regular arguments for `invoke` function.
     */
    var expectedContextParameterCountForInvoke: Int? = null

    /**
     * 返回 dispatch receiver 对应表达式。
     */
    fun dispatchReceiverExpression(): CfirExpression? {
        return dispatchReceiver?.expression
    }

    /**
     * 返回选中的 extension receiver 对应表达式。
     */
    fun chosenExtensionReceiverExpression(): CfirExpression? {
        return chosenExtensionReceiver?.expression
    }

    /**
     * 返回 context arguments 对应表达式列表。
     */
    fun contextArguments(): List<CfirExpression> {
        return contextArguments?.map { it.expression } ?: emptyList()
    }

    /** receiver source 是否已经更新。 */
    private var sourcesWereUpdated = false

    // In case of implicit receivers we want to update corresponding sources to generate correct offset. This method must be called only
    // once when candidate was selected and confirmed to be correct one.
    /**
     * 标记 receiver source 已更新。
     */
    fun updateSourcesOfReceivers() {
        require(!sourcesWereUpdated)
        sourcesWereUpdated = true
    }

    // This thing is mostly for a common fast-path optimization and should not affect the semantics once it's set to `true`
    /** synthetic call 是否已经把期望类型作为 equality 约束加入。 */
    var wasExpectedTypeAddedAsEqualityForSyntheticCall: Boolean = false
        private set

    /**
     * 标记 synthetic call 已经使用期望类型 equality 约束。
     */
    fun markWasExpectedTypeAddedAsEqualityForSyntheticCall() {
        wasExpectedTypeAddedAsEqualityForSyntheticCall = true
    }

    // ---------------------------------------- Backing field ----------------------------------------

    /** 候选是否有可见 backing field。 */
    var hasVisibleBackingField: Boolean = false

    // ---------------------------------------- Util ----------------------------------------

    /** 候选已通过的 resolution stage 数量。 */
    var passedStages: Int = 0

    /** 候选约束系统视图。 */
    val constraintSystem: ConstraintSystemImpl
        get() = system

    /** callable value 合成参数缓存。 */
    private var cachedSyntheticCallableValueParameters: List<CfirValueParameter>? = null

    /**
     * 返回实参映射使用的声明参数列表。
     */
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

    /**
     * 计算候选替换后的返回类型。
     */
    fun substitutedReturnType(declaredReturnType: ConeCangJieType? = null): ConeCangJieType {
        val declaration = symbol.cfir
        val declared = declaredReturnType ?: when (declaration) {
            is CfirFunction -> declaration.returnTypeRef.resolvedConeTypeOrNull()
            is CfirConstructor -> declaration.returnTypeRef.resolvedConeTypeOrNull()
            is CfirEnumConstructor -> enumConstructorOwnerType(declaration)
                ?: declaration.returnTypeRef.resolvedConeTypeOrNull()
            is CfirVariable -> callableValueReturnType(declaration)
            else -> null
        } ?: return ConeErrorType(ConeSimpleDiagnostic("Unresolved return type"))

        val substituted = if (::substitutor.isInitialized) substitutor.substituteOrSelf(declared) else declared
        return if (declaration is CfirFunction && declaration.origin == CfirDeclarationOrigin.Synthetic.FakeFunction) {
            substituted
        } else {
            substituted.replaceThisTypeWithDispatchReceiver()
        }
    }

    /**
     * 将返回类型中的 `This` 类型替换为当前 receiver 绑定类型。
     */
    private fun ConeCangJieType.replaceThisTypeWithDispatchReceiver(): ConeCangJieType {
        val fallbackType = when {
            this is ConeClassLikeType && isThisType -> this
            this is ConeErrorType && diagnostic.isThisTypeNotAllowed -> delegatedType ?: return this
            else -> return this
        }
        return thisTypeBindingReceiverType()
            ?: fallbackType
    }

    /**
     * 计算当前调用能绑定 `This` 的 receiver 类型。
     */
    private fun thisTypeBindingReceiverType(): ConeCangJieType? {
        if (callInfo.explicitReceiver == null) {
            containingClassThisType()?.let { return it }
        }
        return dispatchReceiverExpression()?.coneTypeOrNull
            ?: chosenExtensionReceiverExpression()?.coneTypeOrNull
    }

    /**
     * 构造当前包含 class 的 `This` 类型。
     */
    private fun containingClassThisType(): ConeCangJieType? {
        val containingClass = callInfo.containingDeclarations
            .asReversed()
            .filterIsInstance<CfirClass>()
            .firstOrNull()
            ?: return null
        val classSymbol = containingClass.symbol as? CfirClassSymbol ?: return null
        val typeArguments = containingClass.typeParameters.map { typeParameter ->
            ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
        }
        return classSymbol.constructThisType(typeArguments)
    }

    /** 诊断是否表示 `This` 类型位置不允许。 */
    private val ConeDiagnostic.isThisTypeNotAllowed: Boolean
        get() = (this as? ConeSimpleDiagnostic)?.kind == DiagnosticKind.ThisTypeNotAllowed

    /**
     * 计算 enum constructor 的 owner enum 类型。
     */
    private fun enumConstructorOwnerType(declaration: CfirEnumConstructor): ConeCangJieType? {
        val enumConstructorSymbol = symbol as? CfirEnumConstructorSymbol ?: return null
        val ownerClassId = callInfo.session.cfirProvider.getContainingClass(enumConstructorSymbol)?.classId
            ?: return null
        val ownerTypeArguments = enumConstructorTypeParameters(declaration).map { typeParameter ->
            ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
        }
        return ConeEnumType(ownerClassId.toLookupTag(), ownerTypeArguments)
    }

    /**
     * 获取 enum constructor 可用的 owner 类型参数。
     */
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

    /**
     * 对 callable value 候选提取函数类型返回类型。
     */
    private fun callableValueReturnType(declaration: CfirVariable): ConeCangJieType? {
        val variableType = declaration.returnTypeRef.resolvedConeTypeOrNull() ?: return null
        val functionType = variableType as? ConeFunctionType ?: return null
        return functionType.returnType
    }

    /**
     * 从显式或隐式 resolved type ref 读取 Cone 类型。
     */
    private fun CfirTypeRef.resolvedConeTypeOrNull(): ConeCangJieType? = when (this) {
        is CfirResolvedTypeRef -> coneType
        is ResolvedImplicitTypeRef -> typeRef.coneType
        else -> null
    }

    /**
     * 为 callable value 候选合成形参列表。
     */
    private fun callableValueParametersForMapping(declaration: CfirVariable): List<CfirValueParameter> {
        cachedSyntheticCallableValueParameters?.let { return it }

        val variableType = declaration.returnTypeRef.resolvedConeTypeOrNull()
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

    /**
     * 候选相等性按候选符号判定。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Candidate

        if (symbol != other.symbol) return false

        return true
    }

    /**
     * 候选哈希值按符号计算。
     */
    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    /**
     * 渲染候选调试字符串。
     */
    override fun toString(): String {
        val okOrFail = if (isSuccessful) "OK" else "FAIL"
        val step = "$passedStages/${callInfo.callKind.resolutionSequence.size}"
        return "$okOrFail($step): $symbol"
    }
}
