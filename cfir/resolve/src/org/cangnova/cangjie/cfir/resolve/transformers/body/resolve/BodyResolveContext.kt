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
import org.cangnova.cangjie.cfir.correspondingProperty
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
import org.cangnova.cangjie.cfir.resolve.providers.semanticExtendedType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirTypeParameterScopeImpl
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeVariable
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
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.inference.model.ReceiverConstraintPosition
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeVariableMarker
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
    /** 当前上下文使用的返回类型计算器。 */
    @set:PrivateForInline
    var returnTypeCalculator: ReturnTypeCalculator,
    /** body resolve 共享的数据流分析上下文。 */
    val dataFlowAnalyzerContext: CfirDataFlowAnalyzerContext,
    /** 是否处于仅收集上下文、暂不执行完整解析的模式。 */
    private val isContextCollectorMode: Boolean = false,
) {
    // ── Imports & file ──────────────────────────────────────────────────────

    /** 当前文件已构造的导入 scope 列表。 */
    val fileImportsScope: MutableList<CfirScope> = mutableListOf()

    /** 当前正在进行 body resolve 的文件。 */
    @set:PrivateForInline
    lateinit var file: CfirFile

    // ── Tower data contexts ────────────────────────────────────────────────

    /** 普通 tower data context 集合，按 [CfirTowerDataMode] 区分。 */
    @PrivateForInline
    var regularTowerDataContexts: CfirRegularTowerDataContexts =
        CfirRegularTowerDataContexts(regular = CfirTowerDataContext())

    /** 匿名函数和 callable reference 等特殊上下文缓存。 */
    @PrivateForInline
    val specialTowerDataContexts: CfirSpecialTowerDataContexts = CfirSpecialTowerDataContexts()

    /** 当前 [towerDataMode] 对应的活跃 tower data context。 */
    @OptIn(PrivateForInline::class)
    val towerDataContext: CfirTowerDataContext
        get() = regularTowerDataContexts.currentContext
            ?: throw AssertionError("No regular data context found, towerDataMode = $towerDataMode")

    /** 当前 tower data context 中的隐式值存储。 */
    val implicitValueStorage: ImplicitValueStorage
        get() = towerDataContext.implicitValueStorage

    /** 当前 body resolve 使用的 tower data 模式。 */
    @OptIn(PrivateForInline::class)
    var towerDataMode: CfirTowerDataMode
        get() = regularTowerDataContexts.activeMode
        set(value) {
            regularTowerDataContexts = regularTowerDataContexts.replaceTowerDataMode(value)
        }

    // ── Containers & inference state ───────────────────────────────────────

    /** 当前声明容器栈，文件和声明进入/退出时同步维护。 */
    @set:PrivateForInline
    var containers: ArrayDeque<CfirDeclaration> = ArrayDeque()

    /**
     * 当前 loop jump 可见性区域栈。
     *
     * 由表达式 transformer 按循环结构区域维护（见 [LoopJumpScope]）；函数边界
     * 由函数 body 解析入口统一压栈。`BodyResolveContext` 按文件共享，因此函数
     * 边界的显式维护是必需而非优化。
     */
    val loopJumpScopes: ArrayDeque<LoopJumpScope> = ArrayDeque()

    /** 当前最内层声明容器；没有容器时返回 null。 */
    val containerIfAny: CfirDeclaration?
        get() = containers.lastOrNull()

    /** 当前最内层 regular class 容器。 */
    @set:PrivateForInline
    var containingRegularClass: CfirClass? = null

    /**
     * 当前最内层 class-like 声明容器。
     *
     * 该属性只描述 body resolve 的声明归属，用于 `this`/`super` 等接收者恢复；
     * `super` 在 interface/struct/enum/extend 中是否合法仍由专门 checker 判定。
     */
    val containingClassLikeDeclaration: CfirClassLikeDeclaration?
        get() = containers.asReversed().filterIsInstance<CfirClassLikeDeclaration>().firstOrNull()

    /** 当前调用推断会话。 */
    @set:PrivateForInline
    var inferenceSession: CfirInferenceSession = CfirInferenceSession.DEFAULT

    /**
     * overload-by-lambda 候选分析帧。
     *
     * rollback 试跑可以在候选已经失败后停止继续解析目标类型实参；最终落地分析必须保持完整解析，
     * 以便真实选中候选仍产生普通 body resolve 诊断。
     */
    private data class OverloadByLambdaCandidateFrame(
        val candidate: Candidate,
        val shortCircuitOnFailure: Boolean,
    )

    /**
     * 显式目标类型消费点的根表达式检查帧。
     *
     * [rootExpression] 使用对象 identity 区分真正的赋值或初始化器根与 block/if/match 等
     * 向下传播 expected type 后解析到的嵌套尾表达式。普通赋值才会捕获 [assignmentMismatchOutcome]；
     * 初始化器只借此保留“仅目标类型不匹配”的调用语义，并交由声明 checker 报告诊断。
     */
    private data class ExpectedTypeRootFrame(
        val rootExpression: CfirExpression,
        val expectedType: ConeCangJieType,
        val capturesAssignmentMismatchOutcome: Boolean,
        var assignmentMismatchOutcome: CfirAssignmentTypeMismatchOutcome? = null,
    )

    /** overload-by-lambda 解析过程中待报告诊断的候选栈。 */
    private val overloadByLambdaCandidateStack: ArrayDeque<OverloadByLambdaCandidateFrame> = ArrayDeque()

    /** 赋值和显式类型初始化器共享的 expected-type 根检查帧栈。 */
    private val expectedTypeRootStack: ArrayDeque<ExpectedTypeRootFrame> = ArrayDeque()

    /** 当前是否正在解析赋值 RHS 或显式类型初始化器值。 */
    @set:PrivateForInline
    var isInsideAssignmentOrInitializerValue: Boolean = false

    /** 当前正在解析 initializer 的局部变量栈。 */
    private val variableInitializerStack: ArrayDeque<List<CfirVariable>> = ArrayDeque()

    /** 当前正在解析 initializer 的字段变量栈。 */
    private val fieldInitializerStack: ArrayDeque<CfirFieldVariable> = ArrayDeque()

    /** 当前正在解析 initializer 的最内层变量。 */
    val variableBeingInitialized: CfirVariable?
        get() = variableInitializerStack.lastOrNull()?.singleOrNull()

    /** 当前正在解析 initializer 的最内层变量集合。 */
    val variablesBeingInitialized: List<CfirVariable>
        get() = variableInitializerStack.lastOrNull().orEmpty()

    /** 当前正在解析 initializer 的最内层字段。 */
    val fieldBeingInitialized: CfirFieldVariable?
        get() = fieldInitializerStack.lastOrNull()

    /** 当前 public API inline 函数上下文。 */
    @set:PrivateForInline
    var publicApiInlineFunction: CfirFunction? = null

    /** 当前声明路径上可用于类型解析配置的 class 容器栈。 */
    @set:PrivateForInline
    var containingClassDeclarations: ArrayDeque<CfirClass> = ArrayDeque()

    /** 当前 designated resolve 目标中的局部 class 声明集合。 */
    @set:PrivateForInline
    var targetedLocalClasses: Set<CfirDeclaration> = emptySet()

    /** 对齐 K2：当前正在按依赖上下文分析的匿名函数集合。 */
    val anonymousFunctionsAnalyzedInDependentContext: MutableSet<CfirFunctionSymbol<*>> = mutableSetOf()

    /** 对齐 K2：嵌套的本地 class-like 声明追溯外层所属。 */
    val outerLocalClassForNested: MutableMap<CfirClassLikeSymbol<*>, CfirClassLikeSymbol<*>> = hashMapOf()

    /** 当前是否处于 annotation class 参数或默认值解析上下文。 */
    @set:PrivateForInline
    var isInsideAnnotationContext: Boolean = false

    /** 当前是否正在预解析某个外层调用的实参表达式。 */
    private var callArgumentResolutionDepth: Int = 0

    /** 当前是否正在解析显式 `return` 的结果表达式。 */
    private var explicitReturnResultDepth: Int = 0

    /**
     * 在外层调用实参预解析上下文中执行 [block]。
     *
     * 嵌套泛型调用在该上下文中不能过早 full completion；它需要保留子候选约束，
     * 等外层候选形参 expected type 进入同一约束系统后再完成。
     */
    fun <T> withCallArgumentResolution(block: () -> T): T {
        callArgumentResolutionDepth += 1
        return try {
            block()
        } finally {
            callArgumentResolutionDepth -= 1
        }
    }

    /** 是否处于外层调用实参预解析过程。 */
    val isInsideCallArgumentResolution: Boolean
        get() = callArgumentResolutionDepth > 0

    /**
     * 在显式 `return` 结果语境中执行 [block]。
     *
     * 仓颉会完整检查被返回值的 operator 表达式；与之相对，单独作为语句且结果未被消费的
     * operator 仍按普通错误恢复规则处理。该语境由 return owner 建立，而不是从源码范围猜测。
     */
    fun <T> withExplicitReturnResult(block: () -> T): T {
        explicitReturnResultDepth += 1
        return try {
            block()
        } finally {
            explicitReturnResultDepth -= 1
        }
    }

    /** 当前表达式是否属于显式 `return` 的结果子树。 */
    val isInsideExplicitReturnResult: Boolean
        get() = explicitReturnResultDepth > 0

    // ── File entry ─────────────────────────────────────────────────────────

    /**
     * 在指定文件上下文中执行 body resolve。
     *
     * 该函数会重置并恢复文件导入 scope，同时把文件压入容器栈。
     */
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

    /** 把局部变量写入当前 tower data context。 */
    @OptIn(PrivateForInline::class)
    fun storeVariable(variable: CfirVariable, session: CfirSession) {
        replaceTowerDataContext(towerDataContext.addLocalVariable(variable, session))
    }

    /** 把局部属性写入当前 tower data context。 */
    @OptIn(PrivateForInline::class)
    fun storeProperty(property: CfirProperty, session: CfirSession) {
        replaceTowerDataContext(towerDataContext.addLocalProperty(property, session))
    }

    /** 在参数不是下划线占位符时把 value parameter 写入局部变量 scope。 */
    fun storeValueParameterIfNeeded(valueParameter: CfirValueParameter, session: CfirSession) {
        if (valueParameter.name != UNDERSCORE_FOR_UNUSED_VAR) {
            storeVariable(valueParameter, session)
        }
    }

    /** 把局部函数写入当前最后一个局部 scope。 */
    @OptIn(PrivateForInline::class)
    fun storeFunction(function: CfirNamedFunction, session: CfirSession) {
        val lastScope = towerDataContext.localScopes.lastOrNull() ?: return
        val newLastScope = lastScope.storeFunction(function, session)
        replaceTowerDataContext(towerDataContext.setLastLocalScope(newLastScope))
    }

    /** 把非嵌套局部 class/typealias 写入当前最后一个局部 scope。 */
    @OptIn(PrivateForInline::class)
    fun storeClassOrTypealiasIfNotNested(classLike: CfirClassLikeDeclaration, session: CfirSession) {
        // 嵌套类/类型别名不进局部作用域，由外层 class scope 承载。
        if (containerIfAny is CfirClass) return
        val lastScope = towerDataContext.localScopes.lastOrNull() ?: return
        val newLastScope = lastScope.storeClassOrTypeAlias(classLike, session)
        replaceTowerDataContext(towerDataContext.setLastLocalScope(newLastScope))
    }

    // ── Tower data mutation primitives ────────────────────────────────────

    /** 替换当前活跃 tower data context。 */
    @OptIn(PrivateForInline::class)
    fun replaceTowerDataContext(newContext: CfirTowerDataContext) {
        regularTowerDataContexts = regularTowerDataContexts.replaceCurrentlyActiveContext(newContext)
    }

    /** 清理当前 body resolve 上下文中的可复用状态。 */
    @OptIn(PrivateForInline::class)
    fun clear() {
        specialTowerDataContexts.clear()
        fileImportsScope.clear()
        dataFlowAnalyzerContext.reset()
    }

    /** 向当前 tower data context 添加一个非局部 scope。 */
    fun addNonLocalScope(scope: CfirScope) {
        addNonLocalTowerDataElement(scope.asTowerDataElement(isLocal = false))
    }

    /** 向当前 tower data context 批量添加非局部 scope。 */
    fun addNonLocalScopes(scopes: List<CfirScope>) {
        if (scopes.isEmpty()) return
        addNonLocalTowerDataElements(scopes.map { it.asTowerDataElement(isLocal = false) })
    }

    /** scope 非空时添加到当前非局部 tower data。 */
    fun addNonLocalScopeIfNotNull(scope: CfirScope?) {
        if (scope == null) return
        addNonLocalScope(scope)
    }

    /** 添加一个非局部 tower data element。 */
    fun addNonLocalTowerDataElement(element: CfirTowerDataElement) {
        replaceTowerDataContext(towerDataContext.addNonLocalTowerDataElements(listOf(element)))
    }

    /** 批量添加非局部 tower data element。 */
    fun addNonLocalTowerDataElements(newElements: List<CfirTowerDataElement>) {
        replaceTowerDataContext(towerDataContext.addNonLocalTowerDataElements(newElements))
    }

    /** 添加一个局部 scope。 */
    fun addLocalScope(localScope: CfirLocalScope) {
        replaceTowerDataContext(towerDataContext.addLocalScope(localScope))
    }

    /** 添加一个隐式接收者。 */
    fun addReceiver(name: Name?, implicitReceiverValue: ImplicitReceiverValue<*>) {
        replaceTowerDataContext(towerDataContext.addReceiver(name, implicitReceiverValue))
    }

    /** 接收者非空时添加到当前 tower data context。 */
    fun addReceiverIfNotNull(name: Name?, implicitReceiverValue: ImplicitReceiverValue<*>?) {
        if (implicitReceiverValue == null) return
        addReceiver(name, implicitReceiverValue)
    }

    // ── Container stack ───────────────────────────────────────────────────

    /** 在声明容器栈中临时压入 [declaration] 并执行 [f]。 */
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
     * 在局部变量 initializer 上下文内执行解析。
     *
     * 变量声明会先进入当前 scope 以遮蔽外层同名绑定，但 initializer 内对该变量自身的访问
     * 不能解析为有效引用；表达式解析层据此产生 unresolved 诊断。
     */
    fun <T> withVariableInitializer(variable: CfirVariable, f: () -> T): T =
        withVariableInitializer(listOf(variable), f)

    /**
     * 在一组 pattern binding 的 initializer 上下文内执行解析。
     *
     * `let (a, b) = ...` 这类声明会同时把多个 binding 引入当前声明作用域；initializer
     * 中对这些名字的访问必须命中内层声明并报告 unresolved，而不是退回外层同名变量。
     */
    fun <T> withVariableInitializer(variables: Collection<CfirVariable>, f: () -> T): T {
        if (variables.isEmpty()) return f()
        variableInitializerStack.addLast(variables.toList())
        return try {
            f()
        } finally {
            variableInitializerStack.removeLast()
        }
    }

    /**
     * 在字段 initializer 上下文内执行解析。
     *
     * 字段 initializer 中的裸名字若解析到当前字段自身，官方语义按未声明名处理；显式
     * `this.field` 仍然是成员访问，后续初始化检查负责报告初始化前使用。
     */
    fun <T> withFieldInitializer(field: CfirFieldVariable, f: () -> T): T {
        fieldInitializerStack.addLast(field)
        return try {
            f()
        } finally {
            fieldInitializerStack.removeLast()
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

    /** 在 containing class 栈中临时压入 [declaration] 并执行 [f]。 */
    inline fun <T> withContainingClass(declaration: CfirClass, f: () -> T): T {
        containingClassDeclarations.addLast(declaration)
        return try {
            f()
        } finally {
            containingClassDeclarations.removeLast()
        }
    }

    // ── Tower data with/cleanup combinators ───────────────────────────────

    /** 临时替换整组 regular tower data contexts。 */
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

    /** 临时替换当前活跃 tower data context。 */
    inline fun <T> withTowerDataContext(newContext: CfirTowerDataContext, f: () -> T): T {
        val initialContext = towerDataContext
        return try {
            replaceTowerDataContext(newContext)
            f()
        } finally {
            replaceTowerDataContext(initialContext)
        }
    }

    /** 执行 [l] 后恢复进入前的 tower data context。 */
    inline fun <R> withTowerDataCleanup(l: () -> R): R {
        val initialContext = towerDataContext
        return try {
            l()
        } finally {
            replaceTowerDataContext(initialContext)
        }
    }

    /** 在指定 tower data mode 下执行 [f]，结束后恢复旧模式。 */
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

    /** 执行 [l] 后恢复进入前的 tower data mode。 */
    inline fun <R> withTowerDataModeCleanup(l: () -> R): R {
        val initialMode = towerDataMode
        return try {
            l()
        } finally {
            towerDataMode = initialMode
        }
    }

    /**
     * 在代码片段携带的上下文中执行 body resolve。
     *
     * 代码片段复用原上下文的 tower data，并额外叠加当前文件导入 scope。
     */
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

    /**
     * 为 class-like 声明体安装成员解析所需的 tower data contexts。
     *
     * 该函数分别构造成员、嵌套类、静态成员、构造器头、enum constructor 和 finalizer 的上下文。
     */
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
                constructor.scopesWithPrimaryConstructorParameters(holder.session)
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

        val semanticExtendedType = checkNotNull(extend.semanticExtendedType(holder.session)) {
            "Extend target type must be resolved before body resolution"
        }
        val extensionReceiver = ImplicitExtensionReceiverValue(
            extend.symbol,
            semanticExtendedType,
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

    /** 返回主构造器纯参数 scope。 */
    fun getPrimaryConstructorPureParametersScope(): CfirLocalScope? {
        @OptIn(PrivateForInline::class)
        return regularTowerDataContexts.primaryConstructorPureParametersScope
    }

    /** 返回主构造器全部参数 scope。 */
    fun getPrimaryConstructorAllParametersScope(): CfirLocalScope? {
        @OptIn(PrivateForInline::class)
        return regularTowerDataContexts.primaryConstructorAllParametersScope
    }

    // ── Function body / lambda ────────────────────────────────────────────

    /** 对齐 K2 `withNamedFunction`：注册局部函数，并在函数声明 container 外层安装函数类型参数作用域。 */
    @OptIn(PrivateForInline::class)
    inline fun <T> withNamedFunction(namedFunction: CfirNamedFunction, session: CfirSession, f: () -> T): T {
        if (namedFunction.isLocal || containerIfAny !is CfirClass) {
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
        resolveParameterDefaultsSequentially: Boolean,
        f: () -> T,
    ): T = withFunctionLocalScope(
        function = function,
        session = holder.session,
        preloadValueParameters = !resolveParameterDefaultsSequentially,
        f = f,
    )

    /**
     * finalizer body 与普通函数共享局部参数作用域，
     * 但需要切到 FINALIZER receiver mode：
     * 成员访问仍可通过当前类 receiver 完成，而把 `this` 当值直接使用会由后续 checker 报错。
     */
    @OptIn(PrivateForInline::class)
    inline fun <T> forFinalizerBody(
        finalizer: CfirFinalizer,
        holder: SessionAndScopeSessionHolder,
        resolveParameterDefaultsSequentially: Boolean,
        f: () -> T,
    ): T = withTowerDataMode(CfirTowerDataMode.FINALIZER) {
        withFunctionLocalScope(
            function = finalizer,
            session = holder.session,
            preloadValueParameters = !resolveParameterDefaultsSequentially,
            f = f,
        )
    }

    /**
     * 在函数局部 scope 中执行 [f]。
     *
     * 完整 body resolve 按声明顺序解析默认值，由 [withValueParameter] 在每个默认值完成后
     * 提交当前参数；仅推断函数体时签名已经完成，可以直接预装全部参数。
     */
    @PublishedApi
    internal inline fun <T> withFunctionLocalScope(
        function: CfirFunction,
        session: CfirSession,
        preloadValueParameters: Boolean,
        f: () -> T,
    ): T = withTowerDataCleanup {
        addLocalScope(CfirLocalScope(session))
        if (preloadValueParameters) {
            for (parameter in function.valueParameters) {
                storeVariable(parameter, session)
            }
        }
        f()
    }

    /**
     * 在值参数容器中解析声明内容，并在完成后把参数提交到当前局部 scope。
     * 因而默认值只能引用此前已经完成的参数，不能引用自身或后置参数。
     */
    @OptIn(PrivateForInline::class)
    inline fun <T> withValueParameter(
        valueParameter: CfirValueParameter,
        session: CfirSession,
        f: () -> T,
    ): T {
        val result = withContainer(valueParameter) {
            f()
        }
        storeValueParameterIfNeeded(valueParameter, session)
        return result
    }

    /** 为匿名函数建立参数局部 scope 和容器上下文。 */
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

    /** 记录当前 lambda 正在依赖候选上下文分析，结束后移除标记。 */
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

    /** 在构造器 header tower data mode 下解析构造器声明。 */
    @OptIn(PrivateForInline::class)
    inline fun <T> forConstructor(constructor: CfirConstructor, f: () -> T): T =
        withTowerDataMode(CfirTowerDataMode.CONSTRUCTOR_HEADER) {
            withContainer(constructor, f)
        }

    /** 解析构造器参数默认值，并安装构造器参数 scope。 */
    @OptIn(PrivateForInline::class)
    inline fun <T> forConstructorParameters(
        constructor: CfirConstructor,
        @Suppress("UNUSED_PARAMETER") owningClass: CfirClassLikeDeclaration?,
        holder: SessionAndScopeSessionHolder,
        f: () -> T,
    ): T {
        // 默认值按参数声明顺序解析；withValueParameter 会在当前参数完成后再提交到该 scope。
        require(towerDataMode == CfirTowerDataMode.CONSTRUCTOR_HEADER) {
            "forConstructorParameters must be nested inside forConstructor"
        }
        return withTowerDataCleanup {
            addLocalScope(CfirLocalScope(holder.session))
            f()
        }
    }

    /** 解析委托构造调用的子表达式，并复用构造器参数上下文。 */
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

    /** 构造器参数默认值和委托构造调用子表达式共享的参数 scope 安装入口。 */
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

    /** 构造包含构造器 value parameters 的局部 scope。 */
    fun buildConstructorParametersScope(
        constructor: CfirConstructor,
        session: CfirSession,
    ): CfirLocalScope =
        constructor.valueParameters.fold(CfirLocalScope(session)) { scope, parameter ->
            scope.storeVariable(parameter, session)
        }

    /**
     * 对齐 Kotlin FIR `FirConstructor.scopesWithPrimaryConstructorParameters`：
     * pure scope 只暴露没有提升为成员属性的主构造参数，all scope 保留完整参数集合。
     */
    private fun CfirConstructor.scopesWithPrimaryConstructorParameters(
        session: CfirSession,
    ): Pair<CfirLocalScope, CfirLocalScope> {
        var pureScope = CfirLocalScope(session)
        var allScope = CfirLocalScope(session)
        for (parameter in valueParameters) {
            allScope = allScope.storeVariable(parameter, session)
            if (parameter.correspondingProperty == null) {
                pureScope = pureScope.storeVariable(parameter, session)
            }
        }
        return pureScope to allScope
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

    /** 使用缓存的匿名函数 postponed context 执行 [f]。 */
    @OptIn(PrivateForInline::class)
    inline fun <T> withAnonymousFunctionTowerDataContext(symbol: CfirFunctionSymbol<*>, f: () -> T): T {
        return withTemporaryRegularContext(specialTowerDataContexts.getAnonymousFunctionContext(symbol), f)
    }

    /** 使用缓存的 callable reference postponed context 执行 [f]。 */
    @OptIn(PrivateForInline::class)
    inline fun <T> withCallableReferenceTowerDataContext(access: CfirExpression, f: () -> T): T {
        return withTemporaryRegularContext(specialTowerDataContexts.getCallableReferenceContext(access), f)
    }

    /** 临时切换到 postponed atoms 保存的 regular context 和 inference session。 */
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

    /** 保存匿名函数当前 postponed context。 */
    @OptIn(PrivateForInline::class)
    fun storeContextForAnonymousFunction(anonymousFunction: CfirFunction) {
        val symbol = anonymousFunction.symbol as? CfirFunctionSymbol<*> ?: return
        specialTowerDataContexts.storeAnonymousFunctionContext(symbol, towerDataContext, inferenceSession)
    }

    /** 删除匿名函数已保存的 postponed context。 */
    @OptIn(PrivateForInline::class)
    fun dropContextForAnonymousFunction(anonymousFunction: CfirFunction) {
        val symbol = anonymousFunction.symbol as? CfirFunctionSymbol<*> ?: return
        specialTowerDataContexts.dropAnonymousFunctionContext(symbol)
    }

    /** 保存 callable reference 当前 postponed context。 */
    @OptIn(PrivateForInline::class)
    fun storeCallableReferenceContext(callableReferenceAccess: CfirExpression) {
        specialTowerDataContexts.storeCallableReferenceContext(
            callableReferenceAccess,
            towerDataContext,
            inferenceSession,
        )
    }

    /** 删除 callable reference 已保存的 postponed context。 */
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

    /** 从快照恢复延迟参数解析上下文。 */
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

    /** 从快照恢复 DFA/CFG 上下文。 */
    fun restoreDataFlowAnalyzerContext(snapshot: CfirDataFlowAnalyzerContextSnapshot) {
        dataFlowAnalyzerContext.resetFrom(snapshot.context)
    }

    // ── Inference / expectations ──────────────────────────────────────────

    /** 在指定推断会话中执行 [block]，结束后恢复旧会话。 */
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
    fun <T> withOverloadByLambdaCandidate(
        candidate: Candidate,
        shortCircuitOnFailure: Boolean,
        block: () -> T,
    ): T {
        overloadByLambdaCandidateStack.addLast(OverloadByLambdaCandidateFrame(candidate, shortCircuitOnFailure))
        return try {
            block()
        } finally {
            overloadByLambdaCandidateStack.removeLast()
        }
    }

    /** 是否仍应缩减 overload-by-lambda 候选集合。 */
    fun shouldReduceOverloadByLambdaCandidates(): Boolean {
        val frame = overloadByLambdaCandidateStack.lastOrNull() ?: return true
        return !frame.shortCircuitOnFailure || frame.candidate.isSuccessful
    }

    /** 当前是否处于允许在首个候选失败点停止目标类型检查的 overload-by-lambda 试跑。 */
    fun shouldShortCircuitOverloadByLambdaTargetChecks(): Boolean =
        overloadByLambdaCandidateStack.lastOrNull()?.shortCircuitOnFailure == true

    /** 当前可短路试跑候选是否已经失败。 */
    fun hasCurrentShortCircuitableOverloadByLambdaCandidateFailure(): Boolean {
        val frame = overloadByLambdaCandidateStack.lastOrNull() ?: return false
        return frame.shortCircuitOnFailure && !frame.candidate.isSuccessful
    }

    /** 当前推断会话的共享约束系统是否已经出现矛盾。 */
    fun hasCurrentInferenceSessionContradiction(): Boolean =
        inferenceSession.hasCurrentConstraintContradiction

    /** 把 overload-by-lambda 试跑诊断写入当前候选。 */
    fun reportOverloadByLambdaCandidateDiagnostic(diagnostic: ResolutionDiagnostic) {
        val candidate = overloadByLambdaCandidateStack.lastOrNull()?.candidate ?: return
        if (candidate.isSuccessful) {
            candidate.addDiagnostic(diagnostic)
        }
    }

    /**
     * 在赋值或显式类型初始化器值上建立 expected-type 根检查帧。
     *
     * 根 identity 保证 completion 不会把嵌套表达式的约束错误冒充为外层消费点的类型不匹配。
     * 普通赋值通过 [capturesAssignmentMismatchOutcome] 额外收集 assignment 节点需要的结构化结果；
     * 初始化器仅使用同一边界恢复已解析调用的实际类型。
     */
    @OptIn(PrivateForInline::class)
    private fun <T> withExpectedTypeRoot(
        rootExpression: CfirExpression,
        expectedType: ConeCangJieType?,
        capturesAssignmentMismatchOutcome: Boolean,
        block: (ExpectedTypeRootFrame?) -> T,
    ): T {
        val old = isInsideAssignmentOrInitializerValue
        isInsideAssignmentOrInitializerValue = true
        val frame = expectedType?.let {
            ExpectedTypeRootFrame(rootExpression, it, capturesAssignmentMismatchOutcome)
        }
        if (frame != null) {
            expectedTypeRootStack.addLast(frame)
        }
        return try {
            block(frame)
        } finally {
            if (frame != null) {
                check(expectedTypeRootStack.removeLast() === frame)
            }
            isInsideAssignmentOrInitializerValue = old
        }
    }

    /** 在普通赋值 RHS 上建立并收集 assignment-local mismatch outcome 的 expected-type 根帧。 */
    @OptIn(PrivateForInline::class)
    fun withAssignmentRhs(
        rootExpression: CfirExpression,
        expectedType: ConeCangJieType?,
        block: () -> Unit,
    ): CfirAssignmentTypeMismatchOutcome? = withExpectedTypeRoot(
        rootExpression = rootExpression,
        expectedType = expectedType,
        capturesAssignmentMismatchOutcome = true,
    ) { frame ->
        block()
        frame?.assignmentMismatchOutcome
    }

    /** 在显式类型初始化器上建立 expected-type 根帧，不把结果写入 assignment 节点。 */
    fun <T> withInitializerExpectedType(
        rootExpression: CfirExpression,
        expectedType: ConeCangJieType,
        block: () -> T,
    ): T = withExpectedTypeRoot(
        rootExpression = rootExpression,
        expectedType = expectedType,
        capturesAssignmentMismatchOutcome = false,
    ) {
        block()
    }

    /** 返回 [expression] 作为当前赋值或初始化器根时的 expected type。 */
    fun expectedTypeForRoot(expression: CfirExpression): ConeCangJieType? {
        val frame = expectedTypeRootStack.lastOrNull() ?: return null
        return frame.expectedType.takeIf { frame.rootExpression === expression }
    }

    /** 返回 [expression] 作为普通赋值 RHS 根时的 expected type。 */
    fun assignmentExpectedTypeForRoot(expression: CfirExpression): ConeCangJieType? {
        val frame = expectedTypeRootStack.lastOrNull() ?: return null
        if (!frame.capturesAssignmentMismatchOutcome) return null
        return frame.expectedType.takeIf { frame.rootExpression === expression }
    }

    /**
     * 记录真正 expected-type 根 owner 已确认的普通赋值 mismatch。
     *
     * 该 sink 不执行 subtype 重算；actual type、基础诊断和根有效性均必须由失效前仍持有
     * 完整语义的 owner 提供。初始化器帧不捕获 assignment outcome，仍由声明 checker 消费实际类型。
     */
    fun recordExpectedTypeRootMismatch(
        expression: CfirExpression,
        actualType: ConeCangJieType,
        primaryDiagnostic: CfirAssignmentTypeMismatchPrimaryDiagnostic,
        rhsRootValidity: CfirAssignmentRhsRootValidity,
    ) {
        val frame = expectedTypeRootStack.lastOrNull() ?: return
        if (!frame.capturesAssignmentMismatchOutcome ||
            frame.rootExpression !== expression ||
            frame.assignmentMismatchOutcome != null
        ) return
        frame.assignmentMismatchOutcome = CfirAssignmentTypeMismatchOutcome(
            expectedType = frame.expectedType,
            actualType = actualType,
            primaryDiagnostic = primaryDiagnostic,
            rhsRootValidity = rhsRootValidity,
        )
    }

    /** 在指定 public API inline 函数上下文中执行 [block]。 */
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

    /** 临时替换返回类型计算器。 */
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

    /** 为 designated local class resolve 临时设置返回类型计算器和目标局部类集合。 */
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

/**
 * body resolve 可切换的 tower data 模式。
 *
 * 不同模式对应 class body 内不同语义位置可见的 receiver 与 scope 集合。
 */
enum class CfirTowerDataMode {
    /** 普通成员和表达式解析模式。 */
    REGULAR,
    /** 嵌套 class-like 声明解析模式。 */
    NESTED_CLASS,
    /** 静态成员解析模式。 */
    STATIC_MEMBER,
    /** 构造器头和主构造器参数默认值解析模式。 */
    CONSTRUCTOR_HEADER,
    /** enum constructor 解析模式。 */
    ENUM_CONSTRUCTOR,
    /** finalizer 解析模式。 */
    FINALIZER,
}

/**
 * 一组按 [CfirTowerDataMode] 区分的 regular tower data contexts。
 *
 * 该类型不可变；切换模式或替换 context 时返回新的实例，避免嵌套解析污染外层状态。
 */
class CfirRegularTowerDataContexts private constructor(
    /** tower data mode 到 context 的映射。 */
    private val modeMap: EnumMap<CfirTowerDataMode, CfirTowerDataContext>,
    /** 主构造器中未提升为属性的参数 scope。 */
    val primaryConstructorPureParametersScope: CfirLocalScope? = null,
    /** 主构造器全部参数 scope。 */
    val primaryConstructorAllParametersScope: CfirLocalScope? = null,
    /** 当前活跃 tower data mode。 */
    val activeMode: CfirTowerDataMode = CfirTowerDataMode.REGULAR,
) {
    /**
     * 从各语义位置的 context 构造 regular tower data contexts。
     *
     * 缺失的专用 context 会回退到 [regular]。
     */
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

    /** 当前活跃模式对应的 tower data context。 */
    val currentContext: CfirTowerDataContext?
        get() = modeMap[activeMode]

    /** 返回切换到 [newMode] 后的新 contexts 对象。 */
    fun replaceTowerDataMode(newMode: CfirTowerDataMode): CfirRegularTowerDataContexts =
        if (newMode == activeMode) this
        else CfirRegularTowerDataContexts(
            modeMap,
            primaryConstructorPureParametersScope,
            primaryConstructorAllParametersScope,
            newMode,
        )

    /** 返回替换当前活跃模式 context 后的新 contexts 对象。 */
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

    /** 返回替换 REGULAR context 并切换到 REGULAR 模式后的新 contexts 对象。 */
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
        /** 构造完整的 mode -> context 映射。 */
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

/**
 * postponed atom 后续恢复所需的上下文快照。
 */
data class CfirPostponedAtomsResolutionContext(
    /** 保存时的 tower data context。 */
    val towerDataContext: CfirTowerDataContext,
    /** 保存时的推断会话。 */
    val inferenceSession: CfirInferenceSession,
)

/**
 * 特殊 postponed atom 的 tower data context 存储。
 *
 * 匿名函数按符号存储，callable reference 按表达式对象身份存储。
 */
class CfirSpecialTowerDataContexts {
    /** 匿名函数符号到 postponed context 的映射。 */
    private val anonymousFunctionContexts = LinkedHashMap<CfirFunctionSymbol<*>, CfirPostponedAtomsResolutionContext>()
    /** callable reference 表达式对象到 postponed context 的映射。 */
    private val callableReferenceContexts = IdentityHashMap<CfirExpression, CfirPostponedAtomsResolutionContext>()

    /** 清空所有特殊 postponed context。 */
    fun clear() {
        anonymousFunctionContexts.clear()
        callableReferenceContexts.clear()
    }

    /** 获取匿名函数保存的 postponed context。 */
    fun getAnonymousFunctionContext(symbol: CfirFunctionSymbol<*>): CfirPostponedAtomsResolutionContext? =
        anonymousFunctionContexts[symbol]

    /** 保存匿名函数 postponed context。 */
    fun storeAnonymousFunctionContext(
        symbol: CfirFunctionSymbol<*>,
        towerDataContext: CfirTowerDataContext,
        inferenceSession: CfirInferenceSession,
    ) {
        anonymousFunctionContexts[symbol] = CfirPostponedAtomsResolutionContext(towerDataContext, inferenceSession)
    }

    /** 删除匿名函数 postponed context。 */
    fun dropAnonymousFunctionContext(symbol: CfirFunctionSymbol<*>) {
        anonymousFunctionContexts.remove(symbol)
    }

    /** 获取 callable reference 保存的 postponed context。 */
    fun getCallableReferenceContext(access: CfirExpression): CfirPostponedAtomsResolutionContext? =
        callableReferenceContexts[access]

    /** 保存 callable reference postponed context。 */
    fun storeCallableReferenceContext(
        access: CfirExpression,
        towerDataContext: CfirTowerDataContext,
        inferenceSession: CfirInferenceSession,
    ) {
        callableReferenceContexts[access] = CfirPostponedAtomsResolutionContext(towerDataContext, inferenceSession)
    }

    /** 删除 callable reference postponed context。 */
    fun dropCallableReferenceContext(access: CfirExpression) {
        callableReferenceContexts.remove(access)
    }

    /** 捕获当前全部特殊 postponed context。 */
    fun capture(): CfirSpecialTowerDataContextsSnapshot = CfirSpecialTowerDataContextsSnapshot(
        anonymousFunctionContexts = LinkedHashMap(anonymousFunctionContexts),
        callableReferenceContexts = IdentityHashMap(callableReferenceContexts),
    )

    /** 从快照恢复特殊 postponed context。 */
    fun restore(snapshot: CfirSpecialTowerDataContextsSnapshot) {
        anonymousFunctionContexts.clear()
        anonymousFunctionContexts.putAll(snapshot.anonymousFunctionContexts)
        callableReferenceContexts.clear()
        callableReferenceContexts.putAll(snapshot.callableReferenceContexts)
    }
}

/** 特殊 postponed context 存储的快照对象。 */
class CfirSpecialTowerDataContextsSnapshot(
    /** 匿名函数 postponed context 快照。 */
    val anonymousFunctionContexts: LinkedHashMap<CfirFunctionSymbol<*>, CfirPostponedAtomsResolutionContext>,
    /** callable reference postponed context 快照。 */
    val callableReferenceContexts: IdentityHashMap<CfirExpression, CfirPostponedAtomsResolutionContext>,
)

/** DFA/CFG 快照恢复时使用的恒等映射器。 */
private object IdentitySnapshotCfirMapper : SnapshotCfirMapper {
    /** 符号快照恢复保持原符号对象。 */
    override fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T = symbol

    /** 元素快照恢复保持原元素对象。 */
    override fun <T : CfirElement> mapElement(element: T): T = element
}

/**
 * body resolve 期间可插拔的调用推断会话。
 *
 * 默认实现不共享约束系统；PCLA 等高级模式通过子类改写候选基础约束和 lambda 完成策略。
 */
abstract class CfirInferenceSession {
    /** 当前会话正在使用的约束系统是否已失败。 */
    open val hasCurrentConstraintContradiction: Boolean
        get() = false

    /** 返回指定候选应复用的基础约束存储。 */
    open fun baseConstraintStorageForCandidate(
        candidate: org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate,
        bodyResolveContext: BodyResolveContext,
    ): ConstraintStorage? = null

    /** 返回指定调用应使用的自定义完成模式；null 表示执行普通 full completion。 */
    open fun customCompletionModeInsteadOfFull(call: org.cangnova.cangjie.cfir.expressions.CfirResolvable): ConstraintSystemCompletionMode? =
        null

    /** 处理一次部分解析调用的回调。 */
    open fun <T> processPartiallyResolvedCall(
        call: T,
        resolutionMode: org.cangnova.cangjie.cfir.resolve.ResolutionMode,
        completionMode: ConstraintSystemCompletionMode,
    ) where T : org.cangnova.cangjie.cfir.expressions.CfirResolvable, T : org.cangnova.cangjie.cfir.expressions.CfirExpression {
    }

    /** 在当前推断会话内执行 lambda completion。 */
    open fun runLambdaCompletion(
        candidate: org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate,
        forOverloadByLambdaReturnType: Boolean,
        block: () -> Unit,
    ): ConstraintStorage? {
        block()
        return null
    }

    /** 在兼容时向指定系统添加 subtype 约束。 */
    open fun addSubtypeConstraintIfCompatible(
        lowerType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        upperType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
    ) {
    }

    /** 在兼容时向指定系统添加 subtype 约束。 */
    open fun addSubtypeConstraintIfCompatible(
        lowerType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        upperType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        system: ConstraintSystemImpl,
    ) {
        addSubtypeConstraintIfCompatible(lowerType, upperType)
    }

    /** 在当前推断会话中注册结构化表达式推断产生的类型变量。 */
    open fun registerInferenceVariable(variable: TypeVariableMarker) {
    }

    /**
     * 收窄 fresh lambda receiver 的成员 owner 候选集合。
     *
     * 默认推断会话不维护跨表达式候选集合；PCLA 会话用它模拟官方 `TryInferFromSyntaxInfo`
     * 中同一 lambda 参数成员候选集合的交集。
     */
    open fun refineFreshReceiverCandidateOwners(
        receiverTypeConstructor: TypeConstructorMarker,
        ownerTypes: List<ConeCangJieType>,
    ): Set<ConeCangJieType>? = null

    /**
     * fresh lambda 形参在当前会话中的已知类型界。
     *
     * 官方 `ChkLambda` 逐语句分析，前序语句的调用完成即固定形参类型，后续成员访问
     * 按已固定的具体类型解析。CFIR 的 PCLA_POSTPONED_CALL 把参数检查推迟到 body
     * 转换之后，前序约束不会出现在共享系统的即时视图里；PCLA 会话通过读取已排队
     * postponed 调用的参数映射补足同一信息（如 `getB19(x)` 的映射携带 `x -> B19`），
     * 使后续 `x.foo19(y)` 的 owner 收窄与官方一致。
     */
    open fun knownBoundsForFreshReceiver(
        receiverTypeConstructor: TypeConstructorMarker,
    ): List<ConeCangJieType> = emptyList()

    companion object {
        /** 默认推断会话，不改变普通调用解析行为。 */
        val DEFAULT: CfirInferenceSession = object : CfirInferenceSession() {}

        /** 基于外层系统构造一个共享基础约束系统。 */
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

/**
 * PCLA 推断会话。
 *
 * 该会话维护一个与外层候选共享的 common constraint system，
 * 让 postponed call / lambda body 能在候选试跑期间累积并最终提交约束。
 */
class CfirPCLAInferenceSession(
    /** 外层候选，最终会接收 PCLA 推断结果。 */
    private val outerCandidate: Candidate,
    /** 构造约束系统和访问会话类型上下文所需的推断组件。 */
    private val inferenceComponents: InferenceComponents,
) : CfirInferenceSession() {
    /** 当前 PCLA 共享 common constraint system。 */
    private var currentCommonSystem: ConstraintSystemImpl = prepareSharedBaseSystem(outerCandidate.system, inferenceComponents)
    /** 当前 PCLA common system 是否已经出现约束矛盾。 */
    override val hasCurrentConstraintContradiction: Boolean
        get() = currentCommonSystem.hasContradiction
    /** fresh lambda receiver 对应的候选 owner 集合。 */
    private val freshReceiverCandidateOwnersByTypeVariable =
        linkedMapOf<TypeConstructorMarker, MutableList<ConeCangJieType>>()

    /** 返回当前候选应使用的共享基础约束存储。 */
    override fun baseConstraintStorageForCandidate(
        candidate: Candidate,
        bodyResolveContext: BodyResolveContext,
    ): ConstraintStorage? {
        if (candidate.mightBeAnalyzedAndCompletedIndependently(bodyResolveContext)) return null
        return currentCommonSystem.currentStorage()
    }

    /** 对使用外层约束系统的 postponed call 改用 PCLA postponed completion。 */
    override fun customCompletionModeInsteadOfFull(
        call: CfirResolvable,
    ): ConstraintSystemCompletionMode? = when {
        call.candidate()?.usedOuterCs == true -> ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL
        else -> null
    }

    /** 处理部分解析调用，并在需要时把候选系统写回 common system。 */
    override fun <T> processPartiallyResolvedCall(
        call: T,
        resolutionMode: org.cangnova.cangjie.cfir.resolve.ResolutionMode,
        completionMode: ConstraintSystemCompletionMode,
    ) where T : CfirResolvable, T : CfirExpression {
        call.updateReturnTypeWithCurrentSubstitutor(resolutionMode)

        val candidate = call.candidate()
        if (candidate?.usedOuterCs != true) return

        currentCommonSystem.replaceContentWith(candidate.system.currentStorage())
        candidate.freshReceiverConstraintToDrop?.let { constraintToDrop ->
            currentCommonSystem.removeConstraintsForVariable(constraintToDrop.receiverTypeConstructor) { constraint ->
                val position = constraint.position.from
                position is ReceiverConstraintPosition<*> &&
                    position.argument === constraintToDrop.receiverExpression
            }
            candidate.freshReceiverConstraintToDrop = null
        }

        if (completionMode == ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL) {
            outerCandidate.postponedPCLACalls += ConeAtomWithCandidate(call, candidate)
        }
    }

    /** 在 PCLA 共享系统或候选系统中运行 lambda completion。 */
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

    /** 临时切换当前 common system 执行 [block]。 */
    private fun <T> runWithSpecifiedCurrentCommonSystem(newSystem: ConstraintSystemImpl, block: () -> T): T {
        val previous = currentCommonSystem
        return try {
            currentCommonSystem = newSystem
            block()
        } finally {
            currentCommonSystem = previous
        }
    }

    /** 将 PCLA common system 的最终结果提交回外层候选。 */
    fun applyResultsToMainCandidate() {
        outerCandidate.system.replaceContentWith(currentCommonSystem.currentStorage())
    }

    /**
     * 从已排队 postponed 调用中提取 fresh 形参的已知类型界。
     *
     * 每个排队调用的参数映射里，凡以该 receiver 变量为实参的参数位都给出一个
     * `变量 <: 参数类型` 上界（按该候选自己的当前替换结果换算）；候选系统内已记录
     * 的显式约束与固定解也一并返回。
     */
    override fun knownBoundsForFreshReceiver(
        receiverTypeConstructor: TypeConstructorMarker,
    ): List<ConeCangJieType> {
        val bounds = mutableListOf<ConeCangJieType>()
        for (atom in outerCandidate.postponedPCLACalls.filterIsInstance<ConeAtomWithCandidate>()) {
            val candidate = atom.candidate
            if (candidate.argumentMappingInitialized) {
                val substitutor = candidate.system.currentStorage()
                    .buildCurrentSubstitutor(inferenceComponents.session.typeContext, emptyMap())
                    .asCone()
                for ((argument, parameter) in candidate.argumentMapping) {
                    val argumentType = argument.expression.coneTypeOrNull as? ConeTypeVariableType ?: continue
                    if (argumentType.typeConstructor != receiverTypeConstructor) continue
                    val parameterType = parameter.returnTypeRef.coneTypeOrNull?.let { declared ->
                        substitutor.substituteOrNull(declared) ?: declared
                    } ?: continue
                    if (parameterType is ConeErrorType) continue
                    bounds += parameterType
                }
            }

            val constraints = candidate.system.currentStorage()
                .notFixedTypeVariables[receiverTypeConstructor]?.constraints.orEmpty()
            for (constraint in constraints) {
                if (constraint.kind != ConstraintKind.UPPER &&
                    constraint.kind != ConstraintKind.LOWER &&
                    constraint.kind != ConstraintKind.EQUALITY
                ) continue
                val constraintType = constraint.type as? ConeCangJieType ?: continue
                if (constraintType is ConeErrorType) continue
                bounds += constraintType
            }
            (candidate.system.currentStorage().fixedTypeVariables[receiverTypeConstructor]
                as? ConeCangJieType)?.let { fixedType ->
                if (fixedType !is ConeErrorType) bounds += fixedType
            }
        }
        return bounds.distinctBy { it.toString() }
    }

    /** 把 expected-type subtype 约束加入当前 common system。 */
    override fun addSubtypeConstraintIfCompatible(
        lowerType: ConeCangJieType,
        upperType: ConeCangJieType,
    ) {
        currentCommonSystem.addSubtypeConstraintIfCompatible(
            lowerType,
            upperType,
            ConeExpectedTypeConstraintPosition,
        )
    }

    /** 把 PCLA body 内部新建的结构化推断变量纳入当前 common system。 */
    override fun registerInferenceVariable(variable: TypeVariableMarker) {
        currentCommonSystem.registerVariable(variable)
    }

    /**
     * 按官方 `SynLamExpr` 的语法候选收窄规则维护 fresh receiver owner 集合。
     *
     * 同一 placeholder 上多次成员访问时，候选 owner 取交集；若交集为空，保留当前访问集合，
     * 后续由普通解析诊断负责报告真实歧义或不可推断。
     */
    override fun refineFreshReceiverCandidateOwners(
        receiverTypeConstructor: TypeConstructorMarker,
        ownerTypes: List<ConeCangJieType>,
    ): Set<ConeCangJieType>? {
        val distinctOwnerTypes = ownerTypes.distinctByConeType()
        if (distinctOwnerTypes.isEmpty()) return null

        val previous = freshReceiverCandidateOwnersByTypeVariable[receiverTypeConstructor]
        val refined = when (previous) {
            null -> distinctOwnerTypes
            else -> distinctOwnerTypes.filter { ownerType ->
                previous.any { previousOwner -> previousOwner.isSameConeType(ownerType) }
            }.ifEmpty { distinctOwnerTypes }
        }

        freshReceiverCandidateOwnersByTypeVariable[receiverTypeConstructor] = refined.toMutableList()
        addFreshReceiverOwnerConstraintIfSingle(receiverTypeConstructor, refined)
        return refined.toSet()
    }

    /**
     * owner 候选集合收敛到单一类型时，把它转成真正的 PCLA 约束。
     *
     * 官方 `TryEnforceCandidate` 会在能确定唯一 owner 构造器时约束 placeholder；
     * CFIR 的在线成员候选规约在多候选阶段只维护 owner-sum，直到交集变为单一候选再
     * 写入 common system，避免第一条成员访问过早把等价候选中的代表类型固定下来。
     */
    private fun addFreshReceiverOwnerConstraintIfSingle(
        receiverTypeConstructor: TypeConstructorMarker,
        ownerTypes: List<ConeCangJieType>,
    ) {
        val ownerType = ownerTypes.singleOrNull() ?: return
        val receiverType = (currentCommonSystem.currentStorage().allTypeVariables[receiverTypeConstructor] as? ConeTypeVariable)
            ?.defaultType
            ?: return
        currentCommonSystem.addSubtypeConstraintIfCompatible(
            receiverType,
            ownerType,
            ConeExpectedTypeConstraintPosition,
        )
    }

    /** 按当前 session 类型等价关系去重，避免类型实参结构相同但对象不同导致集合无法相交。 */
    private fun List<ConeCangJieType>.distinctByConeType(): List<ConeCangJieType> {
        val result = mutableListOf<ConeCangJieType>()
        for (type in this) {
            if (result.none { existing -> existing.isSameConeType(type) }) {
                result += type
            }
        }
        return result
    }

    /** 判断两个 Cone 类型是否为同一个 owner 候选。 */
    private fun ConeCangJieType.isSameConeType(other: ConeCangJieType): Boolean =
        AbstractTypeChecker.equalTypes(inferenceComponents.session.typeContext, this, other)

    /** 使用当前约束系统 substitutor 更新表达式返回类型。 */
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

    /** 从可解析表达式上读取当前候选。 */
    private fun CfirResolvable.candidate(): Candidate? =
        (calleeReference as? CfirNamedReferenceWithCandidate)?.candidate

    /** 判断候选是否可以脱离 PCLA common system 独立分析并完成。 */
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

    /** 判断表达式是否是不会向 PCLA common system 注入新约束的简单实参。 */
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

    /** 判断接收者表达式是否仍依赖 postponed 或未固定类型变量。 */
    private fun CfirExpression.isReceiverPostponed(): Boolean {
        return when {
            coneTypeOrNull?.containsNotFixedTypeVariables() == true -> true
            (this as? CfirResolvable)?.candidate()?.usedOuterCs == true -> true
            else -> false
        }
    }

    /** 判断类型中是否包含当前 common system 尚未固定的类型变量。 */
    private fun ConeCangJieType.containsNotFixedTypeVariables(): Boolean =
        contains {
            it is ConeTypeVariableType && it.typeConstructor in currentCommonSystem.allTypeVariables
        }
}

/** 把普通 scope 包装成 tower data element。 */
private fun CfirScope.asTowerDataElement(isLocal: Boolean): CfirTowerDataElement =
    CfirTowerDataElement(scope = this, implicitReceiver = null, isLocal = isLocal)
