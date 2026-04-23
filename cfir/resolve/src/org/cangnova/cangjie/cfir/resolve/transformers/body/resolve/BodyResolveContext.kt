package org.cangnova.cangjie.cfir.resolve.transformers.body.resolve

import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.calls.InaccessibleImplicitReceiverValue
import org.cangnova.cangjie.cfir.calls.ImplicitReceiverValue
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.InaccessibleReceiverKind
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.ImplicitValueStorage
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataContext
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataElement
import org.cangnova.cangjie.cfir.resolve.body.collectTowerDataElementsForClass
import org.cangnova.cangjie.cfir.resolve.body.typeParametersForTower
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirTypeParameterScopeImpl
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.util.PrivateForInline
import java.util.IdentityHashMap
import java.util.EnumMap

/**
 * Body resolve 阶段的中心上下文。
 *
 * 对齐 Kotlin K2 `BodyResolveContext`（`compiler/fir/resolve/.../body/resolve/BodyResolveContext.kt`），
 * 负责作用域栈、容器栈、推断会话、tower data mode 切换等状态。仓颉语义的裁剪见
 * `plans/kotlin-bodyresolvecontext-firlocalscope-noble-flute.md`（不引入 companion / backing field /
 * script / REPL / context parameter 等 Kotlin 特有概念）。
 */
class BodyResolveContext(
    @set:PrivateForInline
    var returnTypeCalculator: ReturnTypeCalculator,
    val dataFlowAnalyzerContext: CfirDataFlowAnalyzerContext,
    private val isContextCollectorMode: Boolean = false,
) {
    // ── Imports & file ──────────────────────────────────────────────────────

    val fileImportsScope: MutableList<CfirScope> = mutableListOf()

    @set:PrivateForInline
    lateinit var file: CfirFile

    // ── Tower data contexts ────────────────────────────────────────────────

    @PrivateForInline
    var regularTowerDataContexts: CfirRegularTowerDataContexts =
        CfirRegularTowerDataContexts(regular = CfirTowerDataContext())

    @PrivateForInline
    val specialTowerDataContexts: CfirSpecialTowerDataContexts = CfirSpecialTowerDataContexts()

    @OptIn(PrivateForInline::class)
    val towerDataContext: CfirTowerDataContext
        get() = regularTowerDataContexts.currentContext
            ?: throw AssertionError("No regular data context found, towerDataMode = $towerDataMode")

    val implicitValueStorage: ImplicitValueStorage
        get() = towerDataContext.implicitValueStorage

    @OptIn(PrivateForInline::class)
    var towerDataMode: CfirTowerDataMode
        get() = regularTowerDataContexts.activeMode
        set(value) {
            regularTowerDataContexts = regularTowerDataContexts.replaceTowerDataMode(value)
        }

    // ── Containers & inference state ───────────────────────────────────────

    @set:PrivateForInline
    var containers: ArrayDeque<CfirDeclaration> = ArrayDeque()

    val containerIfAny: CfirDeclaration?
        get() = containers.lastOrNull()

    @set:PrivateForInline
    var containingRegularClass: CfirClass? = null

    @set:PrivateForInline
    var inferenceSession: CfirInferenceSession = CfirInferenceSession.DEFAULT

    @set:PrivateForInline
    var isInsideAssignmentRhs: Boolean = false

    @set:PrivateForInline
    var publicApiInlineFunction: CfirFunction? = null

    @set:PrivateForInline
    var containingClassDeclarations: ArrayDeque<CfirClass> = ArrayDeque()

    @set:PrivateForInline
    var targetedLocalClasses: Set<CfirDeclaration> = emptySet()

    /** 对齐 K2：当前正在按依赖上下文分析的匿名函数集合。 */
    val anonymousFunctionsAnalyzedInDependentContext: MutableSet<CfirFunctionSymbol<*>> = mutableSetOf()

    /** 对齐 K2：嵌套的本地 class-like 声明追溯外层所属。 */
    val outerLocalClassForNested: MutableMap<CfirClassLikeSymbol<*>, CfirClassLikeSymbol<*>> = hashMapOf()

    @set:PrivateForInline
    var isInsideAnnotationContext: Boolean = false

    // ── File entry ─────────────────────────────────────────────────────────

    @OptIn(PrivateForInline::class)
    inline fun <T> withFile(file: CfirFile, f: () -> T): T {
        val oldFile = if (::file.isInitialized) this.file else null
        val oldImports = fileImportsScope.toList()
        this.file = file
        fileImportsScope.clear()
        return try {
            withContainer(file, f)
        } finally {
            fileImportsScope.clear()
            fileImportsScope.addAll(oldImports)
            if (oldFile != null) {
                this.file = oldFile
            }
        }
    }

    // ── Storage: variables / functions / class-likes ──────────────────────

    @OptIn(PrivateForInline::class)
    fun storeVariable(variable: CfirVariable, session: CfirSession) {
        replaceTowerDataContext(towerDataContext.addLocalVariable(variable, session))
    }

    fun storeValueParameterIfNeeded(valueParameter: CfirValueParameter, session: CfirSession) {
        if (valueParameter.name != UNDERSCORE_FOR_UNUSED_VAR) {
            storeVariable(valueParameter, session)
        }
    }

    @OptIn(PrivateForInline::class)
    fun storeFunction(function: CfirNamedFunction, session: CfirSession) {
        val lastScope = towerDataContext.localScopes.lastOrNull() ?: return
        val newLastScope = lastScope.storeFunction(function, session)
        replaceTowerDataContext(towerDataContext.setLastLocalScope(newLastScope))
    }

    @OptIn(PrivateForInline::class)
    fun storeClassOrTypealiasIfNotNested(classLike: CfirClassLikeDeclaration, session: CfirSession) {
        // 嵌套类/类型别名不进局部作用域，由外层 class scope 承载。
        if (containerIfAny is CfirClass) return
        val lastScope = towerDataContext.localScopes.lastOrNull() ?: return
        val newLastScope = lastScope.storeClassOrTypeAlias(classLike, session)
        replaceTowerDataContext(towerDataContext.setLastLocalScope(newLastScope))
    }

    // ── Tower data mutation primitives ────────────────────────────────────

    @OptIn(PrivateForInline::class)
    fun replaceTowerDataContext(newContext: CfirTowerDataContext) {
        regularTowerDataContexts = regularTowerDataContexts.replaceCurrentlyActiveContext(newContext)
    }

    @OptIn(PrivateForInline::class)
    fun clear() {
        specialTowerDataContexts.clear()
        fileImportsScope.clear()
        dataFlowAnalyzerContext.reset()
    }

    fun addNonLocalScope(scope: CfirScope) {
        addNonLocalTowerDataElement(scope.asTowerDataElement(isLocal = false))
    }

    fun addNonLocalScopes(scopes: List<CfirScope>) {
        if (scopes.isEmpty()) return
        addNonLocalTowerDataElements(scopes.map { it.asTowerDataElement(isLocal = false) })
    }

    fun addNonLocalScopeIfNotNull(scope: CfirScope?) {
        if (scope == null) return
        addNonLocalScope(scope)
    }

    fun addNonLocalTowerDataElement(element: CfirTowerDataElement) {
        replaceTowerDataContext(towerDataContext.addNonLocalTowerDataElements(listOf(element)))
    }

    fun addNonLocalTowerDataElements(newElements: List<CfirTowerDataElement>) {
        replaceTowerDataContext(towerDataContext.addNonLocalTowerDataElements(newElements))
    }

    fun addLocalScope(localScope: CfirLocalScope) {
        replaceTowerDataContext(towerDataContext.addLocalScope(localScope))
    }

    fun addReceiver(name: Name?, implicitReceiverValue: ImplicitReceiverValue<*>) {
        replaceTowerDataContext(towerDataContext.addReceiver(name, implicitReceiverValue))
    }

    fun addReceiverIfNotNull(name: Name?, implicitReceiverValue: ImplicitReceiverValue<*>?) {
        if (implicitReceiverValue == null) return
        addReceiver(name, implicitReceiverValue)
    }

    // ── Container stack ───────────────────────────────────────────────────

    @OptIn(PrivateForInline::class)
    inline fun <T> withContainer(declaration: CfirDeclaration, f: () -> T): T {
        val oldContainingRegularClass = containingRegularClass
        containers.addLast(declaration)
        if (declaration is CfirClass) {
            containingRegularClass = declaration
        }
        return try {
            f()
        } finally {
            containers.removeLast()
            containingRegularClass = oldContainingRegularClass
        }
    }

    inline fun <T> withContainingClass(declaration: CfirClass, f: () -> T): T {
        containingClassDeclarations.addLast(declaration)
        return try {
            f()
        } finally {
            containingClassDeclarations.removeLast()
        }
    }

    // ── Tower data with/cleanup combinators ───────────────────────────────

    @OptIn(PrivateForInline::class)
    inline fun <T> withTowerDataContexts(newContexts: CfirRegularTowerDataContexts, f: () -> T): T {
        val old = regularTowerDataContexts
        regularTowerDataContexts = newContexts
        return try {
            f()
        } finally {
            regularTowerDataContexts = old
        }
    }

    inline fun <T> withTowerDataContext(newContext: CfirTowerDataContext, f: () -> T): T {
        val initialContext = towerDataContext
        return try {
            replaceTowerDataContext(newContext)
            f()
        } finally {
            replaceTowerDataContext(initialContext)
        }
    }

    inline fun <R> withTowerDataCleanup(l: () -> R): R {
        val initialContext = towerDataContext
        return try {
            l()
        } finally {
            replaceTowerDataContext(initialContext)
        }
    }

    inline fun <T> withTowerDataMode(mode: CfirTowerDataMode?, f: () -> T): T {
        val oldMode = towerDataMode
        if (mode != null) {
            towerDataMode = mode
        }
        return try {
            f()
        } finally {
            towerDataMode = oldMode
        }
    }

    inline fun <R> withTowerDataModeCleanup(l: () -> R): R {
        val initialMode = towerDataMode
        return try {
            l()
        } finally {
            towerDataMode = initialMode
        }
    }

    // ── Block / match-branch entry ────────────────────────────────────────

    /** 进入一个普通代码块或 match 分支体，引入空局部作用域。 */
    inline fun <T> forBlock(session: CfirSession, f: () -> T): T {
        return withTowerDataCleanup {
            addLocalScope(CfirLocalScope(session))
            f()
        }
    }

    // ── Class body ────────────────────────────────────────────────────────

    fun <T> withScopesForClass(
        owner: CfirClassLikeDeclaration,
        holder: SessionAndScopeSessionHolder,
        f: () -> T,
    ): T {
        val ownerSymbol = owner.symbol as? CfirClassLikeSymbol<*>
            ?: return f()
        val ownerType = ownerSymbol.constructType()
        val towerElementsForClass = holder.collectTowerDataElementsForClass(owner, ownerType)

        val base = towerDataContext.addNonLocalTowerDataElements(towerElementsForClass.superClassesStaticScopes)
        val statics = base.addNonLocalScopeIfNotNull(towerElementsForClass.staticScope)
        val typeParameterScope = owner.typeParametersForTower()
            .takeIf { it.isNotEmpty() }
            ?.let(::CfirTypeParameterScopeImpl)

        val withTypeParameters = if (typeParameterScope != null) {
            towerDataContext
                .addNonLocalTowerDataElements(towerElementsForClass.superClassesStaticScopes)
                .addNonLocalScopeIfNotNull(towerElementsForClass.staticScope)
                .addNonLocalScope(typeParameterScope)
        } else {
            statics
        }

        val forMembersResolution = withTypeParameters
            .addReceiver(null, towerElementsForClass.thisReceiver)

        val forNestedClasses = statics

        val constructor = (owner as? CfirClass)?.declarations?.firstOrNull { it is CfirConstructor } as? CfirConstructor
        val (primaryConstructorPureParametersScope, primaryConstructorAllParametersScope) =
            if (constructor?.isPrimary == true) {
                val scope = buildConstructorParametersScope(constructor, holder.session)
                scope to scope
            } else {
                null to null
            }

        val forConstructorHeaders = if (!isContextCollectorMode) {
            val inaccessibleThisInHeader = InaccessibleImplicitReceiverValue(
                ownerSymbol,
                ownerType,
                InaccessibleReceiverKind.CLASS_HEADER,
                holder.session,
                holder.scopeSession,
            )

            towerDataContext
                .addReceiver(null, inaccessibleThisInHeader)
                .addNonLocalTowerDataElements(towerElementsForClass.superClassesStaticScopes)
                .addNonLocalScopeIfNotNull(towerElementsForClass.staticScope)
                .addNonLocalScopeIfNotNull(typeParameterScope)
        } else {
            withTypeParameters
        }

        val forEnumConstructors = if (owner is CfirEnum && !isContextCollectorMode) {
            val inaccessibleReceiver = InaccessibleImplicitReceiverValue(
                ownerSymbol,
                ownerType,
                InaccessibleReceiverKind.ENUM_CONSTRUCTOR,
                holder.session,
                holder.scopeSession,
            )

            towerDataContext
                .addReceiver(null, inaccessibleReceiver)
                .addNonLocalTowerDataElements(towerElementsForClass.superClassesStaticScopes)
                .addNonLocalScopeIfNotNull(towerElementsForClass.staticScope)
                .addNonLocalScopeIfNotNull(typeParameterScope)
        } else {
            withTypeParameters
        }

        val newContexts = CfirRegularTowerDataContexts(
            regular = forMembersResolution,
            forNestedClasses = forNestedClasses,
            forStaticMembers = statics,
            forConstructorHeaders = forConstructorHeaders,
            forEnumConstructors = forEnumConstructors,
            primaryConstructorPureParametersScope = primaryConstructorPureParametersScope,
            primaryConstructorAllParametersScope = primaryConstructorAllParametersScope,
        )

        return withTowerDataContexts(newContexts, f)
    }

    fun getPrimaryConstructorPureParametersScope(): CfirLocalScope? {
        @OptIn(PrivateForInline::class)
        return regularTowerDataContexts.primaryConstructorPureParametersScope
    }

    fun getPrimaryConstructorAllParametersScope(): CfirLocalScope? {
        @OptIn(PrivateForInline::class)
        return regularTowerDataContexts.primaryConstructorAllParametersScope
    }

    // ── Function body / lambda ────────────────────────────────────────────

    /** 对齐 K2 `withNamedFunction`：非 class 成员时把函数注册到当前局部作用域。 */
    @OptIn(PrivateForInline::class)
    inline fun <T> withNamedFunction(namedFunction: CfirNamedFunction, session: CfirSession, f: () -> T): T {
        if (containerIfAny !is CfirClass) {
            storeFunction(namedFunction, session)
        }
        return withContainer(namedFunction, f)
    }

    /**
     * 对齐 K2 `forFunctionBody`：开一层局部作用域并把每个参数注册进去。
     * 仓颉没有 context parameter / receiver parameter 独立声明，逻辑相应简化。
     */
    @OptIn(PrivateForInline::class)
    inline fun <T> forFunctionBody(
        function: CfirFunction,
        holder: SessionAndScopeSessionHolder,
        f: () -> T,
    ): T = withTowerDataCleanup {
        addLocalScope(CfirLocalScope(holder.session))
        for (parameter in function.valueParameters) {
            storeVariable(parameter, holder.session)
        }
        f()
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withValueParameter(
        valueParameter: CfirValueParameter,
        session: CfirSession,
        f: () -> T,
    ): T {
        storeValueParameterIfNeeded(valueParameter, session)
        return withContainer(valueParameter) {
            f()
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withAnonymousFunction(
        anonymousFunction: CfirFunction,
        holder: SessionAndScopeSessionHolder,
        f: () -> T,
    ): T = withTowerDataCleanup {
        addLocalScope(CfirLocalScope(holder.session))
        for (parameter in anonymousFunction.valueParameters) {
            storeValueParameterIfNeeded(parameter, holder.session)
        }
        withContainer(anonymousFunction, f)
    }

    inline fun <R> withLambdaBeingAnalyzedInDependentContext(
        lambda: CfirFunctionSymbol<*>,
        l: () -> R,
    ): R {
        anonymousFunctionsAnalyzedInDependentContext.add(lambda)
        return try {
            l()
        } finally {
            anonymousFunctionsAnalyzedInDependentContext.remove(lambda)
        }
    }

    // ── Constructor ───────────────────────────────────────────────────────

    @OptIn(PrivateForInline::class)
    inline fun <T> forConstructor(constructor: CfirConstructor, f: () -> T): T =
        withTowerDataMode(CfirTowerDataMode.CONSTRUCTOR_HEADER) {
            withContainer(constructor, f)
        }

    @OptIn(PrivateForInline::class)
    inline fun <T> forConstructorParameters(
        constructor: CfirConstructor,
        owningClass: CfirClassLikeDeclaration?,
        holder: SessionAndScopeSessionHolder,
        f: () -> T,
    ): T {
        // 构造器默认值不能访问构造中类的成员；
        // 由 checker 在发现前一个参数被后一个参数引用且未初始化时报错。
        return forConstructorParametersOrDelegatedConstructorCallChildren(constructor, owningClass, holder, f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> forDelegatedConstructorCallChildren(
        constructor: CfirConstructor,
        owningClass: CfirClassLikeDeclaration?,
        holder: SessionAndScopeSessionHolder,
        f: () -> T,
    ): T = forConstructorParametersOrDelegatedConstructorCallChildren(constructor, owningClass, holder, f)

    /**
     * 委托构造调用自身的参数解析：在构造器体 tower data 模式下允许 `this` 隐式接收者参与（用于 inner super call）。
     */
    @OptIn(PrivateForInline::class)
    inline fun <T> forDelegatedConstructorCallResolution(f: () -> T): T {
        require(towerDataMode == CfirTowerDataMode.CONSTRUCTOR_HEADER) {
            "forDelegatedConstructorCallResolution must be nested inside forConstructor"
        }
        return withTowerDataMode(CfirTowerDataMode.REGULAR) { f() }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> forConstructorParametersOrDelegatedConstructorCallChildren(
        constructor: CfirConstructor,
        @Suppress("UNUSED_PARAMETER") owningClass: CfirClassLikeDeclaration?,
        holder: SessionAndScopeSessionHolder,
        f: () -> T,
    ): T {
        require(towerDataMode == CfirTowerDataMode.CONSTRUCTOR_HEADER) {
            "forConstructorParameters must be nested inside forConstructor"
        }
        return withTowerDataCleanup {
            addLocalScope(buildConstructorParametersScope(constructor, holder.session))
            f()
        }
    }

    /**
     * 对齐 K2 `forConstructorBody`。仓颉主/次构造器体的区别：
     *  - primary：沿用 CONSTRUCTOR_HEADER tower data（无 `this` 隐式接收者），仅把参数注入局部作用域；
     *  - secondary：切回 REGULAR tower data，允许访问当前类成员。
     */
    @OptIn(PrivateForInline::class)
    inline fun <T> forConstructorBody(
        constructor: CfirConstructor,
        session: CfirSession,
        f: () -> T,
    ): T {
        require(towerDataMode == CfirTowerDataMode.CONSTRUCTOR_HEADER) {
            "forConstructorBody must be nested inside forConstructor"
        }
        return if (constructor.isPrimary) {
            withTowerDataCleanup {
                addLocalScope(buildConstructorParametersScope(constructor, session))
                f()
            }
        } else {
            withTowerDataMode(CfirTowerDataMode.REGULAR) {
                withTowerDataCleanup {
                    addLocalScope(buildConstructorParametersScope(constructor, session))
                    f()
                }
            }
        }
    }

    fun buildConstructorParametersScope(
        constructor: CfirConstructor,
        session: CfirSession,
    ): CfirLocalScope =
        constructor.valueParameters.fold(CfirLocalScope(session)) { scope, parameter ->
            scope.storeVariable(parameter, session)
        }

    // ── Annotation class context ──────────────────────────────────────────

    /**
     * 仓颉 annotation class 的 primary constructor 上下文：
     * 参数默认值必须是常量表达式，且不能访问构造类的实例成员。
     * 目前以标志位 + CONSTRUCTOR_HEADER tower data 表达约束，具体语义由 checker 细化。
     */
    @OptIn(PrivateForInline::class)
    inline fun <T> withAnnotationContext(f: () -> T): T {
        val old = isInsideAnnotationContext
        isInsideAnnotationContext = true
        return try {
            withTowerDataMode(CfirTowerDataMode.CONSTRUCTOR_HEADER, f)
        } finally {
            isInsideAnnotationContext = old
        }
    }

    // ── Lambda / callable reference postponed contexts ────────────────────

    @OptIn(PrivateForInline::class)
    inline fun <T> withAnonymousFunctionTowerDataContext(symbol: CfirFunctionSymbol<*>, f: () -> T): T {
        return withTemporaryRegularContext(specialTowerDataContexts.getAnonymousFunctionContext(symbol), f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withCallableReferenceTowerDataContext(access: CfirExpression, f: () -> T): T {
        return withTemporaryRegularContext(specialTowerDataContexts.getCallableReferenceContext(access), f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withTemporaryRegularContext(newContext: CfirPostponedAtomsResolutionContext?, f: () -> T): T {
        val context = newContext ?: return f()
        return withTowerDataContexts(regularTowerDataContexts.replaceAndSetActiveRegularContext(context.towerDataContext)) {
            if (context.inferenceSession !== inferenceSession) {
                withInferenceSession(context.inferenceSession) { f() }
            } else {
                f()
            }
        }
    }

    @OptIn(PrivateForInline::class)
    fun storeContextForAnonymousFunction(anonymousFunction: CfirFunction) {
        val symbol = anonymousFunction.symbol as? CfirFunctionSymbol<*> ?: return
        specialTowerDataContexts.storeAnonymousFunctionContext(symbol, towerDataContext, inferenceSession)
    }

    @OptIn(PrivateForInline::class)
    fun dropContextForAnonymousFunction(anonymousFunction: CfirFunction) {
        val symbol = anonymousFunction.symbol as? CfirFunctionSymbol<*> ?: return
        specialTowerDataContexts.dropAnonymousFunctionContext(symbol)
    }

    @OptIn(PrivateForInline::class)
    fun storeCallableReferenceContext(callableReferenceAccess: CfirExpression) {
        specialTowerDataContexts.storeCallableReferenceContext(
            callableReferenceAccess,
            towerDataContext,
            inferenceSession,
        )
    }

    @OptIn(PrivateForInline::class)
    fun dropCallableReferenceContext(callableReferenceAccess: CfirExpression) {
        specialTowerDataContexts.dropCallableReferenceContext(callableReferenceAccess)
    }

    // ── Inference / expectations ──────────────────────────────────────────

    @OptIn(PrivateForInline::class)
    inline fun <T> withInferenceSession(
        inferenceSession: CfirInferenceSession,
        block: CfirInferenceSession.() -> T,
    ): T {
        val oldSession = this.inferenceSession
        this.inferenceSession = inferenceSession
        return try {
            inferenceSession.block()
        } finally {
            this.inferenceSession = oldSession
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <R> withAssignmentRhs(block: () -> R): R {
        val old = isInsideAssignmentRhs
        isInsideAssignmentRhs = true
        return try {
            block()
        } finally {
            isInsideAssignmentRhs = old
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withPublicApiInlineFunction(function: CfirFunction?, block: () -> T): T {
        val old = publicApiInlineFunction
        publicApiInlineFunction = function
        return try {
            block()
        } finally {
            publicApiInlineFunction = old
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withReturnTypeCalculator(
        returnTypeCalculator: ReturnTypeCalculator,
        f: () -> T,
    ): T {
        val oldReturnTypeCalculator = this.returnTypeCalculator
        return try {
            this.returnTypeCalculator = returnTypeCalculator
            f()
        } finally {
            this.returnTypeCalculator = oldReturnTypeCalculator
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> forLocalClasses(
        returnTypeCalculator: ReturnTypeCalculator,
        targetedLocalClasses: Set<CfirDeclaration>,
        f: () -> T,
    ): T {
        val oldReturnTypeCalculator = this.returnTypeCalculator
        val oldTargetedLocalClasses = this.targetedLocalClasses
        return try {
            this.returnTypeCalculator = returnTypeCalculator
            this.targetedLocalClasses = targetedLocalClasses
            f()
        } finally {
            this.returnTypeCalculator = oldReturnTypeCalculator
            this.targetedLocalClasses = oldTargetedLocalClasses
        }
    }
}

enum class CfirTowerDataMode {
    REGULAR,
    NESTED_CLASS,
    STATIC_MEMBER,
    CONSTRUCTOR_HEADER,
    ENUM_CONSTRUCTOR,
}

class CfirRegularTowerDataContexts private constructor(
    private val modeMap: EnumMap<CfirTowerDataMode, CfirTowerDataContext>,
    val primaryConstructorPureParametersScope: CfirLocalScope? = null,
    val primaryConstructorAllParametersScope: CfirLocalScope? = null,
    val activeMode: CfirTowerDataMode = CfirTowerDataMode.REGULAR,
) {
    constructor(
        regular: CfirTowerDataContext,
        forNestedClasses: CfirTowerDataContext? = null,
        forStaticMembers: CfirTowerDataContext? = null,
        forConstructorHeaders: CfirTowerDataContext? = null,
        forEnumConstructors: CfirTowerDataContext? = null,
        primaryConstructorPureParametersScope: CfirLocalScope? = null,
        primaryConstructorAllParametersScope: CfirLocalScope? = null,
    ) : this(
        enumMap(regular, forNestedClasses, forStaticMembers, forConstructorHeaders, forEnumConstructors),
        primaryConstructorPureParametersScope,
        primaryConstructorAllParametersScope,
        CfirTowerDataMode.REGULAR,
    )

    val currentContext: CfirTowerDataContext?
        get() = modeMap[activeMode]

    fun replaceTowerDataMode(newMode: CfirTowerDataMode): CfirRegularTowerDataContexts =
        if (newMode == activeMode) this
        else CfirRegularTowerDataContexts(
            modeMap,
            primaryConstructorPureParametersScope,
            primaryConstructorAllParametersScope,
            newMode,
        )

    fun replaceCurrentlyActiveContext(newContext: CfirTowerDataContext): CfirRegularTowerDataContexts {
        val newModeMap = EnumMap<CfirTowerDataMode, CfirTowerDataContext>(CfirTowerDataMode::class.java)
        newModeMap.putAll(modeMap)
        newModeMap[activeMode] = newContext
        return CfirRegularTowerDataContexts(
            newModeMap,
            primaryConstructorPureParametersScope,
            primaryConstructorAllParametersScope,
            activeMode,
        )
    }

    fun replaceAndSetActiveRegularContext(newContext: CfirTowerDataContext): CfirRegularTowerDataContexts {
        val newModeMap = EnumMap<CfirTowerDataMode, CfirTowerDataContext>(CfirTowerDataMode::class.java)
        newModeMap.putAll(modeMap)
        newModeMap[CfirTowerDataMode.REGULAR] = newContext
        return CfirRegularTowerDataContexts(
            newModeMap,
            primaryConstructorPureParametersScope,
            primaryConstructorAllParametersScope,
            CfirTowerDataMode.REGULAR,
        )
    }

    companion object {
        private fun enumMap(
            regular: CfirTowerDataContext,
            forNestedClasses: CfirTowerDataContext?,
            forStaticMembers: CfirTowerDataContext?,
            forConstructorHeaders: CfirTowerDataContext?,
            forEnumConstructors: CfirTowerDataContext?,
        ): EnumMap<CfirTowerDataMode, CfirTowerDataContext> {
            val result = EnumMap<CfirTowerDataMode, CfirTowerDataContext>(CfirTowerDataMode::class.java)
            result[CfirTowerDataMode.REGULAR] = regular
            result[CfirTowerDataMode.NESTED_CLASS] = forNestedClasses ?: regular
            result[CfirTowerDataMode.STATIC_MEMBER] = forStaticMembers ?: regular
            result[CfirTowerDataMode.CONSTRUCTOR_HEADER] = forConstructorHeaders ?: regular
            result[CfirTowerDataMode.ENUM_CONSTRUCTOR] = forEnumConstructors ?: regular
            return result
        }
    }
}

data class CfirPostponedAtomsResolutionContext(
    val towerDataContext: CfirTowerDataContext,
    val inferenceSession: CfirInferenceSession,
)

class CfirSpecialTowerDataContexts {
    private val anonymousFunctionContexts = LinkedHashMap<CfirFunctionSymbol<*>, CfirPostponedAtomsResolutionContext>()
    private val callableReferenceContexts = IdentityHashMap<CfirExpression, CfirPostponedAtomsResolutionContext>()

    fun clear() {
        anonymousFunctionContexts.clear()
        callableReferenceContexts.clear()
    }

    fun getAnonymousFunctionContext(symbol: CfirFunctionSymbol<*>): CfirPostponedAtomsResolutionContext? =
        anonymousFunctionContexts[symbol]

    fun storeAnonymousFunctionContext(
        symbol: CfirFunctionSymbol<*>,
        towerDataContext: CfirTowerDataContext,
        inferenceSession: CfirInferenceSession,
    ) {
        anonymousFunctionContexts[symbol] = CfirPostponedAtomsResolutionContext(towerDataContext, inferenceSession)
    }

    fun dropAnonymousFunctionContext(symbol: CfirFunctionSymbol<*>) {
        anonymousFunctionContexts.remove(symbol)
    }

    fun getCallableReferenceContext(access: CfirExpression): CfirPostponedAtomsResolutionContext? =
        callableReferenceContexts[access]

    fun storeCallableReferenceContext(
        access: CfirExpression,
        towerDataContext: CfirTowerDataContext,
        inferenceSession: CfirInferenceSession,
    ) {
        callableReferenceContexts[access] = CfirPostponedAtomsResolutionContext(towerDataContext, inferenceSession)
    }

    fun dropCallableReferenceContext(access: CfirExpression) {
        callableReferenceContexts.remove(access)
    }
}

abstract class CfirInferenceSession {
    open fun baseConstraintStorageForCandidate(
        candidate: org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate,
        bodyResolveContext: BodyResolveContext,
    ): ConstraintStorage? = null

    open fun customCompletionModeInsteadOfFull(call: org.cangnova.cangjie.cfir.expressions.CfirResolvable): ConstraintSystemCompletionMode? =
        null

    open fun <T> processPartiallyResolvedCall(
        call: T,
        resolutionMode: org.cangnova.cangjie.cfir.resolve.ResolutionMode,
        completionMode: ConstraintSystemCompletionMode,
    ) where T : org.cangnova.cangjie.cfir.expressions.CfirResolvable, T : org.cangnova.cangjie.cfir.expressions.CfirExpression {
    }

    open fun runLambdaCompletion(
        candidate: org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate,
        forOverloadByLambdaReturnType: Boolean,
        block: () -> Unit,
    ): ConstraintStorage? {
        block()
        return null
    }

    open fun addSubtypeConstraintIfCompatible(
        lowerType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        upperType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        system: ConstraintSystemImpl,
    ) {
    }

    companion object {
        val DEFAULT: CfirInferenceSession = object : CfirInferenceSession() {}
    }
}

class CfirPCLAInferenceSession(
    private val candidate: org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate,
    private val inferenceComponents: org.cangnova.cangjie.cfir.resolve.inference.InferenceComponents,
) : CfirInferenceSession()

private fun CfirScope.asTowerDataElement(isLocal: Boolean): CfirTowerDataElement =
    CfirTowerDataElement(scope = this, implicitReceiver = null, isLocal = isLocal)
