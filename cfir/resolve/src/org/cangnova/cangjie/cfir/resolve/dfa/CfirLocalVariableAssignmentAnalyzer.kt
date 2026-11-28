package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirFunctionBodyOwner
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.defaultValueMetadata
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirAugmentedAssignment
import org.cangnova.cangjie.cfir.expressions.CfirIncrementDecrementExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.CfirIterationOwner
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.unwrapAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.patterns.visibleBindingVariables
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.body.SnapshotCfirMapper
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 局部变量赋值分析器（CFIR 版），结构对位 Kotlin FIR `FirLocalVariableAssignmentAnalyzer`。
 *
 * 该组件只基于仓颉真实 AST 做“语法级”赋值流跟踪，用于回答：
 * 当前作用域中的某个局部可变变量访问是否已不再稳定（不可用于可靠 smart-cast）。
 *
 * 与 Kotlin FIR 对位约束：
 * - 保持同构状态：`rootSymbol` / `assignedLocalVariablesByDeclaration` / `variableAssignments` / `scopes` / `postponedLambdas`
 * - 保持同构内部结构：`Fork` / `Assignment` / `VariableAssignments` / `MiniFlow` / `MiniCfgBuilder`
 * - 不引入 Kotlin-only 节点语义；仅适配 CFIR 真实节点
 */
class CfirLocalVariableAssignmentAnalyzer private constructor(
    /**
     * 当前分析根声明（包含代码块）的符号。
     */
    private var rootSymbol: CfirBasedSymbol<*>?,
    /** 根声明中每个局部声明或循环对应的赋值分叉信息缓存。 */
    private var assignedLocalVariablesByDeclaration: Map<Any /* CfirBasedSymbol<*> | CfirLoopExpression */, Fork>?,
    /** 每个局部 callable 声明对应的赋值记录列表。 */
    private var variableAssignments: Map<CfirCallableDeclaration, List<Assignment>>?,
    /** 当前词法/控制流作用域栈，保存 scope 入口分叉与被禁止 smart-cast 的赋值集合。 */
    private val scopes: Stack<Pair<Fork?, VariableAssignments>>,
    // 与 CFG builder 的 postponed lambda 拓扑保持同构。
    /** 当前函数调用链路中尚未按调用完成结果落地的 lambda 分叉。 */
    private val postponedLambdas: Stack<MutableMap<Fork, Boolean /* data-flow only */>>,
    /** 解析态复制在实际词法宿主图中使用显式 old→new 语义视图，不改写宿主源码或绑定。 */
    private val projectionMapper: SnapshotCfirMapper?,
) {
    constructor(projectionMapper: SnapshotCfirMapper? = null) : this(
        rootSymbol = null,
        assignedLocalVariablesByDeclaration = null,
        variableAssignments = null,
        scopes = stackOf(),
        postponedLambdas = stackOf(),
        projectionMapper = projectionMapper,
    )

    /** 赋值分析器的声明/循环键、fork 与未完成作用域同样属于保留快照，不借助反射或 mini-CFG 重建。 */
    fun acceptSourceChildren(visitor: DfaSourceVisitor) {
        val forks = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Fork, Boolean>())
        fun fork(value: Fork?) {
            if (value != null && forks.add(value)) {
                value.assignedLater.acceptSourceChildren(visitor)
                value.assignedInside.acceptSourceChildren(visitor)
            }
        }
        rootSymbol?.let { visitor.visitElement(it.cfir) }
        assignedLocalVariablesByDeclaration?.forEach { (owner, value) ->
            when (owner) {
                is CfirBasedSymbol<*> -> visitor.visitElement(owner.cfir)
                is CfirElement -> visitor.visitElement(owner)
                else -> error("Unexpected local assignment owner: ${owner::class.simpleName}")
            }
            fork(value)
        }
        variableAssignments?.forEach { (owner, assignments) ->
            visitor.visitElement(owner)
            assignments.forEach { it.acceptSourceChildren(visitor) }
        }
        scopes.all().forEach { (value, assignments) ->
            fork(value)
            assignments.acceptSourceChildren(visitor)
        }
        postponedLambdas.all().forEach { lambdas -> lambdas.keys.forEach(::fork) }
    }

    /**
     * 深拷贝当前分析器状态，并对快照中涉及到的 symbol/element 做 mapper 映射。
     */
    @CfgInternals
    internal fun createSnapshot(firMapper: SnapshotCfirMapper): CfirLocalVariableAssignmentAnalyzer {
        // 同一写事件同时出现在全局索引和多个 fork/scope 集合；本次快照只克隆一次。
        val assignmentCopies = java.util.IdentityHashMap<Assignment, Assignment>()
        val forkCopies = java.util.IdentityHashMap<Fork, Fork>()
        val setCopies = java.util.IdentityHashMap<VariableAssignments, VariableAssignments>()
        fun cloneAssignment(value: Assignment): Assignment = assignmentCopies.getOrPut(value) {
            value.createSnapshot(firMapper)
        }
        /**
         * 深拷贝状态对象，并把其中的 CFIR symbol/element 映射到快照树。
         */
        fun <T> clone(value: T): T {
            @Suppress("UNCHECKED_CAST")
            return when (value) {
                is CfirBasedSymbol<*> -> firMapper.mapSymbol(value) as T
                is CfirElement -> firMapper.mapElement(value) as T
                is Fork -> forkCopies.getOrPut(value) { value.createSnapshot(firMapper, ::cloneAssignment) } as T
                is VariableAssignments -> setCopies.getOrPut(value) { value.createSnapshot(firMapper, ::cloneAssignment) } as T
                is Pair<*, *> -> Pair(clone(value.first), clone(value.second)) as T
                is List<*> -> value.map { clone(it) } as T
                is Map<*, *> -> buildMap {
                    value.forEach { (k, v) -> put(clone(k), clone(v)) }
                } as T
                is Stack<*> -> value.createSnapshot { clone(it) } as T
                is Assignment -> cloneAssignment(value) as T
                is Boolean, null -> value
                else -> error("Unexpected key type: ${value::class.simpleName}")
            }
        }

        return CfirLocalVariableAssignmentAnalyzer(
            rootSymbol = clone(rootSymbol),
            assignedLocalVariablesByDeclaration = clone(assignedLocalVariablesByDeclaration),
            variableAssignments = clone(variableAssignments),
            scopes = clone(scopes),
            postponedLambdas = clone(postponedLambdas),
            // 首次换代安装本次映射；已有源→有效树投影时接上有效树→快照树映射。
            projectionMapper = projectionMapper?.let { previousMapper ->
                object : SnapshotCfirMapper {
                    override fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T =
                        firMapper.mapSymbol(previousMapper.mapSymbol(symbol))
                    override fun <T : CfirElement> mapElement(element: T): T =
                        firMapper.mapElement(previousMapper.mapElement(element))
                }
            } ?: firMapper,
        )
    }

    /**
     * 清空当前分析根、缓存和作用域栈。
     */
    fun reset() {
        rootSymbol = null
        assignedLocalVariablesByDeclaration = null
        variableAssignments = null
        postponedLambdas.reset()
        scopes.reset()
    }

    /**
     * 判断当前作用域内某个局部变量访问是否不稳定。
     */
    fun isUnstableInCurrentScope(declaration: CfirDeclaration, types: Set<ConeCangJieType>?, session: CfirSession): Boolean {
        if (assignedLocalVariablesByDeclaration == null) return false
        val callable = (projectionMapper?.mapElement(declaration) ?: declaration) as? CfirCallableDeclaration ?: return false
        if (!callable.isLocal || !callable.isMutableLocalVariable()) return false
        return !allAssignmentsPreserveType(scopes.top().second[callable], types, session) || postponedLambdas.all().any { lambdas ->
            lambdas.any { (lambda, dataFlowOnly) -> dataFlowOnly && callable in lambda.assignedInside }
        }
    }

    /**
     * 判断 callable 声明是否是可被重新赋值的局部变量。
     */
    private fun CfirCallableDeclaration.isMutableLocalVariable(): Boolean {
        return when (this) {
            is CfirProperty -> status.isMut
            is CfirVariable -> isVar
            else -> false
        }
    }

    /**
     * 判断当前已知赋值是否都保持在 smart-cast 类型集合以内。
     */
    private fun allAssignmentsPreserveType(
        assignments: Set<Assignment>?,
        types: Set<ConeCangJieType>?,
        session: CfirSession,
    ): Boolean {
        return assignments.isNullOrEmpty() || (
            types != null &&
                assignments.all { it.type != null } &&
                assignments.all { assignment ->
                    types.all { AbstractTypeChecker.isSubtypeOf(session.typeContext, assignment.type!!, it) == true }
                }
            )
    }

    /**
     * 查询某个声明或循环在当前根声明 mini CFG 中的分叉信息。
     */
    private fun getInfoForDeclaration(symbol: Any): Fork? {
        val root = rootSymbol ?: return null
        val effectiveOwner = when (symbol) {
            is CfirBasedSymbol<*> -> projectionMapper?.mapSymbol(symbol) ?: symbol
            is CfirElement -> projectionMapper?.mapElement(symbol) ?: symbol
            else -> error("Unexpected local assignment owner: ${symbol::class.simpleName}")
        }
        if (root == effectiveOwner) return null
        val cachedMap = buildInfoForRoot(root)
        return cachedMap[effectiveOwner]
    }

    /**
     * 为根声明构造并缓存 mini CFG 赋值信息。
     */
    private fun buildInfoForRoot(root: CfirBasedSymbol<*>): Map<Any, Fork> {
        assignedLocalVariablesByDeclaration?.let { return it }

        val data = MiniCfgBuilder.MiniCfgData()
        MiniCfgBuilder(projectionMapper).visitElement(root.cfir, data)

        assignedLocalVariablesByDeclaration = data.forks
        variableAssignments = data.assignments
        return data.forks
    }

    /**
     * 进入一个新的分析作用域。
     *
     * 非原地求值的作用域会把内部赋值传播到外层 prohibition 集合，并把外层后续赋值传播回当前作用域。
     */
    private fun enterScope(symbol: Any, evaluatedInPlace: Boolean): Pair<Fork?, VariableAssignments> {
        val currentInfo = getInfoForDeclaration(symbol)
        val prohibitInThisScope = scopes.top().second.copy()
        scopes.push(currentInfo to prohibitInThisScope)
        if (!evaluatedInPlace) {
            for ((outerInfo, prohibitInOuterScope) in scopes.all()) {
                prohibitInOuterScope.merge(currentInfo?.assignedInside)
                prohibitInThisScope.merge(outerInfo?.assignedLater)
            }
        }
        return scopes.top()
    }

    /**
     * 进入函数分析作用域，并返回函数入口处已在内部赋值的符号集合。
     */
    fun enterFunction(function: CfirFunctionBodyOwner): Set<CfirBasedSymbol<*>> {
        if (rootSymbol == null) {
            rootSymbol = projectionMapper?.mapSymbol(function.symbol) ?: function.symbol
            scopes.push(null to VariableAssignments())
            return emptySet()
        }
        val (info, prohibitSmartCasts) = enterScope(
            symbol = function.symbol,
            // calls-in-place / contract 仓颉承载层暂缺，未知时保持 postponed 拓扑，不做“已 in-place”假设。
            evaluatedInPlace = false,
        )
        for (concurrentLambdas in postponedLambdas.all()) {
            for ((otherLambda, dataFlowOnly) in concurrentLambdas) {
                if (!dataFlowOnly && otherLambda != info) {
                    prohibitSmartCasts.merge(otherLambda.assignedInside)
                }
            }
        }
        return scopes.top().first?.assignedInside?.getAssignedSymbols().orEmpty()
    }

    /**
     * 返回局部 callable 直接写入的外层局部声明。
     *
     * mini CFG 在进入局部函数或 lambda 时只保留词法外层声明，因此这里暴露的集合天然是
     * callable write effect，而不是函数自身局部变量的普通赋值。调用图的传递闭包由消费方
     * 按实际 resolved call 计算，避免把“声明了闭包”误当成“已经执行闭包”。
     */
    internal fun assignedOuterLocals(function: CfirFunction): Set<CfirCallableDeclaration> =
        getInfoForDeclaration(function.symbol)
            ?.assignedInside
            ?.getAssignedDeclarations()
            .orEmpty()

    /**
     * 退出当前函数作用域。
     */
    fun exitFunction() {
        scopes.pop()
        if (scopes.isEmpty) {
            rootSymbol = null
            assignedLocalVariablesByDeclaration = null
            variableAssignments = null
        }
    }

    /**
     * 进入代码片段时按需建立新的顶层根作用域。
     */
    fun enterCodeFragment(codeFragment: CfirCodeFragment) {
        enterNewTopLevelScopeIfNeeded(codeFragment)
    }

    /**
     * 退出代码片段时按需清理顶层根作用域。
     */
    fun exitCodeFragment(codeFragment: CfirCodeFragment) {
        exitNewTopLevelScopeIfNeeded(codeFragment)
    }

    /**
     * 当前没有根声明时，把给定声明作为新的顶层分析根。
     */
    private fun enterNewTopLevelScopeIfNeeded(declaration: CfirDeclaration) {
        if (rootSymbol != null) return
        rootSymbol = projectionMapper?.mapSymbol(declaration.symbol) ?: declaration.symbol
        scopes.push(null to VariableAssignments())
    }

    /**
     * 当前根声明正是给定声明时，退出并清空顶层分析根。
     */
    private fun exitNewTopLevelScopeIfNeeded(declaration: CfirDeclaration) {
        if (rootSymbol == (projectionMapper?.mapSymbol(declaration.symbol) ?: declaration.symbol)) {
            rootSymbol = null
            scopes.pop()
            assignedLocalVariablesByDeclaration = null
            variableAssignments = null
        }
    }

    /**
     * 进入 class 作用域。
     */
    fun enterClass(klass: CfirClass) {
        if (rootSymbol == null) return
        enterScope(klass.symbol, evaluatedInPlace = false)
    }

    /**
     * 退出 class 作用域。
     */
    fun exitClass() {
        if (rootSymbol == null) return
        scopes.pop()
    }

    /**
     * 进入函数调用，记录当前调用的 lambda 参数分叉。
     */
    fun enterFunctionCall(lambdaArgs: Collection<CfirAnonymousFunction>) {
        if (rootSymbol == null) return
        postponedLambdas.push(lambdaArgs.mapNotNull { getInfoForDeclaration(it.symbol) }.associateWithTo(mutableMapOf()) { false })
    }

    /**
     * 退出函数调用；未完成调用的 lambda 会作为 data-flow-only postponed lambda 传播到外层。
     */
    fun exitFunctionCall(callCompleted: Boolean) {
        if (rootSymbol == null) return
        val lambdasInCall = postponedLambdas.pop()
        if (!callCompleted) {
            lambdasInCall.keys.associateWithTo(postponedLambdas.topOrNull() ?: return) { true }
        }
    }

    /**
     * 进入循环作用域，并返回循环内部赋值的符号集合。
     */
    fun enterLoop(loop: CfirIterationOwner): Set<CfirBasedSymbol<*>> {
        if (rootSymbol == null) return emptySet()
        val (info, _) = enterScope(loop, evaluatedInPlace = true)
        return info?.assignedInside?.getAssignedSymbols().orEmpty()
    }

    /**
     * 退出循环作用域，并返回循环内部赋值的符号集合。
     */
    fun exitLoop(): Set<CfirBasedSymbol<*>> {
        if (rootSymbol == null) return emptySet()
        val (info, _) = scopes.pop()
        return info?.assignedInside?.getAssignedSymbols().orEmpty()
    }

    /** 保留 For 的初始化和重复入口分离；阶段消费者查询同一 mini 已完成事实，不另存符号表。 */
    fun assignedInsideLoop(loop: CfirIterationOwner): Set<CfirBasedSymbol<*>> =
        getInfoForDeclaration(loop)?.assignedInside?.getAssignedSymbols().orEmpty()

    /**
     * 记录某个局部 callable 的一次真实赋值类型。
     */
    internal fun visitAssignment(declaration: CfirCallableDeclaration, occurrence: AssignmentOccurrence, type: ConeCangJieType) {
        buildInfoForRoot(rootSymbol ?: return)
        val effectiveDeclaration = projectionMapper?.mapElement(declaration) ?: declaration
        val assignments = variableAssignments?.get(effectiveDeclaration) ?: return
        val effectiveOwner = projectionMapper?.mapElement(occurrence.owner) ?: occurrence.owner
        assignments.filter { it.occurrence == occurrence.withOwner(effectiveOwner) }.forEach { it.type = type }
    }

    /** 正常降低保留同一个写事件，把 raw owner 显式换为实际 resolved owner。 */
    fun rebindAssignment(original: CfirElement, replacement: CfirElement) {
        buildInfoForRoot(rootSymbol ?: return)
        val oldOwner = projectionMapper?.mapElement(original) ?: original
        val newOwner = projectionMapper?.mapElement(replacement) ?: replacement
        variableAssignments?.values?.forEach { assignments ->
            assignments.forEach { assignment ->
                if (assignment.occurrence.owner === oldOwner) {
                    assignment.occurrence = assignment.occurrence.withOwner(newOwner)
                }
            }
        }
    }

    companion object {
        /**
         * mini CFG 中某个声明或循环的赋值分叉信息。
         */
        private class Fork(
            /** 分叉之后仍会发生的赋值集合。 */
            val assignedLater: VariableAssignments,
            /** 分叉内部发生的赋值集合。 */
            val assignedInside: VariableAssignments,
        ) {
            /**
             * 为 fork 创建快照副本。
             */
            @CfgInternals
            fun createSnapshot(firMapper: SnapshotCfirMapper, cloneAssignment: (Assignment) -> Assignment): Fork {
                return Fork(assignedLater.createSnapshot(firMapper, cloneAssignment), assignedInside.createSnapshot(firMapper, cloneAssignment))
            }
        }

        /**
         * 单次赋值记录。
         */
        private class Assignment(
            /** 真实写入的节点与槽位身份；正常降低只显式重绑 owner。 */
            var occurrence: AssignmentOccurrence,
            /** 是否来自复合/操作符赋值。 */
            val operatorAssignment: Boolean,
            /** 赋值右侧推断出的类型；mini CFG 构造阶段可暂为空。 */
            var type: ConeCangJieType? = null,
        ) {
            /**
             * 创建赋值记录副本。
             */
            fun createSnapshot(firMapper: SnapshotCfirMapper): Assignment =
                Assignment(occurrence.withOwner(firMapper.mapElement(occurrence.owner)), operatorAssignment, type)

            fun acceptSourceChildren(visitor: DfaSourceVisitor) {
                visitor.visitElement(occurrence.owner)
                type?.let(visitor::visitType)
            }
        }

        /**
         * 局部 callable 到赋值集合的映射。
         */
        private class VariableAssignments {
            /** 按局部声明聚合的赋值记录。 */
            private val assignments: MutableMap<CfirCallableDeclaration, MutableSet<Assignment>> = mutableMapOf()

            fun acceptSourceChildren(visitor: DfaSourceVisitor) {
                assignments.forEach { (owner, values) ->
                    visitor.visitElement(owner)
                    values.forEach { it.acceptSourceChildren(visitor) }
                }
            }

            /**
             * 查询声明对应的赋值集合。
             */
            operator fun get(declaration: CfirCallableDeclaration): Set<Assignment>? = assignments[declaration]

            /**
             * 判断声明是否已有赋值记录。
             */
            operator fun contains(declaration: CfirCallableDeclaration): Boolean = declaration in assignments

            /**
             * 添加一次赋值记录。
             */
            fun add(declaration: CfirCallableDeclaration, assignment: Assignment): Boolean {
                return assignments.getOrPut(declaration) { mutableSetOf() }.add(assignment)
            }

            /**
             * 创建赋值映射快照。
             */
            @CfgInternals
            fun createSnapshot(firMapper: SnapshotCfirMapper, cloneAssignment: (Assignment) -> Assignment): VariableAssignments {
                val copy = VariableAssignments()
                for ((key, value) in assignments) {
                    copy.assignments[firMapper.mapElement(key)] = value.mapTo(mutableSetOf(), cloneAssignment)
                }
                return copy
            }

            /**
             * 创建浅拷贝，复用赋值记录对象。
             */
            fun copy(): VariableAssignments {
                val copy = VariableAssignments()
                copy.assignments += assignments
                return copy
            }

            /**
             * 合并另一组赋值记录。
             */
            fun merge(other: VariableAssignments?): Boolean {
                if (other == null || other.assignments.isEmpty()) return false
                var modified = false
                for ((declaration, values) in other.assignments) {
                    modified = modified or assignments.getOrPut(declaration) { mutableSetOf() }.addAll(values)
                }
                return modified
            }

            /**
             * 只保留给定声明集合内的赋值记录。
             */
            fun retain(declarations: Set<CfirCallableDeclaration>) {
                assignments.keys.retainAll(declarations)
            }

            /**
             * 返回发生过非操作符赋值的局部声明符号。
             */
            fun getAssignedSymbols(): Set<CfirBasedSymbol<*>> {
                return assignments.entries
                    .filter { (_, values) -> values.any { !it.operatorAssignment } }
                    .mapTo(mutableSetOf()) { (declaration, _) -> declaration.symbol }
            }

            /** 返回本赋值集合中的全部局部声明。 */
            fun getAssignedDeclarations(): Set<CfirCallableDeclaration> = assignments.keys
        }

        /**
         * mini CFG 中的轻量控制流节点。
         */
        private class MiniFlow(val parents: Set<MiniFlow>) {
            /** 从该节点向后可见的赋值集合。 */
            val assignedLater = VariableAssignments()

            /**
             * 创建以当前节点为父节点的新分叉。
             */
            fun fork(): MiniFlow = MiniFlow(setOf(this))

            /**
             * mini flow 工厂。
             */
            companion object {
                /** 创建无父节点的入口 flow。 */
                fun start() = MiniFlow(emptySet())
            }
        }

        /**
         * 语法级 mini CFG 构造器：
         * - 仅抽取“局部声明 + 赋值 + 分支/循环/异常/调用参数顺序”这些对稳定性判定必要的信息；
         * - 节点选择与遍历顺序对位 Kotlin FIR 的 `MiniCfgBuilder`，但仅使用仓颉真实节点。
         */
        private class MiniCfgBuilder(private val projectionMapper: SnapshotCfirMapper?) : CfirVisitor<Unit, MiniCfgBuilder.MiniCfgData>() {
            /** 所有子边在 dispatch 之前投影；借用边由调用方的显式映射协议保留。 */
            private val sourceVisitor = object : CfirVisitor<Unit, MiniCfgData>() {
                override fun visitElement(element: CfirElement, data: MiniCfgData) {
                    val projected = if (projectionMapper == null) element else projectionMapper.mapElement(element)
                    projected.accept(this@MiniCfgBuilder, data)
                }
            }

            /**
             * 默认递归访问子节点。
             */
            override fun visitElement(element: CfirElement, data: MiniCfgData) {
                element.acceptChildren(sourceVisitor, data)
            }

            /**
             * 在新的词法作用域中访问元素并返回该作用域内自由变量赋值集合。
             */
            private fun visitElementWithLexicalScope(element: CfirElement, data: MiniCfgData): VariableAssignments {
                val flow = MiniFlow.start()
                val freeVariables = data.variableDeclarations.flatMapTo(mutableSetOf()) { it.values }
                data.flow = flow
                element.acceptChildren(sourceVisitor, data)
                return flow.assignedLater.apply { retain(freeVariables) }
            }

            /**
             * 匿名函数作为局部声明处理。
             */
            override fun visitAnonymousFunction(anonymousFunction: CfirAnonymousFunction, data: MiniCfgData) =
                visitLocalDeclaration(anonymousFunction, data)

            /**
             * 函数作为局部声明处理。
             */
            override fun visitFunction(function: CfirFunction, data: MiniCfgData) =
                run {
                    function.valueParameters.mapNotNull { it.defaultValueMetadata?.function }.forEach {
                        it.accept(sourceVisitor, data)
                    }
                    visitLocalDeclaration(function, data)
                }

            /** generated named 入口不委托 visitFunction；与固定 Kotlin 一样显式建立函数 fork。 */
            override fun visitNamedFunction(namedFunction: CfirNamedFunction, data: MiniCfgData) =
                visitFunction(namedFunction, data)

            override fun visitEnumConstructor(enumConstructor: CfirEnumConstructor, data: MiniCfgData) {
                if (enumConstructor.body != null) visitLocalDeclaration(enumConstructor, data)
            }

            /**
             * class 作为局部声明处理。
             */
            override fun visitClass(klass: CfirClass, data: MiniCfgData) =
                visitLocalDeclaration(klass, data)

            /**
             * code fragment 作为局部声明处理。
             */
            override fun visitCodeFragment(codeFragment: CfirCodeFragment, data: MiniCfgData) =
                visitLocalDeclaration(codeFragment, data)

            /**
             * 访问局部声明，记录其内部赋值与后续赋值分叉。
             */
            private fun visitLocalDeclaration(declaration: CfirDeclaration, data: MiniCfgData) {
                val flow = data.flow
                val assignedInside = visitElementWithLexicalScope(declaration, data)
                flow.recordAssignments(assignedInside)
                data.flow = flow.fork()
                data.forks[declaration.symbol] = Fork(data.flow.assignedLater, assignedInside)
            }

            /**
             * 访问 match 表达式并合并所有分支的 mini flow。
             */
            override fun visitMatchExpression(matchExpression: CfirMatchExpression, data: MiniCfgData) {
                matchExpression.subject?.accept(sourceVisitor, data)
                val flow = data.flow
                data.flow = matchExpression.branches.mapTo(mutableSetOf(flow)) { branch ->
                    data.flow = flow
                    branch.accept(sourceVisitor, data)
                    data.flow
                }.join()
            }

            /** 独立 If 的条件只执行一次，真/假分支分别从条件后的 flow 出发再合流。 */
            override fun visitIfExpression(ifExpression: CfirIfExpression, data: MiniCfgData) {
                data.variableDeclarations.addLast(mutableMapOf())
                ifExpression.condition.accept(sourceVisitor, data)
                val entry = data.flow
                ifExpression.thenBranch.accept(sourceVisitor, data)
                val thenFlow = data.flow
                data.variableDeclarations.removeLast()
                data.flow = entry
                ifExpression.elseBranch?.accept(sourceVisitor, data)
                data.flow = setOf(thenFlow, data.flow).join()
            }

            /** 右操作数的赋值只属于执行它的短路分支；PIPELINE/COMPOSITION 保持实际解糖顺序。 */
            override fun visitBinaryOp(binaryOp: CfirBinaryOp, data: MiniCfgData) {
                binaryOp.left.accept(sourceVisitor, data)
                val entry = data.flow
                binaryOp.right.accept(sourceVisitor, data)
                if (binaryOp.kind == CfirBinaryOpKind.AND || binaryOp.kind == CfirBinaryOpKind.OR ||
                    binaryOp.kind == CfirBinaryOpKind.COALESCING
                ) data.flow = setOf(entry, data.flow).join()
            }

            /**
             * 访问 try/catch/handler/finally，并合并异常分支 flow。
             */
            override fun visitTryExpression(tryExpression: CfirTryExpression, data: MiniCfgData) {
                tryExpression.resources.forEach { it.accept(sourceVisitor, data) }
                tryExpression.tryBlock.accept(sourceVisitor, data)
                val flow = data.flow
                data.flow = buildSet {
                    add(flow)
                    tryExpression.catches.forEach { catch ->
                        data.flow = flow
                        catch.accept(sourceVisitor, data)
                        add(data.flow)
                    }
                    tryExpression.handlers.forEach { handler ->
                        data.flow = flow
                        handler.accept(sourceVisitor, data)
                        add(data.flow)
                    }
                }.join()
                tryExpression.finallyBlock?.accept(sourceVisitor, data)
            }

            /**
             * 合并多个 mini flow。
             */
            private fun Set<MiniFlow>.join(): MiniFlow = singleOrNull() ?: MiniFlow(this)

            /**
             * 访问循环表达式并记录循环体内部赋值。
             */
            override fun visitLoopExpression(loopExpression: CfirLoopExpression, data: MiniCfgData) {
                val entry = data.flow
                val assignedInside = visitElementWithLexicalScope(loopExpression, data)
                entry.recordAssignments(assignedInside)
                data.flow.recordAssignments(assignedInside)
                data.flow = entry.fork()
                data.forks[loopExpression] = Fork(data.flow.assignedLater, assignedInside)
            }

            /** 序列只访问一次；pattern/guard/body 才进入每轮重复的赋值域。 */
            override fun visitForInExpression(forInExpression: CfirForInExpression, data: MiniCfgData) {
                val execution = CfirForInLoopExecution.read(forInExpression)
                val freeVariables = data.variableDeclarations.flatMapTo(mutableSetOf()) { it.values }
                var entry: MiniFlow? = null
                var repeated: MiniFlow? = null
                fun enterRepeated() {
                    if (repeated == null) {
                        entry = data.flow
                        repeated = MiniFlow.start().also { data.flow = it }
                    }
                }
                execution.accept(object : CfirForInLoopExecution.Visitor {
                    override fun expression(expression: CfirExpression) { expression.accept(sourceVisitor, data) }
                    override fun initialize(variable: CfirVariable) { variable.accept(sourceVisitor, data) }
                    override fun enterLoop(execution: CfirForInLoopExecution) { data.variableDeclarations.addLast(mutableMapOf()) }
                    override fun initialCheck(execution: CfirForInLoopExecution) {
                        if (execution is CfirForInLoopExecution.Range) expression(execution.comparison)
                    }
                    override fun enterCondition(execution: CfirForInLoopExecution) = enterRepeated()
                    override fun enterIteration(execution: CfirForInLoopExecution) = enterRepeated()
                    override fun check(execution: CfirForInLoopExecution) {
                        enterRepeated()
                    }
                    override fun bindAndBody(execution: CfirForInLoopExecution) {
                        data.variableDeclarations.addLast(mutableMapOf())
                        execution.pattern.accept(sourceVisitor, data)
                        forInExpression.patternGuard?.let(::expression)
                        expression(forInExpression.body)
                        data.variableDeclarations.removeLast()
                    }
                    override fun exitBody(execution: CfirForInLoopExecution) = Unit
                    override fun updateString(execution: CfirForInLoopExecution.StringIteration) {
                        val index = if (projectionMapper == null) execution.index else projectionMapper.mapElement(execution.index)
                        data.recordAssignment(index, AssignmentOccurrence(forInExpression, emptyList()), operatorAssignment = true)
                    }
                    override fun exitLoop(execution: CfirForInLoopExecution) {
                        val assignedInside = checkNotNull(repeated).assignedLater.apply { retain(freeVariables) }
                        val initialization = checkNotNull(entry)
                        data.variableDeclarations.removeLast()
                        initialization.recordAssignments(assignedInside)
                        data.flow.recordAssignments(assignedInside)
                        data.flow = initialization.fork()
                        data.forks[forInExpression] = Fork(data.flow.assignedLater, assignedInside)
                    }
                })
            }

            /**
             * 按调用求值顺序访问函数调用，并将 lambda 参数作为 postponed 参数放在普通参数之后。
             */
            override fun visitFunctionCall(functionCall: CfirFunctionCall, data: MiniCfgData) {
                val explicitReceiver = functionCall.explicitReceiver
                explicitReceiver?.accept(sourceVisitor, data)

                // resolved call 会让 explicit/dispatch receiver 共享同一个 CFIR DAG 节点；
                // receiver 在语言求值顺序中只执行一次，mini CFG 也必须按对象身份去重。
                val dispatchReceiver = functionCall.dispatchReceiver
                if (dispatchReceiver !== explicitReceiver) {
                    dispatchReceiver?.accept(sourceVisitor, data)
                }

                val (postponedLambdas, normalArguments) =
                    functionCall.argumentList.arguments.partition { it.unwrapAnonymousFunctionExpression() != null }
                normalArguments.forEach { it.accept(sourceVisitor, data) }
                postponedLambdas.forEach { it.accept(sourceVisitor, data) }

                functionCall.calleeReference.accept(sourceVisitor, data)
            }

            /**
             * 进入 block 词法作用域并记录其中声明的局部变量。
             */
            override fun visitBlock(block: CfirBlock, data: MiniCfgData) {
                data.variableDeclarations.addLast(mutableMapOf())
                visitElement(block, data)
                data.variableDeclarations.removeLast()
            }

            /**
             * 访问属性声明并把局部属性注册到当前词法变量表。
             */
            override fun visitProperty(property: CfirProperty, data: MiniCfgData) {
                visitElement(property, data)
                if (property.isLocal) {
                    data.variableDeclarations.last()[property.symbol.name] = property
                }
            }

            /**
             * 访问变量声明并把局部变量注册到当前词法变量表。
             */
            override fun visitVariable(variable: CfirVariable, data: MiniCfgData) {
                visitElement(variable, data)
                if (variable.isLocal) {
                    data.variableDeclarations.last()[variable.symbol.name] = variable
                }
            }

            /** 独立绑定声明的 generated 入口不委托 visitVariable，必须按同一词法协议登记。 */
            override fun visitPatternBindingVariable(variable: CfirPatternBindingVariable, data: MiniCfgData) {
                visitVariable(variable, data)
            }

            override fun visitFieldVariable(variable: CfirFieldVariable, data: MiniCfgData) {
                visitVariable(variable, data)
            }

            override fun visitValueParameter(variable: CfirValueParameter, data: MiniCfgData) {
                visitVariable(variable, data)
            }

            /** 模式容器与简单 binding 共用的 initializer 只访问一次，安装实际可见声明。 */
            override fun visitPatternVariable(variable: CfirPatternVariable, data: MiniCfgData) {
                variable.initializer?.accept(sourceVisitor, data)
                variable.pattern.visibleBindingVariables().forEach { binding ->
                    val projected = if (projectionMapper == null) binding else projectionMapper.mapElement(binding)
                    if (projected.isLocal) data.variableDeclarations.last()[projected.name] = projected
                }
            }

            /**
             * 访问赋值表达式并记录对局部变量的赋值。
             */
            override fun visitAssignment(assignment: CfirAssignment, data: MiniCfgData) {
                visitElement(assignment, data)
                CfirAssignmentExecution.of(assignment).targets.forEach { target ->
                    val lValue = target.expression as? CfirQualifiedAccessExpression ?: return@forEach
                    if (lValue.explicitReceiver != null || lValue.dispatchReceiver != null) return@forEach
                    val reference = lValue.calleeReference as? CfirNamedReference ?: return@forEach
                    data.recordAssignment(reference, AssignmentOccurrence(assignment, target.path),
                        operatorAssignment = assignment.isCompound || assignment.augmentedOperation != null)
                }
            }

            /** RAW 与已解析 compound/increment 的写入共享同一赋值记录协议。 */
            override fun visitAugmentedAssignment(assignment: CfirAugmentedAssignment, data: MiniCfgData) {
                visitElement(assignment, data)
                val left = assignment.leftArgument as? CfirQualifiedAccessExpression ?: return
                if (left.explicitReceiver != null || left.dispatchReceiver != null) return
                (left.calleeReference as? CfirNamedReference)?.let {
                    data.recordAssignment(it, AssignmentOccurrence(assignment, emptyList()), operatorAssignment = true)
                }
            }

            override fun visitIncrementDecrementExpression(expression: CfirIncrementDecrementExpression, data: MiniCfgData) {
                visitElement(expression, data)
                val left = expression.expression as? CfirQualifiedAccessExpression ?: return
                if (left.explicitReceiver != null || left.dispatchReceiver != null) return
                (left.calleeReference as? CfirNamedReference)?.let {
                    data.recordAssignment(it, AssignmentOccurrence(expression, emptyList()), operatorAssignment = true)
                }
            }

            /**
             * 将一次赋值写入 mini CFG 数据和当前 flow。
             */
            private fun MiniCfgData.recordAssignment(reference: CfirNamedReference, occurrence: AssignmentOccurrence, operatorAssignment: Boolean) {
                val declaration = resolveLocalDeclaration(reference) ?: return
                recordAssignment(declaration, occurrence, operatorAssignment)
            }

            /** STRING latch 没有赋值 AST，按真实 index 声明登记隐式写入。 */
            private fun MiniCfgData.recordAssignment(declaration: CfirCallableDeclaration, occurrence: AssignmentOccurrence, operatorAssignment: Boolean) {
                val assignment = Assignment(occurrence, operatorAssignment)
                assignments.getOrPut(declaration) { mutableListOf() }.add(assignment)
                flow.recordAssignment(declaration, assignment)
            }

            /**
             * 从引用解析结果或当前词法变量表中定位局部声明。
             */
            private fun MiniCfgData.resolveLocalDeclaration(reference: CfirNamedReference): CfirCallableDeclaration? {
                if (reference is CfirResolvedNamedReference) {
                    val symbol = if (projectionMapper == null) reference.resolvedSymbol else projectionMapper.mapSymbol(reference.resolvedSymbol)
                    return (symbol.cfir as? CfirCallableDeclaration)?.takeIf { it.isLocal }
                }

                val name = reference.name
                return variableDeclarations.lastOrNull { name in it }?.get(name)
            }

            /**
             * 记录当前 flow 上的单次赋值，并向父 flow 传播。
             */
            private fun MiniFlow.recordAssignment(declaration: CfirCallableDeclaration, assignment: Assignment) {
                if (!assignedLater.add(declaration, assignment)) return
                parents.forEach { it.recordAssignment(declaration, assignment) }
            }

            /**
             * 批量记录赋值集合，并向父 flow 传播。
             */
            private fun MiniFlow.recordAssignments(declarations: VariableAssignments) {
                if (!assignedLater.merge(declarations)) return
                parents.forEach { it.recordAssignments(declarations) }
            }

            /**
             * mini CFG 构造过程中共享的可变数据。
             */
            class MiniCfgData {
                /** 当前控制流位置。 */
                var flow: MiniFlow = MiniFlow.start()
                /** 词法作用域栈中的局部变量声明表。 */
                val variableDeclarations: ArrayDeque<MutableMap<Name, CfirCallableDeclaration>> =
                    ArrayDeque(listOf(mutableMapOf()))
                /** 每个局部声明的赋值记录列表。 */
                val assignments: MutableMap<CfirCallableDeclaration, MutableList<Assignment>> = mutableMapOf()
                /** 声明或循环到分叉信息的映射。 */
                val forks: MutableMap<Any /* CfirBasedSymbol<*> | CfirLoopExpression */, Fork> = mutableMapOf()
            }
        }
    }
}
