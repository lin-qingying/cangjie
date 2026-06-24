package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
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
) {
    constructor() : this(
        rootSymbol = null,
        assignedLocalVariablesByDeclaration = null,
        variableAssignments = null,
        scopes = stackOf(),
        postponedLambdas = stackOf(),
    )

    /**
     * 深拷贝当前分析器状态，并对快照中涉及到的 symbol/element 做 mapper 映射。
     */
    @CfgInternals
    internal fun createSnapshot(firMapper: SnapshotCfirMapper): CfirLocalVariableAssignmentAnalyzer {
        /**
         * 深拷贝状态对象，并把其中的 CFIR symbol/element 映射到快照树。
         */
        fun <T> clone(value: T): T {
            @Suppress("UNCHECKED_CAST")
            return when (value) {
                is CfirBasedSymbol<*> -> firMapper.mapSymbol(value) as T
                is CfirElement -> firMapper.mapElement(value) as T
                is Fork -> value.createSnapshot(firMapper) as T
                is VariableAssignments -> value.createSnapshot(firMapper) as T
                is Pair<*, *> -> Pair(clone(value.first), clone(value.second)) as T
                is List<*> -> value.map { clone(it) } as T
                is Map<*, *> -> buildMap {
                    value.forEach { (k, v) -> put(clone(k), clone(v)) }
                } as T
                is Stack<*> -> value.createSnapshot { clone(it) } as T
                is Assignment -> value.createSnapshot() as T
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
        val callable = declaration as? CfirCallableDeclaration ?: return false
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
        if (root == symbol) return null
        val cachedMap = buildInfoForRoot(root)
        return cachedMap[symbol]
    }

    /**
     * 为根声明构造并缓存 mini CFG 赋值信息。
     */
    private fun buildInfoForRoot(root: CfirBasedSymbol<*>): Map<Any, Fork> {
        assignedLocalVariablesByDeclaration?.let { return it }

        val data = MiniCfgBuilder.MiniCfgData()
        MiniCfgBuilder().visitElement(root.cfir, data)

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
    fun enterFunction(function: CfirFunction): Set<CfirBasedSymbol<*>> {
        if (rootSymbol == null) {
            rootSymbol = function.symbol
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
        rootSymbol = declaration.symbol
        scopes.push(null to VariableAssignments())
    }

    /**
     * 当前根声明正是给定声明时，退出并清空顶层分析根。
     */
    private fun exitNewTopLevelScopeIfNeeded(declaration: CfirDeclaration) {
        if (rootSymbol == declaration.symbol) {
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
    fun enterLoop(loop: CfirLoopExpression): Set<CfirBasedSymbol<*>> {
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

    /**
     * 记录某个局部 callable 的一次真实赋值类型。
     */
    fun visitAssignment(declaration: CfirCallableDeclaration, type: ConeCangJieType) {
        buildInfoForRoot(rootSymbol ?: return)
        val assignments = variableAssignments?.get(declaration) ?: return
        val assignment = assignments.firstOrNull { it.type == null } ?: return
        assignment.type = type
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
            fun createSnapshot(firMapper: SnapshotCfirMapper): Fork {
                return Fork(assignedLater.createSnapshot(firMapper), assignedInside.createSnapshot(firMapper))
            }
        }

        /**
         * 单次赋值记录。
         */
        private class Assignment(
            /** 是否来自复合/操作符赋值。 */
            val operatorAssignment: Boolean,
            /** 赋值右侧推断出的类型；mini CFG 构造阶段可暂为空。 */
            var type: ConeCangJieType? = null,
        ) {
            /**
             * 创建赋值记录副本。
             */
            fun createSnapshot(): Assignment = Assignment(operatorAssignment, type)
        }

        /**
         * 局部 callable 到赋值集合的映射。
         */
        private class VariableAssignments {
            /** 按局部声明聚合的赋值记录。 */
            private val assignments: MutableMap<CfirCallableDeclaration, MutableSet<Assignment>> = mutableMapOf()

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
            fun createSnapshot(firMapper: SnapshotCfirMapper): VariableAssignments {
                val copy = VariableAssignments()
                for ((key, value) in assignments) {
                    copy.assignments[firMapper.mapElement(key)] = value.mapTo(mutableSetOf()) { it.createSnapshot() }
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
        private class MiniCfgBuilder : CfirVisitor<Unit, MiniCfgBuilder.MiniCfgData>() {
            /**
             * 默认递归访问子节点。
             */
            override fun visitElement(element: CfirElement, data: MiniCfgData) {
                element.acceptChildren(this, data)
            }

            /**
             * 在新的词法作用域中访问元素并返回该作用域内自由变量赋值集合。
             */
            private fun visitElementWithLexicalScope(element: CfirElement, data: MiniCfgData): VariableAssignments {
                val flow = MiniFlow.start()
                val freeVariables = data.variableDeclarations.flatMapTo(mutableSetOf()) { it.values }
                data.flow = flow
                element.acceptChildren(this, data)
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
                visitLocalDeclaration(function, data)

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
                matchExpression.subject?.accept(this, data)
                val flow = data.flow
                data.flow = matchExpression.branches.mapTo(mutableSetOf(flow)) { branch ->
                    data.flow = flow
                    branch.accept(this, data)
                    data.flow
                }.join()
            }

            /**
             * 访问 try/catch/handler/finally，并合并异常分支 flow。
             */
            override fun visitTryExpression(tryExpression: CfirTryExpression, data: MiniCfgData) {
                tryExpression.tryBlock.accept(this, data)
                val flow = data.flow
                data.flow = buildSet {
                    add(flow)
                    tryExpression.catches.forEach { catch ->
                        data.flow = flow
                        catch.accept(this@MiniCfgBuilder, data)
                        add(data.flow)
                    }
                    tryExpression.handlers.forEach { handler ->
                        data.flow = flow
                        handler.accept(this@MiniCfgBuilder, data)
                        add(data.flow)
                    }
                }.join()
                tryExpression.finallyBlock?.accept(this, data)
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

            /**
             * 按调用求值顺序访问函数调用，并将 lambda 参数作为 postponed 参数放在普通参数之后。
             */
            override fun visitFunctionCall(functionCall: CfirFunctionCall, data: MiniCfgData) {
                functionCall.explicitReceiver?.accept(this, data)
                functionCall.dispatchReceiver?.accept(this, data)

                val (postponedLambdas, normalArguments) =
                    functionCall.argumentList.arguments.partition { it is CfirAnonymousFunctionExpression }
                normalArguments.forEach { it.accept(this, data) }
                postponedLambdas.forEach { it.accept(this, data) }

                functionCall.calleeReference.accept(this, data)
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

            /**
             * 访问赋值表达式并记录对局部变量的赋值。
             */
            override fun visitAssignment(assignment: CfirAssignment, data: MiniCfgData) {
                visitElement(assignment, data)
                val lValue = assignment.lValue as? CfirQualifiedAccessExpression ?: return
                if (lValue.explicitReceiver != null || lValue.dispatchReceiver != null) return
                val calleeReference = lValue.calleeReference
                if (calleeReference !is CfirNamedReference) return

                // 目前 CFIR 没有稳定的 compound-assignment 承载点，保持 false，不做伪造推断。
                data.recordAssignment(calleeReference, operatorAssignment = false)
            }

            /**
             * 将一次赋值写入 mini CFG 数据和当前 flow。
             */
            private fun MiniCfgData.recordAssignment(reference: CfirNamedReference, operatorAssignment: Boolean) {
                val declaration = resolveLocalDeclaration(reference) ?: return
                val assignment = Assignment(operatorAssignment)
                assignments.getOrPut(declaration) { mutableListOf() }.add(assignment)
                flow.recordAssignment(declaration, assignment)
            }

            /**
             * 从引用解析结果或当前词法变量表中定位局部声明。
             */
            private fun MiniCfgData.resolveLocalDeclaration(reference: CfirNamedReference): CfirCallableDeclaration? {
                val resolved = (reference as? CfirResolvedNamedReference)?.resolvedSymbol?.cfir as? CfirCallableDeclaration
                if (resolved != null && resolved.isLocal) {
                    return resolved
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
