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
    private var assignedLocalVariablesByDeclaration: Map<Any /* CfirBasedSymbol<*> | CfirLoopExpression */, Fork>?,
    private var variableAssignments: Map<CfirCallableDeclaration, List<Assignment>>?,
    private val scopes: Stack<Pair<Fork?, VariableAssignments>>,
    // 与 CFG builder 的 postponed lambda 拓扑保持同构。
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

    private fun CfirCallableDeclaration.isMutableLocalVariable(): Boolean {
        return when (this) {
            is CfirProperty -> status.isMut
            is CfirVariable -> isVar
            else -> false
        }
    }

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

    private fun getInfoForDeclaration(symbol: Any): Fork? {
        val root = rootSymbol ?: return null
        if (root == symbol) return null
        val cachedMap = buildInfoForRoot(root)
        return cachedMap[symbol]
    }

    private fun buildInfoForRoot(root: CfirBasedSymbol<*>): Map<Any, Fork> {
        assignedLocalVariablesByDeclaration?.let { return it }

        val data = MiniCfgBuilder.MiniCfgData()
        MiniCfgBuilder().visitElement(root.cfir, data)

        assignedLocalVariablesByDeclaration = data.forks
        variableAssignments = data.assignments
        return data.forks
    }

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

    fun exitFunction() {
        scopes.pop()
        if (scopes.isEmpty) {
            rootSymbol = null
            assignedLocalVariablesByDeclaration = null
            variableAssignments = null
        }
    }

    fun enterCodeFragment(codeFragment: CfirCodeFragment) {
        enterNewTopLevelScopeIfNeeded(codeFragment)
    }

    fun exitCodeFragment(codeFragment: CfirCodeFragment) {
        exitNewTopLevelScopeIfNeeded(codeFragment)
    }

    private fun enterNewTopLevelScopeIfNeeded(declaration: CfirDeclaration) {
        if (rootSymbol != null) return
        rootSymbol = declaration.symbol
        scopes.push(null to VariableAssignments())
    }

    private fun exitNewTopLevelScopeIfNeeded(declaration: CfirDeclaration) {
        if (rootSymbol == declaration.symbol) {
            rootSymbol = null
            scopes.pop()
            assignedLocalVariablesByDeclaration = null
            variableAssignments = null
        }
    }

    fun enterClass(klass: CfirClass) {
        if (rootSymbol == null) return
        enterScope(klass.symbol, evaluatedInPlace = false)
    }

    fun exitClass() {
        if (rootSymbol == null) return
        scopes.pop()
    }

    fun enterFunctionCall(lambdaArgs: Collection<CfirAnonymousFunction>) {
        if (rootSymbol == null) return
        postponedLambdas.push(lambdaArgs.mapNotNull { getInfoForDeclaration(it.symbol) }.associateWithTo(mutableMapOf()) { false })
    }

    fun exitFunctionCall(callCompleted: Boolean) {
        if (rootSymbol == null) return
        val lambdasInCall = postponedLambdas.pop()
        if (!callCompleted) {
            lambdasInCall.keys.associateWithTo(postponedLambdas.topOrNull() ?: return) { true }
        }
    }

    fun enterLoop(loop: CfirLoopExpression): Set<CfirBasedSymbol<*>> {
        if (rootSymbol == null) return emptySet()
        val (info, _) = enterScope(loop, evaluatedInPlace = true)
        return info?.assignedInside?.getAssignedSymbols().orEmpty()
    }

    fun exitLoop(): Set<CfirBasedSymbol<*>> {
        if (rootSymbol == null) return emptySet()
        val (info, _) = scopes.pop()
        return info?.assignedInside?.getAssignedSymbols().orEmpty()
    }

    fun visitAssignment(declaration: CfirCallableDeclaration, type: ConeCangJieType) {
        buildInfoForRoot(rootSymbol ?: return)
        val assignments = variableAssignments?.get(declaration) ?: return
        val assignment = assignments.firstOrNull { it.type == null } ?: return
        assignment.type = type
    }

    companion object {
        private class Fork(
            val assignedLater: VariableAssignments,
            val assignedInside: VariableAssignments,
        ) {
            @CfgInternals
            fun createSnapshot(firMapper: SnapshotCfirMapper): Fork {
                return Fork(assignedLater.createSnapshot(firMapper), assignedInside.createSnapshot(firMapper))
            }
        }

        private class Assignment(
            val operatorAssignment: Boolean,
            var type: ConeCangJieType? = null,
        ) {
            fun createSnapshot(): Assignment = Assignment(operatorAssignment, type)
        }

        private class VariableAssignments {
            private val assignments: MutableMap<CfirCallableDeclaration, MutableSet<Assignment>> = mutableMapOf()

            operator fun get(declaration: CfirCallableDeclaration): Set<Assignment>? = assignments[declaration]

            operator fun contains(declaration: CfirCallableDeclaration): Boolean = declaration in assignments

            fun add(declaration: CfirCallableDeclaration, assignment: Assignment): Boolean {
                return assignments.getOrPut(declaration) { mutableSetOf() }.add(assignment)
            }

            @CfgInternals
            fun createSnapshot(firMapper: SnapshotCfirMapper): VariableAssignments {
                val copy = VariableAssignments()
                for ((key, value) in assignments) {
                    copy.assignments[firMapper.mapElement(key)] = value.mapTo(mutableSetOf()) { it.createSnapshot() }
                }
                return copy
            }

            fun copy(): VariableAssignments {
                val copy = VariableAssignments()
                copy.assignments += assignments
                return copy
            }

            fun merge(other: VariableAssignments?): Boolean {
                if (other == null || other.assignments.isEmpty()) return false
                var modified = false
                for ((declaration, values) in other.assignments) {
                    modified = modified or assignments.getOrPut(declaration) { mutableSetOf() }.addAll(values)
                }
                return modified
            }

            fun retain(declarations: Set<CfirCallableDeclaration>) {
                assignments.keys.retainAll(declarations)
            }

            fun getAssignedSymbols(): Set<CfirBasedSymbol<*>> {
                return assignments.entries
                    .filter { (_, values) -> values.any { !it.operatorAssignment } }
                    .mapTo(mutableSetOf()) { (declaration, _) -> declaration.symbol }
            }
        }

        private class MiniFlow(val parents: Set<MiniFlow>) {
            val assignedLater = VariableAssignments()

            fun fork(): MiniFlow = MiniFlow(setOf(this))

            companion object {
                fun start() = MiniFlow(emptySet())
            }
        }

        /**
         * 语法级 mini CFG 构造器：
         * - 仅抽取“局部声明 + 赋值 + 分支/循环/异常/调用参数顺序”这些对稳定性判定必要的信息；
         * - 节点选择与遍历顺序对位 Kotlin FIR 的 `MiniCfgBuilder`，但仅使用仓颉真实节点。
         */
        private class MiniCfgBuilder : CfirVisitor<Unit, MiniCfgBuilder.MiniCfgData>() {
            override fun visitElement(element: CfirElement, data: MiniCfgData) {
                element.acceptChildren(this, data)
            }

            private fun visitElementWithLexicalScope(element: CfirElement, data: MiniCfgData): VariableAssignments {
                val flow = MiniFlow.start()
                val freeVariables = data.variableDeclarations.flatMapTo(mutableSetOf()) { it.values }
                data.flow = flow
                element.acceptChildren(this, data)
                return flow.assignedLater.apply { retain(freeVariables) }
            }

            override fun visitAnonymousFunction(anonymousFunction: CfirAnonymousFunction, data: MiniCfgData) =
                visitLocalDeclaration(anonymousFunction, data)

            override fun visitFunction(function: CfirFunction, data: MiniCfgData) =
                visitLocalDeclaration(function, data)

            override fun visitClass(klass: CfirClass, data: MiniCfgData) =
                visitLocalDeclaration(klass, data)

            override fun visitCodeFragment(codeFragment: CfirCodeFragment, data: MiniCfgData) =
                visitLocalDeclaration(codeFragment, data)

            private fun visitLocalDeclaration(declaration: CfirDeclaration, data: MiniCfgData) {
                val flow = data.flow
                val assignedInside = visitElementWithLexicalScope(declaration, data)
                flow.recordAssignments(assignedInside)
                data.flow = flow.fork()
                data.forks[declaration.symbol] = Fork(data.flow.assignedLater, assignedInside)
            }

            override fun visitMatchExpression(matchExpression: CfirMatchExpression, data: MiniCfgData) {
                matchExpression.subject?.accept(this, data)
                val flow = data.flow
                data.flow = matchExpression.branches.mapTo(mutableSetOf(flow)) { branch ->
                    data.flow = flow
                    branch.accept(this, data)
                    data.flow
                }.join()
            }

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

            private fun Set<MiniFlow>.join(): MiniFlow = singleOrNull() ?: MiniFlow(this)

            override fun visitLoopExpression(loopExpression: CfirLoopExpression, data: MiniCfgData) {
                val entry = data.flow
                val assignedInside = visitElementWithLexicalScope(loopExpression, data)
                entry.recordAssignments(assignedInside)
                data.flow.recordAssignments(assignedInside)
                data.flow = entry.fork()
                data.forks[loopExpression] = Fork(data.flow.assignedLater, assignedInside)
            }

            override fun visitFunctionCall(functionCall: CfirFunctionCall, data: MiniCfgData) {
                functionCall.explicitReceiver?.accept(this, data)
                functionCall.dispatchReceiver?.accept(this, data)

                val (postponedLambdas, normalArguments) =
                    functionCall.argumentList.arguments.partition { it is CfirAnonymousFunctionExpression }
                normalArguments.forEach { it.accept(this, data) }
                postponedLambdas.forEach { it.accept(this, data) }

                functionCall.calleeReference.accept(this, data)
            }

            override fun visitBlock(block: CfirBlock, data: MiniCfgData) {
                data.variableDeclarations.addLast(mutableMapOf())
                visitElement(block, data)
                data.variableDeclarations.removeLast()
            }

            override fun visitProperty(property: CfirProperty, data: MiniCfgData) {
                visitElement(property, data)
                if (property.isLocal) {
                    data.variableDeclarations.last()[property.symbol.name] = property
                }
            }

            override fun visitVariable(variable: CfirVariable, data: MiniCfgData) {
                visitElement(variable, data)
                if (variable.isLocal) {
                    data.variableDeclarations.last()[variable.symbol.name] = variable
                }
            }

            override fun visitAssignment(assignment: CfirAssignment, data: MiniCfgData) {
                visitElement(assignment, data)
                val lValue = assignment.lValue as? CfirQualifiedAccessExpression ?: return
                if (lValue.explicitReceiver != null || lValue.dispatchReceiver != null) return
                val calleeReference = lValue.calleeReference
                if (calleeReference !is CfirNamedReference) return

                // 目前 CFIR 没有稳定的 compound-assignment 承载点，保持 false，不做伪造推断。
                data.recordAssignment(calleeReference, operatorAssignment = false)
            }

            private fun MiniCfgData.recordAssignment(reference: CfirNamedReference, operatorAssignment: Boolean) {
                val declaration = resolveLocalDeclaration(reference) ?: return
                val assignment = Assignment(operatorAssignment)
                assignments.getOrPut(declaration) { mutableListOf() }.add(assignment)
                flow.recordAssignment(declaration, assignment)
            }

            private fun MiniCfgData.resolveLocalDeclaration(reference: CfirNamedReference): CfirCallableDeclaration? {
                val resolved = (reference as? CfirResolvedNamedReference)?.resolvedSymbol?.cfir as? CfirCallableDeclaration
                if (resolved != null && resolved.isLocal) {
                    return resolved
                }

                val name = reference.name
                return variableDeclarations.lastOrNull { name in it }?.get(name)
            }

            private fun MiniFlow.recordAssignment(declaration: CfirCallableDeclaration, assignment: Assignment) {
                if (!assignedLater.add(declaration, assignment)) return
                parents.forEach { it.recordAssignment(declaration, assignment) }
            }

            private fun MiniFlow.recordAssignments(declarations: VariableAssignments) {
                if (!assignedLater.merge(declarations)) return
                parents.forEach { it.recordAssignments(declarations) }
            }

            class MiniCfgData {
                var flow: MiniFlow = MiniFlow.start()
                val variableDeclarations: ArrayDeque<MutableMap<Name, CfirCallableDeclaration>> =
                    ArrayDeque(listOf(mutableMapOf()))
                val assignments: MutableMap<CfirCallableDeclaration, MutableList<Assignment>> = mutableMapOf()
                val forks: MutableMap<Any /* CfirBasedSymbol<*> | CfirLoopExpression */, Fork> = mutableMapOf()
            }
        }
    }
}
