package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvedForStatuslessDeclaration
import org.cangnova.cangjie.cfir.scopes.unsubstitutedScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.util.PrivateForInline

internal class CfirStatusResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.STATUS,
) {
    override val transformer: CfirStatusResolveTransformer = run {
        val statusComputationSession = CfirStatusComputationSession(session, scopeSession)
        CfirStatusResolveTransformer(statusComputationSession)
    }
}

open class CfirStatusComputationSession(
    val useSiteSession: CfirSession,
    val useSiteScopeSession: ScopeSession,
) {
    private val statusMap: MutableMap<CfirDeclaration, StatusComputationStatus> =
        hashMapOf<CfirDeclaration, StatusComputationStatus>()
            .withDefault { StatusComputationStatus.NotComputed }

    operator fun get(declaration: CfirDeclaration): StatusComputationStatus = statusMap.getValue(declaration)

    fun startComputing(declaration: CfirDeclaration): StatusComputationStatus {
        return statusMap.getOrPut(declaration) { StatusComputationStatus.Computing }
    }

    fun endComputing(declaration: CfirDeclaration) {
        statusMap[declaration] = StatusComputationStatus.Computed
    }

    fun computeOnlyDeclarationStatus(declaration: CfirDeclaration) {
        val existedStatus = statusMap.getValue(declaration)
        if (existedStatus < StatusComputationStatus.ComputedOnlyDeclarationStatus) {
            statusMap[declaration] = StatusComputationStatus.ComputedOnlyDeclarationStatus
        }
    }

    enum class StatusComputationStatus(val requiresComputation: Boolean) {
        NotComputed(true),
        Computing(false),
        ComputedOnlyDeclarationStatus(true),
        Computed(false),
    }

    /**
     * 对齐 Kotlin `StatusComputationSession.forceResolveStatusesOfSupertypes`。
     *
     * STATUS 主干在进入当前 source class 之前，必须先把所有 super class 的 STATUS 推到稳定态，
     * 否则 override / accessor / enum constructor 都会在半初始化状态下读取上层信息。
     */
    open fun forceResolveStatusesOfSupertypes(declaration: CfirDeclaration) {
        val classLikeDeclaration = declaration as? CfirClassLikeDeclaration ?: return
        for (superTypeRef in classLikeDeclaration.superTypeRefs + additionalSuperTypes(classLikeDeclaration)) {
            for (classifierSymbol in superTypeToSymbols(superTypeRef)) {
                forceResolveStatusOfCorrespondingClass(classifierSymbol)
            }
        }
    }

    /**
     * 对齐 Kotlin FIR `StatusComputationSession.superTypeToSymbols`。
     *
     * 主干默认只按当前 use-site session 查询 class-like symbol；
     * low-level 会在子类里扩展为多 session 搜索。
     */
    open fun superTypeToSymbols(typeRef: CfirTypeRef): Collection<CfirClassLikeSymbol<*>> {
        val classId = typeRef.coneType.classId ?: return emptyList()
        return listOfNotNull(useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId))
    }

    private fun forceResolveStatusOfCorrespondingClass(classLikeSymbol: CfirClassLikeSymbol<*>) {
        when (classLikeSymbol) {
            is CfirClassSymbol -> forceResolveStatusesOfClassLike(classLikeSymbol.cfir)
            is CfirInterfaceSymbol -> forceResolveStatusesOfClassLike(classLikeSymbol.cfir)
            is CfirStructSymbol -> forceResolveStatusesOfClassLike(classLikeSymbol.cfir)
            is CfirEnumSymbol -> forceResolveStatusesOfClassLike(classLikeSymbol.cfir)
            is CfirPrimitiveTypeSymbol -> forceResolveStatusesOfClassLike(classLikeSymbol.cfir)
            is CfirTypeAliasSymbol -> {
                for (expandedSymbol in superTypeToSymbols(classLikeSymbol.cfir.expandedTypeRef)) {
                    forceResolveStatusOfCorrespondingClass(expandedSymbol)
                }
            }
        }
    }

    private fun forceResolveStatusesOfClassLike(classLikeDeclaration: CfirClassLikeDeclaration) {
        if (classLikeDeclaration.origin != CfirDeclarationOrigin.Source) {
            val computationStatus = this[classLikeDeclaration]
            if (!computationStatus.requiresComputation) return

            startComputing(classLikeDeclaration)
            forceResolveStatusesOfSupertypes(classLikeDeclaration)
            endComputing(classLikeDeclaration)
            return
        }

        val computationStatus = this[classLikeDeclaration]
        if (!computationStatus.requiresComputation) return
        resolveClassForSuperType(classLikeDeclaration)
    }

    /**
     * 对齐 Kotlin FIR `StatusComputationSession.resolveClassForSuperType`。
     *
     * 主干必须真正推进 source class 的 STATUS，而不是永久返回 `false`。
     * 这里没有 Kotlin 的 designation 基础设施，因此直接对目标 class subtree 执行 STATUS resolve。
     */
    open fun resolveClassForSuperType(classLikeDeclaration: CfirClassLikeDeclaration): Boolean {
        classLikeDeclaration.transformSingle(CfirStatusResolveTransformer(this), null)
        return true
    }

    /**
     * 对齐 Kotlin FIR `StatusComputationSession.additionalSuperTypes`。
     *
     * 仓颉主干默认没有额外 platform-mapped super type。
     */
    open fun additionalSuperTypes(classLikeDeclaration: CfirClassLikeDeclaration): List<CfirTypeRef> = emptyList()
}

open class AbstractCfirStatusResolveTransformer(
    val statusComputationSession: CfirStatusComputationSession,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.STATUS) {
    @PrivateForInline
    val classes: MutableList<CfirClass> = mutableListOf()
    val statusResolver: CfirStatusResolver = CfirStatusResolver(session, statusComputationSession.useSiteScopeSession)
    override val session: CfirSession
        get() = statusComputationSession.useSiteSession
    @OptIn(PrivateForInline::class)
    val containingClass: CfirClass?
        get() = classes.lastOrNull()

    @OptIn(PrivateForInline::class)
    inline fun storeClass(
        klass: CfirClass,
        computeResult: () -> Unit,
    ) {
        classes += klass
        computeResult()
        classes.removeAt(classes.lastIndex)
    }

    override fun <E : CfirElement> transformElement(element: E, data: Nothing?): E {
        if (element is CfirDeclaration) {
            @Suppress("UNCHECKED_CAST")
            return transformDeclaration(element, data) as E
        }
        return super.transformElement(element, data)
    }

    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        return withResolvedStatusPhase(declaration) {
            declaration.transformChildren(this, data)
        }
    }

    /**
     * 统一 STATUS 发布入口。
     *
     * 这里负责 phase 迁移和最终 resolved-status 收敛；真正的“怎么解状态”由各个具体声明 override。
     */
    protected fun <D : CfirDeclaration> withResolvedStatusPhase(
        target: D,
        action: () -> Unit,
    ): D {
        if (target.resolvePhase < CfirResolvePhase.TYPES || target.resolvePhase >= CfirResolvePhase.STATUS) {
            return target
        }

        when (statusComputationSession.startComputing(target)) {
            CfirStatusComputationSession.StatusComputationStatus.Computed,
            CfirStatusComputationSession.StatusComputationStatus.Computing,
            -> return target

            else -> Unit
        }

        action()
        if (target is CfirMemberDeclaration) {
            target.publishResolvedStatusIfNeeded()
        }
        target.replaceResolvePhase(CfirResolvePhase.STATUS)
        statusComputationSession.endComputing(target)
        return target
    }

    protected fun transformClassMembers(klass: CfirClass) {
        val declarations = klass.declarations
        declarations.forEach { declaration ->
            if (declaration !is CfirClassLikeDeclaration) {
                declaration.transformSingle(this, null)
            }
        }
        declarations.forEach { declaration ->
            if (declaration is CfirClassLikeDeclaration) {
                declaration.transformSingle(this, null)
            }
        }
    }

    protected fun transformExtendMembers(extend: CfirExtend) {
        val declarations = extend.declarations
        declarations.forEach { declaration ->
            if (declaration !is CfirClassLikeDeclaration) {
                declaration.transformSingle(this, null)
            }
        }
        declarations.forEach { declaration ->
            if (declaration is CfirClassLikeDeclaration) {
                declaration.transformSingle(this, null)
            }
        }
    }

    override fun transformClass(klass: CfirClass, data: Nothing?): CfirClass {
        val outerClass = containingClass
        return withResolvedStatusPhase(klass) {
            storeClass(klass) {
                statusComputationSession.forceResolveStatusesOfSupertypes(klass)
                klass.transformTypeParameters(this, null)
                transformClassStatus(klass, outerClass)
                transformClassMembers(klass)
            }
        }
    }

    /**
     * 只计算并发布 class 自身 STATUS。
     *
     * Low-level lazy resolver 会在自己的写锁中调用该入口，避免 class 目标重新落入 generic
     * `transformSingle` 路径后被 phase 状态短路。
     */
    fun transformClassStatus(
        klass: CfirClass,
        containingClass: CfirClass? = this.containingClass,
    ) {
        klass.replaceStatus(statusResolver.resolveStatus(klass, containingClass, isLocal = false))
    }

    override fun transformTypeAlias(typeAlias: CfirTypeAlias, data: Nothing?): CfirTypeAlias {
        return withResolvedStatusPhase(typeAlias) {
            typeAlias.transformTypeParameters(this, null)
            typeAlias.replaceStatus(statusResolver.resolveStatus(typeAlias, containingClass, isLocal = false))
        }
    }

    override fun transformExtend(extend: CfirExtend, data: Nothing?): CfirExtend {
        return withResolvedStatusPhase(extend) {
            transformExtendStatusWithoutPhaseGuard(extend)
            transformExtendMembers(extend)
        }
    }

    /**
     * 只计算并发布 extend 自身 STATUS。
     *
     * `extend` 是仓颉特有的成员容器节点，LL STATUS resolver 需要在写锁中直接调用该入口，
     * 避免 generic `transformSingle` 路径被 phase 状态短路。
     */
    fun transformExtendStatusWithoutPhaseGuard(extend: CfirExtend) {
        extend.transformTypeParameters(this, null)
        extend.replaceStatus(statusResolver.resolveStatus(extend, containingClass, isLocal = false))
    }

    override fun transformFunction(function: CfirFunction, data: Nothing?): CfirFunction {
        return transformFunctionStatus(function)
    }

    override fun transformMainFunction(mainFunction: CfirMainFunction, data: Nothing?): CfirMainFunction {
        return transformFunctionStatus(mainFunction) as CfirMainFunction
    }

    override fun transformMacroDeclaration(macroDeclaration: CfirMacroDeclaration, data: Nothing?): CfirMacroDeclaration {
        return transformFunctionStatus(macroDeclaration) as CfirMacroDeclaration
    }

    override fun transformFinalizer(finalizer: CfirFinalizer, data: Nothing?): CfirFinalizer {
        return transformFunctionStatus(finalizer) as CfirFinalizer
    }

    /**
     * 处理仓颉特有的 `CfirFunction` 直接子类。
     *
     * Kotlin FIR 的普通函数统一落在 `FirNamedFunction`，而仓颉 CFIR 还存在 main/macro/finalizer 等
     * 非 `CfirNamedFunction` 的函数节点；这些节点没有 overridden callable 语义，但仍必须发布 resolved status。
     */
    fun transformFunctionStatus(function: CfirFunction): CfirFunction {
        return withResolvedStatusPhase(function) {
            transformFunctionStatusWithoutPhaseGuard(function)
        }
    }

    /**
     * 只计算并发布函数自身 STATUS。
     *
     * 仓颉存在 `main` / macro / finalizer 等非 `CfirNamedFunction` 的函数节点；LL STATUS resolver
     * 需要像 Kotlin 的 named function 专门路径一样，在写锁中直接调用该入口。
     */
    fun transformFunctionStatusWithoutPhaseGuard(function: CfirFunction) {
        function.replaceStatus(statusResolver.resolveStatus(function, containingClass, isLocal = false))
        function.valueParameters.forEach(::transformValueParameterStatusWithoutPhaseGuard)
    }

    override fun transformNamedFunction(namedFunction: CfirNamedFunction, data: Nothing?): CfirNamedFunction {
        return withResolvedStatusPhase(namedFunction) {
            val overriddenFunctions = statusResolver.getOverriddenFunctions(namedFunction, containingClass)
            transformNamedFunction(namedFunction, overriddenFunctions)
        }
    }

    fun transformNamedFunction(
        namedFunction: CfirNamedFunction,
        overriddenFunctions: List<CfirNamedFunction>,
    ) {
        val overriddenStatuses = overriddenFunctions.mapNotNull { it.status as? CfirResolvedDeclarationStatus }
        namedFunction.replaceStatus(
            statusResolver.resolveStatus(
                namedFunction,
                containingClass,
                isLocal = false,
                overriddenStatuses = overriddenStatuses,
            ),
        )
        namedFunction.valueParameters.forEach(::transformValueParameterStatusWithoutPhaseGuard)
    }

    override fun transformConstructor(constructor: CfirConstructor, data: Nothing?): CfirConstructor {
        return withResolvedStatusPhase(constructor) {
            constructor.replaceStatus(statusResolver.resolveStatus(constructor, containingClass, isLocal = false))
            constructor.valueParameters.forEach(::transformValueParameterStatusWithoutPhaseGuard)
        }
    }

    override fun transformEnumConstructor(enumConstructor: CfirEnumConstructor, data: Nothing?): CfirEnumConstructor {
        return withResolvedStatusPhase(enumConstructor) {
            enumConstructor.replaceStatus(statusResolver.resolveStatus(enumConstructor, containingClass, isLocal = false))
            enumConstructor.valueParameters.forEach(::transformValueParameterStatusWithoutPhaseGuard)
        }
    }

    override fun transformProperty(property: CfirProperty, data: Nothing?): CfirProperty {
        return withResolvedStatusPhase(property) {
            val overriddenProperties = statusResolver.getOverriddenProperties(property, containingClass)
            transformProperty(property, overriddenProperties)
        }
    }

    fun transformProperty(
        property: CfirProperty,
        overriddenProperties: List<CfirProperty>,
    ) {
        val overriddenStatuses = overriddenProperties.mapNotNull { it.status as? CfirResolvedDeclarationStatus }
        val overriddenSetters = overriddenProperties.mapNotNull { overriddenProperty ->
            overriddenProperty.setter?.status as? CfirResolvedDeclarationStatus
        }

        property.replaceStatus(
            statusResolver.resolveStatus(
                property,
                containingClass,
                isLocal = false,
                overriddenStatuses = overriddenStatuses,
            ),
        )

        property.getter?.let { transformPropertyAccessor(it, property) }
        property.setter?.let { transformPropertyAccessor(it, property, overriddenSetters) }
    }

    protected fun transformPropertyAccessor(
        propertyAccessor: CfirPropertyAccessor,
        containingProperty: CfirProperty,
        overriddenStatuses: List<CfirResolvedDeclarationStatus> = emptyList(),
    ) {
        propertyAccessor.replaceStatus(
            statusResolver.resolveStatus(
                propertyAccessor,
                containingClass,
                containingProperty,
                isLocal = false,
                overriddenStatuses = overriddenStatuses,
            ),
        )
        propertyAccessor.valueParameters.forEach(::transformValueParameterStatusWithoutPhaseGuard)
    }

    override fun transformPropertyAccessor(propertyAccessor: CfirPropertyAccessor, data: Nothing?): CfirPropertyAccessor {
        transformProperty(propertyAccessor.propertySymbol.cfir, data)
        return propertyAccessor
    }

    override fun transformFieldVariable(fieldVariable: CfirFieldVariable, data: Nothing?): CfirFieldVariable {
        return withResolvedStatusPhase(fieldVariable) {
            transformVariableStatusWithoutPhaseGuard(fieldVariable)
        }
    }

    override fun transformPatternBindingVariable(
        patternBindingVariable: CfirPatternBindingVariable,
        data: Nothing?,
    ): CfirPatternBindingVariable {
        return withResolvedStatusPhase(patternBindingVariable) {
            transformVariableStatusWithoutPhaseGuard(patternBindingVariable)
        }
    }

    override fun transformPatternVariable(
        patternVariable: CfirPatternVariable,
        data: Nothing?,
    ): CfirPatternVariable {
        return withResolvedStatusPhase(patternVariable) {
            transformVariableStatusWithoutPhaseGuard(patternVariable)
        }
    }

    override fun transformValueParameter(valueParameter: CfirValueParameter, data: Nothing?): CfirValueParameter {
        return withResolvedStatusPhase(valueParameter) {}
    }

    /**
     * 只发布 value parameter 的 STATUS。
     *
     * LL STATUS resolver 的 callable 专门路径在写锁中直接调用 callable helper；
     * 参数不能再回到 generic phase guard，否则会因为宿主 callable 已推进 phase 而跳过 status 发布。
     */
    fun transformValueParameterStatusWithoutPhaseGuard(valueParameter: CfirValueParameter) {
        valueParameter.publishResolvedStatusIfNeeded()
        valueParameter.replaceResolvePhase(CfirResolvePhase.STATUS)
        statusComputationSession.endComputing(valueParameter)
    }

    /**
     * 只计算并发布变量自身 STATUS。
     *
     * 仓颉的 pattern variable / pattern binding variable 是可被符号恢复直接消费的
     * `CfirVariable`，但 Kotlin FIR 没有同名节点；它们在 STATUS 阶段对位 Kotlin
     * 普通 callable 的“仅发布自身 status”路径，不能依赖通用 declaration fallback。
     */
    fun transformVariableStatusWithoutPhaseGuard(variable: CfirVariable) {
        variable.replaceStatus(statusResolver.resolveStatus(variable, containingClass, isLocal = false))
    }

    override fun transformTypeParameter(typeParameter: CfirTypeParameter, data: Nothing?): CfirTypeParameter {
        if (typeParameter.resolvePhase < CfirResolvePhase.TYPES || typeParameter.resolvePhase >= CfirResolvePhase.STATUS) {
            return typeParameter
        }
        typeParameter.transformBounds(this, null)
        typeParameter.replaceResolvePhase(CfirResolvePhase.STATUS)
        return typeParameter
    }
}

open class CfirStatusResolveTransformer(
    statusComputationSession: CfirStatusComputationSession,
) : AbstractCfirStatusResolveTransformer(
    statusComputationSession = statusComputationSession,
) 

private fun CfirMemberDeclaration.publishResolvedStatusIfNeeded() {
    val currentStatus = status
    if (currentStatus is CfirResolvedDeclarationStatus) return

    if (this is CfirValueParameter) {
        // value parameter 仍沿用 statusless declaration 约定。
        replaceStatus(currentStatus.resolvedForStatuslessDeclaration())
        return
    }

    val currentModality = currentStatus.modality
        ?: error("Status modality must be initialized before publishing STATUS for ${this::class.simpleName}")

    replaceStatus(
        buildResolvedDeclarationStatus {
            source = currentStatus.source
            visibility = currentStatus.visibility
            isVisibilityExplicit = currentStatus.isVisibilityExplicit
            isModalityExplicit = currentStatus.isModalityExplicit
            isOverride = currentStatus.isOverride
            isOperator = currentStatus.isOperator
            isStatic = currentStatus.isStatic
            isConst = currentStatus.isConst
            isMut = currentStatus.isMut
            isUnsafe = currentStatus.isUnsafe
            isForeign = currentStatus.isForeign
            isCommon = currentStatus.isCommon
            isSpecific = currentStatus.isSpecific
            isRedef = currentStatus.isRedef
            isAbstract = currentStatus.isAbstract
            isOpen = currentStatus.isOpen
            isSealed = currentStatus.isSealed
            modality = currentModality
        }
    )
}

/**
 * 仓颉 STATUS 主干对位 Kotlin `FirStatusResolver` 的最小同构实现。
 *
 * 当前仓颉 tree 还没有 Kotlin 那种 `Unknown visibility/modality` raw 状态，因此这里按
 * `isVisibilityExplicit / isModalityExplicit` 重新解释 raw status，把真正的默认决策放回 STATUS 阶段。
 */
class CfirStatusResolver(
    private val session: CfirSession,
    private val scopeSession: ScopeSession,
) {
    fun getOverriddenProperties(
        property: CfirProperty,
        containingClass: CfirClass?,
    ): List<CfirProperty> {
        val scope = containingClass?.unsubstitutedScope(
            useSiteSession = session,
            scopeSession = scopeSession,
            withForcedTypeCalculator = false,
            memberRequiredPhase = null,
        ) ?: return emptyList()

        val result = linkedSetOf<CfirProperty>()
        scope.processDirectOverriddenPropertiesWithBaseScope(property.symbol as CfirPropertySymbol) { overriddenSymbol, _ ->
            result += overriddenSymbol.cfir
            ProcessorAction.NEXT
        }
        return result.toList()
    }

    fun getOverriddenFunctions(
        function: CfirNamedFunction,
        containingClass: CfirClass?,
    ): List<CfirNamedFunction> {
        val scope = containingClass?.unsubstitutedScope(
            useSiteSession = session,
            scopeSession = scopeSession,
            withForcedTypeCalculator = false,
            memberRequiredPhase = null,
        ) ?: return emptyList()

        val result = linkedSetOf<CfirNamedFunction>()
        scope.processDirectOverriddenFunctionsWithBaseScope(function.symbol) { overriddenSymbol, _ ->
            result += overriddenSymbol.cfir
            ProcessorAction.NEXT
        }
        return result.toList()
    }

    fun resolveStatus(
        declaration: CfirClass,
        containingClass: CfirClass?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    fun resolveStatus(
        declaration: CfirTypeAlias,
        containingClass: CfirClass?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    fun resolveStatus(
        declaration: CfirFunction,
        containingClass: CfirClass?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    fun resolveStatus(
        declaration: CfirNamedFunction,
        containingClass: CfirClass?,
        isLocal: Boolean,
        overriddenStatuses: List<CfirResolvedDeclarationStatus>,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
            overriddenStatuses = overriddenStatuses,
        )
    }

    fun resolveStatus(
        declaration: CfirConstructor,
        containingClass: CfirClass?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    fun resolveStatus(
        declaration: CfirEnumConstructor,
        containingClass: CfirClass?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    fun resolveStatus(
        declaration: CfirProperty,
        containingClass: CfirClass?,
        isLocal: Boolean,
        overriddenStatuses: List<CfirResolvedDeclarationStatus>,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
            overriddenStatuses = overriddenStatuses,
        )
    }

    fun resolveStatus(
        declaration: CfirPropertyAccessor,
        containingClass: CfirClass?,
        containingProperty: CfirProperty?,
        isLocal: Boolean,
        overriddenStatuses: List<CfirResolvedDeclarationStatus>,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = containingProperty,
            isLocal = isLocal,
            overriddenStatuses = overriddenStatuses,
        )
    }

    fun resolveStatus(
        declaration: CfirVariable,
        containingClass: CfirClass?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    fun resolveStatus(
        declaration: CfirExtend,
        containingClass: CfirClass?,
        isLocal: Boolean,
    ): CfirResolvedDeclarationStatus {
        return resolveStatus(
            declaration = declaration,
            status = declaration.status,
            containingClass = containingClass,
            containingProperty = null,
            isLocal = isLocal,
        )
    }

    private fun resolveStatus(
        declaration: CfirDeclaration,
        status: CfirDeclarationStatus,
        containingClass: CfirClass?,
        containingProperty: CfirProperty?,
        isLocal: Boolean,
        overriddenStatuses: List<CfirResolvedDeclarationStatus> = emptyList(),
    ): CfirResolvedDeclarationStatus {
        if (status is CfirResolvedDeclarationStatus) return status

        val visibility = if (status.isVisibilityExplicit) {
            status.visibility
        } else {
            resolveVisibility(declaration, containingProperty, overriddenStatuses, isLocal)
        }

        val modality = if (status.isModalityExplicit) {
            status.modality ?: resolveModality(declaration, containingProperty, containingClass)
        } else {
            resolveModality(declaration, containingProperty, containingClass)
        }

        return buildResolvedDeclarationStatus {
            source = status.source
            this.visibility = visibility
            isVisibilityExplicit = status.isVisibilityExplicit
            isModalityExplicit = status.isModalityExplicit
            isOverride = status.isOverride
            isOperator = status.isOperator
            isStatic = status.isStatic
            isConst = status.isConst
            isMut = status.isMut
            isUnsafe = status.isUnsafe
            isForeign = status.isForeign
            isCommon = status.isCommon
            isSpecific = status.isSpecific
            isRedef = status.isRedef
            isAbstract = status.isAbstract
            isOpen = status.isOpen
            isSealed = status.isSealed
            this.modality = modality
        }
    }

    private fun resolveVisibility(
        declaration: CfirDeclaration,
        containingProperty: CfirProperty?,
        overriddenStatuses: List<CfirResolvedDeclarationStatus>,
        isLocal: Boolean,
    ) = when {
        isLocal -> Visibilities.Local
        declaration is CfirPropertyAccessor && containingProperty != null -> containingProperty.status.visibility
        overriddenStatuses.isNotEmpty() -> {
            overriddenStatuses.map { it.visibility }
                .maxWithOrNull { left, right -> Visibilities.compare(left, right) ?: 0 }
                ?: Visibilities.Public
        }

        else -> declaration.statusOrNull()?.visibility ?: Visibilities.Public
    }

    private fun resolveModality(
        declaration: CfirDeclaration,
        containingProperty: CfirProperty?,
        containingClass: CfirClass?,
    ): Modality {
        return when (declaration) {
            is CfirClass -> if (declaration is CfirInterface) Modality.ABSTRACT else Modality.FINAL
            is CfirCallableDeclaration -> {
                val containingPropertyModality = containingProperty?.status?.modality
                when {
                    containingClass == null -> Modality.FINAL
                    declaration is CfirPropertyAccessor && containingPropertyModality != null -> containingPropertyModality
                    containingClass is CfirInterface -> {
                        when {
                            declaration.status.visibility == Visibilities.Private -> Modality.FINAL
                            !declaration.hasOwnBodyOrAccessorBody() -> Modality.ABSTRACT
                            else -> Modality.OPEN
                        }
                    }

                    declaration.status.isOverride -> Modality.OPEN
                    else -> Modality.FINAL
                }
            }

            else -> Modality.FINAL
        }
    }
}

private fun CfirDeclaration.statusOrNull(): CfirDeclarationStatus? {
    return when (this) {
        is CfirClass -> status
        is CfirFunction -> status
        is CfirProperty -> status
        is CfirEnumConstructor -> status
        is CfirVariable -> status
        is CfirExtend -> status
        is CfirTypeAlias -> status
        else -> null
    }
}

private fun CfirDeclaration.hasOwnBodyOrAccessorBody(): Boolean {
    return when (this) {
        is CfirFunction -> body != null
        is CfirProperty -> getter?.body != null || setter?.body != null
        else -> true
    }
}
