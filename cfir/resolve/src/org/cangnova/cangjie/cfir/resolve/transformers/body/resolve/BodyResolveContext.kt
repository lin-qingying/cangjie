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

package org.cangnova.cangjie.cfir.resolve.transformers.body.resolve

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.calls.ImplicitExtensionReceiverValue
import org.cangnova.cangjie.cfir.calls.ImplicitReceiverValue
import org.cangnova.cangjie.cfir.calls.InaccessibleImplicitReceiverValue
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.resolve.ImplicitValueStorage
import org.cangnova.cangjie.cfir.resolve.body.*
import org.cangnova.cangjie.cfir.resolve.body.asTowerDataElement
import org.cangnova.cangjie.cfir.resolve.body.collectTowerDataElementsForClass
import org.cangnova.cangjie.cfir.resolve.body.typeParametersForTower
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.codeFragmentContext
import org.cangnova.cangjie.cfir.resolve.inference.InferenceComponents
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirTypeParameterScopeImpl
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
import org.cangnova.cangjie.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.util.PrivateForInline
import java.util.EnumMap
import java.util.IdentityHashMap
import kotlin.collections.ArrayDeque
import kotlin.collections.LinkedHashMap
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.emptySet
import kotlin.collections.filterIsInstance
import kotlin.collections.firstOrNull
import kotlin.collections.fold
import kotlin.collections.hashMapOf
import kotlin.collections.isNotEmpty
import kotlin.collections.lastOrNull
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.mutableSetOf
import kotlin.collections.set
import kotlin.collections.toList

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

    private val overloadByLambdaCandidateStack: ArrayDeque<Candidate> = ArrayDeque()

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

    @OptIn(PrivateForInline::class)
    fun storeProperty(property: CfirProperty, session: CfirSession) {
        replaceTowerDataContext(towerDataContext.addLocalProperty(property, session))
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
        containers.addLast(declaration)
        return try {
            f()
        } finally {
            containers.removeLast()
        }
    }

    /**
     * 对齐 Kotlin `withContainerRegularClass`：regular class 既要入容器栈，也要更新当前 containing class。
     */
    @OptIn(PrivateForInline::class)
    private inline fun <T> withContainerRegularClass(declaration: CfirClass, f: () -> T): T {
        val oldContainingRegularClass = containingRegularClass
        containers.addLast(declaration)
        containingRegularClass = declaration
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

    fun <T> withCodeFragment(
        codeFragment: CfirCodeFragment,
        holder: SessionAndScopeSessionHolder,
        f: () -> T,
    ): T {
        val codeFragmentContext = codeFragment.codeFragmentContext ?: error("Context is not set for a code fragment")
        val towerDataContext = codeFragmentContext.towerDataContext

        val fragmentImportTowerDataElements = fileImportsScope.map { scope ->
            scope.asTowerDataElement(isLocal = false)
        }

        val base = towerDataContext
            .addNonLocalTowerDataElements(towerDataContext.nonLocalTowerDataElements)
            .addNonLocalTowerDataElements(fragmentImportTowerDataElements)

        val baseWithLocalScope = towerDataContext.localScopes.fold(base) { acc, scope -> acc.addLocalScope(scope) }

        val newContexts = CfirRegularTowerDataContexts(
            regular = baseWithLocalScope,
            forNestedClasses = baseWithLocalScope,
            forStaticMembers = baseWithLocalScope,
            forConstructorHeaders = null,
            forEnumConstructors = null,
            primaryConstructorPureParametersScope = null,
            primaryConstructorAllParametersScope = null,
        )

        return withTowerDataContexts(newContexts) {
            withContainer(codeFragment, f)
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
        val ownerTypeArguments = owner.typeParameters.map { typeParameter ->
            ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
        }
        val ownerType = when (ownerSymbol) {
            is CfirClassSymbol -> ownerSymbol.constructThisType(ownerTypeArguments)
            else -> ownerSymbol.constructType(ownerTypeArguments)
        }
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

        val forFinalizers = if (owner is CfirClass && !isContextCollectorMode) {
            val inaccessibleReceiver = InaccessibleImplicitReceiverValue(
                ownerSymbol,
                ownerType,
                InaccessibleReceiverKind.FINALIZER,
                holder.session,
                holder.scopeSession,
            )

            withTypeParameters.addReceiver(null, inaccessibleReceiver)
        } else {
            forMembersResolution
        }

        val newContexts = CfirRegularTowerDataContexts(
            regular = forMembersResolution,
            forNestedClasses = forNestedClasses,
            forStaticMembers = statics,
            forConstructorHeaders = forConstructorHeaders,
            forEnumConstructors = forEnumConstructors,
            forFinalizers = forFinalizers,
            primaryConstructorPureParametersScope = primaryConstructorPureParametersScope,
            primaryConstructorAllParametersScope = primaryConstructorAllParametersScope,
        )

        return withTowerDataContexts(newContexts, f)
    }

    /**
     * 进入 extend 声明体时，`this` 指向被扩展类型而不是 extend 声明本身。
     *
     * 作用域形状沿用 Kotlin class body 的 tower-data 框架：类型参数作用域优先，
     * 再加入一个可用的隐式接收者；差异只来自仓颉 extend 语义。
     */
    fun <T> withScopesForExtend(
        extend: CfirExtend,
        holder: SessionAndScopeSessionHolder,
        f: () -> T,
    ): T {
        val typeParameterScope = extend.typeParameters
            .takeIf { it.isNotEmpty() }
            ?.let(::CfirTypeParameterScopeImpl)

        val withTypeParameters = if (typeParameterScope != null) {
            towerDataContext.addNonLocalScope(typeParameterScope)
        } else {
            towerDataContext
        }

        val extensionReceiver = ImplicitExtensionReceiverValue(
            extend.symbol,
            extend.extendedTypeRef.coneType,
            holder.session,
            holder.scopeSession,
        )
        val forMembersResolution = withTypeParameters.addReceiver(null, extensionReceiver)

        val newContexts = CfirRegularTowerDataContexts(
            regular = forMembersResolution,
            forNestedClasses = withTypeParameters,
            forStaticMembers = withTypeParameters,
            forConstructorHeaders = withTypeParameters,
            forEnumConstructors = withTypeParameters,
            forFinalizers = forMembersResolution,
        )

        return withTowerDataContexts(newContexts, f)
    }

    /**
     * 对齐 Kotlin `forRegularClassBody`：
     * 统一 regular class body 的本地 class 注册、class scope 安装和容器栈更新。
     *
     * 仓颉当前没有 Kotlin 的 inner/companion 静态嵌套 tower-data mode 切换语义，
     * 因此这里只保留统一入口，不额外改写 tower data mode。
     */
    @OptIn(PrivateForInline::class)
    fun <T> forRegularClassBody(
        regularClass: CfirClass,
        holder: SessionAndScopeSessionHolder,
        f: () -> T,
    ): T {
        storeClassOrTypealiasIfNotNested(regularClass, holder.session)
        return withScopesForClass(regularClass, holder) {
            withContainerRegularClass(regularClass, f)
        }
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

    /** 对齐 K2 `withNamedFunction`：注册局部函数，并在函数声明 container 外层安装函数类型参数作用域。 */
    @OptIn(PrivateForInline::class)
    inline fun <T> withNamedFunction(namedFunction: CfirNamedFunction, session: CfirSession, f: () -> T): T {
        if (containerIfAny !is CfirClass) {
            storeFunction(namedFunction, session)
        }
        return withTypeParametersOf(namedFunction) {
            withContainer(namedFunction, f)
        }
    }

    /**
     * 对齐 K2 `withProperty`：属性声明自身引入类型参数作用域，并作为访问器解析的父容器。
     */
    @OptIn(PrivateForInline::class)
    inline fun <T> withProperty(property: CfirProperty, f: () -> T): T =
        withTypeParametersOf(property) {
            withContainer(property, f)
        }

    /**
     * 对齐 K2 `withPropertyAccessor`：访问器 body 独立拥有局部参数作用域，
     * 但仍运行在所属属性已经建立好的 class / property tower data 之内。
     */
    @OptIn(PrivateForInline::class)
    inline fun <T> withPropertyAccessor(
        property: CfirProperty,
        accessor: CfirPropertyAccessor,
        holder: SessionAndScopeSessionHolder,
        f: () -> T,
    ): T {
        if (accessor.body == null) {
            return if (accessor.isGetter) {
                withContainer(accessor, f)
            } else {
                withTowerDataCleanup {
                    addLocalScope(CfirLocalScope(holder.session))
                    withContainer(accessor, f)
                }
            }
        }

        return withTowerDataCleanup {
            addLocalScope(CfirLocalScope(holder.session))
            for (valueParameter in accessor.valueParameters) {
                storeValueParameterIfNeeded(valueParameter, holder.session)
            }

            withPublicApiInlineFunction(accessor) {
                withContainer(accessor, f)
            }
        }
    }

    /**
     * 对齐 K2 `withTypeParametersOf`，仅把真实类型参数放入 tower data。
     */
    @OptIn(PrivateForInline::class)
    inline fun <T> withTypeParametersOf(declaration: CfirTypeParameterRefsOwner, f: () -> T): T {
        val typeParameters = declaration.typeParameters.filterIsInstance<CfirTypeParameter>()
        if (typeParameters.isEmpty()) {
            return f()
        }

        return withTowerDataCleanup {
            addNonLocalScope(CfirTypeParameterScopeImpl(typeParameters))
            f()
        }
    }

    /**
     * 对齐 K2 `forFunctionBody`：函数体只开参数局部 scope；函数类型参数由 `withNamedFunction` 提前安装。
     * 仓颉没有 context parameter / receiver parameter 独立声明，逻辑相应简化。
     */
    @OptIn(PrivateForInline::class)
    inline fun <T> forFunctionBody(
        function: CfirFunction,
        holder: SessionAndScopeSessionHolder,
        f: () -> T,
    ): T = withFunctionLocalScope(function, holder.session, f)

    /**
     * finalizer body 与普通函数共享局部参数作用域，
     * 但需要切到 FINALIZER receiver mode：
     * 成员访问仍可通过当前类 receiver 完成，而把 `this` 当值直接使用会由后续 checker 报错。
     */
    @OptIn(PrivateForInline::class)
    inline fun <T> forFinalizerBody(
        finalizer: CfirFinalizer,
        holder: SessionAndScopeSessionHolder,
        f: () -> T,
    ): T = withTowerDataMode(CfirTowerDataMode.FINALIZER) {
        withFunctionLocalScope(finalizer, holder.session, f)
    }

    @PublishedApi
    internal inline fun <T> withFunctionLocalScope(
        function: CfirFunction,
        session: CfirSession,
        f: () -> T,
    ): T = withTowerDataCleanup {
        addLocalScope(CfirLocalScope(session))
        for (parameter in function.valueParameters) {
            storeVariable(parameter, session)
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

    /**
     * 捕获延迟参数解析上下文，用于候选级 speculative lambda body 重检后恢复。
     *
     * 官方语义允许同一个 lambda body 在不同候选目标函数类型下反复重检；这些重检
     * 不能消耗掉后续候选或最终提交仍需使用的 tower context。
     */
    @OptIn(PrivateForInline::class)
    fun capturePostponedAtomsResolutionContexts(): CfirSpecialTowerDataContextsSnapshot =
        specialTowerDataContexts.capture()

    @OptIn(PrivateForInline::class)
    fun restorePostponedAtomsResolutionContexts(snapshot: CfirSpecialTowerDataContextsSnapshot) {
        specialTowerDataContexts.restore(snapshot)
    }

    /**
     * 捕获 DFA/CFG 上下文，用于 speculative lambda body 重检后的候选级回滚。
     *
     * 对位 Kotlin FIR 的 data-flow snapshot 角色：候选试跑可以构造 CFG、记录 return
     * 表达式和赋值状态，但这些状态只有在最终选中的候选提交时才能保留。
     */
    fun captureDataFlowAnalyzerContext(): CfirDataFlowAnalyzerContextSnapshot =
        dataFlowAnalyzerContext.createSnapshot(IdentitySnapshotCfirMapper)

    fun restoreDataFlowAnalyzerContext(snapshot: CfirDataFlowAnalyzerContextSnapshot) {
        dataFlowAnalyzerContext.resetFrom(snapshot.context)
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

    /**
     * 记录当前正在按候选目标函数类型试跑的 lambda body。
     *
     * 仓颉 overload-by-lambda 需要完整重检 lambda body；当外层候选已经因 body 约束失败后，
     * 嵌套调用继续做完整 OBL 候选试跑不会让外层候选重新成功，只会制造指数级重复分析。
     */
    fun <T> withOverloadByLambdaCandidate(candidate: Candidate, block: () -> T): T {
        overloadByLambdaCandidateStack.addLast(candidate)
        return try {
            block()
        } finally {
            overloadByLambdaCandidateStack.removeLast()
        }
    }

    fun shouldReduceOverloadByLambdaCandidates(): Boolean =
        overloadByLambdaCandidateStack.lastOrNull()?.isSuccessful != false

    fun reportOverloadByLambdaCandidateDiagnostic(diagnostic: ResolutionDiagnostic) {
        val candidate = overloadByLambdaCandidateStack.lastOrNull() ?: return
        if (candidate.isSuccessful) {
            candidate.addDiagnostic(diagnostic)
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
    FINALIZER,
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
        forFinalizers: CfirTowerDataContext? = null,
        primaryConstructorPureParametersScope: CfirLocalScope? = null,
        primaryConstructorAllParametersScope: CfirLocalScope? = null,
    ) : this(
        enumMap(regular, forNestedClasses, forStaticMembers, forConstructorHeaders, forEnumConstructors, forFinalizers),
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
            forFinalizers: CfirTowerDataContext?,
        ): EnumMap<CfirTowerDataMode, CfirTowerDataContext> {
            val result = EnumMap<CfirTowerDataMode, CfirTowerDataContext>(CfirTowerDataMode::class.java)
            result[CfirTowerDataMode.REGULAR] = regular
            result[CfirTowerDataMode.NESTED_CLASS] = forNestedClasses ?: regular
            result[CfirTowerDataMode.STATIC_MEMBER] = forStaticMembers ?: regular
            result[CfirTowerDataMode.CONSTRUCTOR_HEADER] = forConstructorHeaders ?: regular
            result[CfirTowerDataMode.ENUM_CONSTRUCTOR] = forEnumConstructors ?: regular
            result[CfirTowerDataMode.FINALIZER] = forFinalizers ?: regular
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

    fun capture(): CfirSpecialTowerDataContextsSnapshot = CfirSpecialTowerDataContextsSnapshot(
        anonymousFunctionContexts = LinkedHashMap(anonymousFunctionContexts),
        callableReferenceContexts = IdentityHashMap(callableReferenceContexts),
    )

    fun restore(snapshot: CfirSpecialTowerDataContextsSnapshot) {
        anonymousFunctionContexts.clear()
        anonymousFunctionContexts.putAll(snapshot.anonymousFunctionContexts)
        callableReferenceContexts.clear()
        callableReferenceContexts.putAll(snapshot.callableReferenceContexts)
    }
}

class CfirSpecialTowerDataContextsSnapshot(
    val anonymousFunctionContexts: LinkedHashMap<CfirFunctionSymbol<*>, CfirPostponedAtomsResolutionContext>,
    val callableReferenceContexts: IdentityHashMap<CfirExpression, CfirPostponedAtomsResolutionContext>,
)

private object IdentitySnapshotCfirMapper : SnapshotCfirMapper {
    override fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T = symbol

    override fun <T : CfirElement> mapElement(element: T): T = element
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

        @JvmStatic
        protected fun prepareSharedBaseSystem(
            outerSystem: ConstraintSystemImpl,
            components: InferenceComponents,
        ): ConstraintSystemImpl {
            return components.createConstraintSystem().apply {
                addOuterSystem(outerSystem.currentStorage())
            }
        }
    }
}

class CfirPCLAInferenceSession(
    private val outerCandidate: Candidate,
    private val inferenceComponents: InferenceComponents,
) : CfirInferenceSession() {
    private var currentCommonSystem: ConstraintSystemImpl = prepareSharedBaseSystem(outerCandidate.system, inferenceComponents)

    override fun baseConstraintStorageForCandidate(
        candidate: Candidate,
        bodyResolveContext: BodyResolveContext,
    ): ConstraintStorage? {
        if (candidate.mightBeAnalyzedAndCompletedIndependently(bodyResolveContext)) return null
        return currentCommonSystem.currentStorage()
    }

    override fun customCompletionModeInsteadOfFull(
        call: CfirResolvable,
    ): ConstraintSystemCompletionMode? = when {
        call.candidate()?.usedOuterCs == true -> ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL
        else -> null
    }

    override fun <T> processPartiallyResolvedCall(
        call: T,
        resolutionMode: org.cangnova.cangjie.cfir.resolve.ResolutionMode,
        completionMode: ConstraintSystemCompletionMode,
    ) where T : CfirResolvable, T : CfirExpression {
        call.updateReturnTypeWithCurrentSubstitutor(resolutionMode)

        val candidate = call.candidate()
        if (candidate?.usedOuterCs != true) return

        currentCommonSystem.replaceContentWith(candidate.system.currentStorage())

        if (completionMode == ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL) {
            outerCandidate.postponedPCLACalls += ConeAtomWithCandidate(call, candidate)
        }
    }

    override fun runLambdaCompletion(
        candidate: Candidate,
        forOverloadByLambdaReturnType: Boolean,
        block: () -> Unit,
    ): ConstraintStorage? {
        if (forOverloadByLambdaReturnType) {
            val constraintAccumulatorForLambda = inferenceComponents.createConstraintSystem().apply {
                setBaseSystem(currentCommonSystem.currentStorage())
            }

            runWithSpecifiedCurrentCommonSystem(constraintAccumulatorForLambda, block)
            return constraintAccumulatorForLambda.currentStorage()
        }

        runWithSpecifiedCurrentCommonSystem(candidate.system, block)
        return null
    }

    private fun <T> runWithSpecifiedCurrentCommonSystem(newSystem: ConstraintSystemImpl, block: () -> T): T {
        val previous = currentCommonSystem
        return try {
            currentCommonSystem = newSystem
            block()
        } finally {
            currentCommonSystem = previous
        }
    }

    fun applyResultsToMainCandidate() {
        outerCandidate.system.replaceContentWith(currentCommonSystem.currentStorage())
    }

    override fun addSubtypeConstraintIfCompatible(
        lowerType: ConeCangJieType,
        upperType: ConeCangJieType,
        system: ConstraintSystemImpl,
    ) {
        currentCommonSystem.addSubtypeConstraintIfCompatible(
            lowerType,
            upperType,
            ConeExpectedTypeConstraintPosition,
        )
    }

    private fun CfirExpression.updateReturnTypeWithCurrentSubstitutor(
        resolutionMode: org.cangnova.cangjie.cfir.resolve.ResolutionMode,
    ) {
        val system = (this as? CfirResolvable)?.candidate()?.system ?: currentCommonSystem
        val substitutor = system.currentStorage()
            .buildCurrentSubstitutor(inferenceComponents.session.typeContext, emptyMap<TypeConstructorMarker, org.cangnova.cangjie.type.model.CangJieTypeMarker>())
            .asCone()
        val currentType = coneTypeOrNull ?: return
        val updatedType = substitutor.substituteOrNull(currentType) ?: return
        replaceConeTypeOrNull(updatedType)
    }

    private fun CfirResolvable.candidate(): Candidate? =
        (calleeReference as? CfirNamedReferenceWithCandidate)?.candidate

    private fun Candidate.mightBeAnalyzedAndCompletedIndependently(bodyResolveContext: BodyResolveContext): Boolean {
        when (val mode = callInfo.resolutionMode) {
            is org.cangnova.cangjie.cfir.resolve.ResolutionMode.WithExpectedType -> {
                if (mode.expectedType.containsNotFixedTypeVariables()) return false
            }

            is org.cangnova.cangjie.cfir.resolve.ResolutionMode.WithStatus,
            is org.cangnova.cangjie.cfir.resolve.ResolutionMode.UpdateImplicitTypeRef,
            -> error("$this call should not be analyzed in ${callInfo.resolutionMode}")

            is org.cangnova.cangjie.cfir.resolve.ResolutionMode.ContextDependent,
            org.cangnova.cangjie.cfir.resolve.ResolutionMode.ContextIndependent,
            is org.cangnova.cangjie.cfir.resolve.ResolutionMode.ReceiverResolution,
            -> {
            }
        }

        val callSite = callInfo.callSite
        if (callSite is CfirAnnotationCall) return true
        if (callSite is CfirArrayLiteral) return bodyResolveContext.isInsideAnnotationContext
        if (callSite !is CfirResolvable) return false

        if (dispatchReceiver?.expression?.isReceiverPostponed() == true) return false
        if (givenExtensionReceiver?.expression?.isReceiverPostponed() == true) return false

        val returnType = (symbol as? CfirCallableSymbol<*>)?.cfir
            ?.let { it as? CfirCallableDeclaration }
            ?.let(bodyResolveContext.returnTypeCalculator::tryCalculateReturnType)
        if (returnType?.coneType?.containsNotFixedTypeVariables() == true) return false

        if (callInfo.arguments.any { !it.isTrivialArgument() }) return false
        return true
    }

    private fun CfirExpression.isTrivialArgument(): Boolean = when (this) {
        is CfirArrayLiteral -> false
        is CfirResolvable -> when (val candidate = candidate()) {
            null -> coneTypeOrNull?.containsNotFixedTypeVariables() != true
            else -> !candidate.usedOuterCs
        }

        is CfirWrappedExpression -> expression.isTrivialArgument()
        is CfirFunctionCall -> argumentList.arguments.all { it.isTrivialArgument() }
        is CfirBinaryOp -> left.isTrivialArgument() && right.isTrivialArgument()
        is CfirComparisonExpression -> left.isTrivialArgument() && right.isTrivialArgument()
        is CfirBlock -> (statements.lastOrNull() as? CfirExpression)?.isTrivialArgument() ?: true
        is CfirTupleLiteral -> elements.all { it.isTrivialArgument() }
        is CfirStringInterpolation -> parts.all { it.isTrivialArgument() }
        is CfirLiteralExpression -> true
        else -> false
    }

    private fun CfirExpression.isReceiverPostponed(): Boolean {
        return when {
            coneTypeOrNull?.containsNotFixedTypeVariables() == true -> true
            (this as? CfirResolvable)?.candidate()?.usedOuterCs == true -> true
            else -> false
        }
    }

    private fun ConeCangJieType.containsNotFixedTypeVariables(): Boolean =
        contains {
            it is ConeTypeVariableType && it.typeConstructor in currentCommonSystem.allTypeVariables
        }
}

private fun CfirScope.asTowerDataElement(isLocal: Boolean): CfirTowerDataElement =
    CfirTowerDataElement(scope = this, implicitReceiver = null, isLocal = isLocal)
