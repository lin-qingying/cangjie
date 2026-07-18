package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirInoutArgumentExpression
import org.cangnova.cangjie.cfir.expressions.CfirLoopExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 在已解析函数体上执行 enum constructor tag 的前向数据流分析。
 *
 * 官方 CHIR 常量分析只为“声明中存在 payload constructor”的 enum 保留 tuple/tag 表示；
 * 本分析复现同一抽象域，并在分支汇合时只保留所有路径一致的 tag。普通调用不会改写
 * caller 的局部存储；只有显式赋值、inout 和已知局部 callable 的外层写效应会使 fact 失效。
 */
class CfirEnumTagFlowAnalyzer(
    /** 当前分析使用的 session。 */
    private val session: CfirSession,
    /** 复用局部变量赋值分析器提供的 callable 直接外层写集合。 */
    private val localVariableAssignmentAnalyzer: CfirLocalVariableAssignmentAnalyzer,
) {
    /** 已知局部 callable 的传递写效应缓存。 */
    private val callableWriteEffects = IdentityHashMap<CfirFunction, Set<CfirCallableDeclaration>>()

    /** 分析函数并返回每个 match 程序点的唯一 constructor tag。 */
    fun analyze(function: CfirFunction): Map<CfirMatchExpression, CfirEnumConstructorSymbol> {
        val body = function.body ?: return emptyMap()
        val result = IdentityHashMap<CfirMatchExpression, CfirEnumConstructorSymbol>()
        body.accept(Visitor(result), FlowState())
        return result
    }

    /** 当前控制流点上局部声明的已知 enum tag。 */
    private class FlowState(
        val enumTags: IdentityHashMap<CfirCallableDeclaration, CfirEnumConstructorSymbol> = IdentityHashMap(),
    ) {
        fun copy(): FlowState = FlowState(IdentityHashMap(enumTags))

        fun replaceWith(other: FlowState) {
            enumTags.clear()
            enumTags.putAll(other.enumTags)
        }

        fun invalidateTags(declarations: Collection<CfirCallableDeclaration>) {
            declarations.forEach(enumTags::remove)
        }

        companion object {
            fun join(states: Collection<FlowState>): FlowState {
                val first = states.firstOrNull()?.copy() ?: return FlowState()
                first.enumTags.entries.removeIf { (declaration, tag) ->
                    states.any { state -> state.enumTags[declaration] !== tag }
                }
                return first
            }
        }
    }

    /** 按 CFIR 求值顺序传播 tag，并对控制流分叉执行交汇。 */
    private inner class Visitor(
        private val result: IdentityHashMap<CfirMatchExpression, CfirEnumConstructorSymbol>,
    ) : CfirVisitor<Unit, FlowState>() {
        override fun visitElement(element: CfirElement, data: FlowState) {
            element.acceptChildren(this, data)
        }

        override fun visitBlock(block: CfirBlock, data: FlowState) {
            block.statements.forEach { statement -> statement.accept(this, data) }
        }

        override fun visitVariable(variable: CfirVariable, data: FlowState) {
            variable.initializer?.accept(this, data)
            data.updateTag(variable, variable.initializer)
        }

        override fun visitFieldVariable(fieldVariable: CfirFieldVariable, data: FlowState) {
            visitVariable(fieldVariable, data)
        }

        override fun visitPatternVariable(patternVariable: CfirPatternVariable, data: FlowState) {
            val initializer = patternVariable.initializer
            initializer?.accept(this, data)

            // 简单 `let/var x = value` 的可引用声明是 pattern 内层 binding variable。
            // resolve 已把 whole initializer 同步到该 binding；析构 pattern 的分量值域
            // 需要独立投影分析，不能把整个 enum tag 直接复制给所有绑定。
            patternVariable.pattern.bindingVariables()
                .filter { binding -> binding.initializer === initializer }
                .forEach { binding -> data.updateTag(binding, binding.initializer) }
        }

        override fun visitPatternBindingVariable(patternBindingVariable: CfirPatternBindingVariable, data: FlowState) {
            // binding initializer 由外层 CfirPatternVariable 统一求值，避免共享表达式被重复执行。
        }

        override fun visitAssignment(assignment: CfirAssignment, data: FlowState) {
            assignment.rValue.accept(this, data)
            val declaration = assignment.lValue.resolvedCallableDeclarationOrNull() ?: return
            data.updateTag(declaration, assignment.rValue)
        }

        override fun visitInoutArgumentExpression(inoutArgumentExpression: CfirInoutArgumentExpression, data: FlowState) {
            inoutArgumentExpression.expression.accept(this, data)
            inoutArgumentExpression.expression.resolvedCallableDeclarationOrNull()?.let { declaration ->
                data.invalidateTags(listOf(declaration))
            }
        }

        override fun visitIfExpression(ifExpression: CfirIfExpression, data: FlowState) {
            ifExpression.condition.accept(this, data)
            val incoming = data.copy()
            val thenState = incoming.copy().also { state -> ifExpression.thenBranch.accept(this, state) }
            val elseState = incoming.copy().also { state -> ifExpression.elseBranch?.accept(this, state) }
            data.replaceWith(FlowState.join(listOf(thenState, elseState)))
        }

        override fun visitMatchExpression(matchExpression: CfirMatchExpression, data: FlowState) {
            matchExpression.subject?.accept(this, data)
            matchExpression.subject?.knownEnumTag(data)?.let { tag -> result[matchExpression] = tag }

            val incoming = data.copy()
            val branchStates = matchExpression.branches.map { branch ->
                incoming.copy().also { state ->
                    branch.guard?.accept(this, state)
                    branch.body.accept(this, state)
                }
            }
            data.replaceWith(FlowState.join(branchStates + incoming))
        }

        override fun visitLoopExpression(loopExpression: CfirLoopExpression, data: FlowState) {
            val incoming = data.copy()
            val iterated = incoming.copy()
            if (loopExpression.isDoWhile) {
                loopExpression.body.accept(this, iterated)
                loopExpression.condition.accept(this, iterated)
            } else {
                loopExpression.condition.accept(this, iterated)
                loopExpression.body.accept(this, iterated)
            }
            data.replaceWith(FlowState.join(listOf(incoming, iterated)))
        }

        override fun visitForInExpression(forInExpression: CfirForInExpression, data: FlowState) {
            forInExpression.iterable.accept(this, data)
            val incoming = data.copy()
            val iterated = incoming.copy()
            forInExpression.variable.accept(this, iterated)
            forInExpression.body.accept(this, iterated)
            data.replaceWith(FlowState.join(listOf(incoming, iterated)))
        }

        override fun visitTryExpression(tryExpression: CfirTryExpression, data: FlowState) {
            tryExpression.resources.forEach { resource -> resource.accept(this, data) }
            val incoming = data.copy()
            val exits = buildList {
                add(incoming.copy().also { state -> tryExpression.tryBlock.accept(this@Visitor, state) })
                tryExpression.catches.forEach { catch ->
                    add(incoming.copy().also { state -> catch.body.accept(this@Visitor, state) })
                }
                tryExpression.handlers.forEach { handler ->
                    add(incoming.copy().also { state -> handler.accept(this@Visitor, state) })
                }
            }
            val joined = FlowState.join(exits + incoming)
            tryExpression.finallyBlock?.accept(this, joined)
            data.replaceWith(joined)
        }

        override fun visitFunctionCall(functionCall: CfirFunctionCall, data: FlowState) {
            functionCall.acceptChildren(this, data)
            val effects = linkedSetOf<CfirCallableDeclaration>()
            functionCall.callableEffectTargets().forEach { target ->
                effects += transitiveWriteEffects(target)
            }
            data.invalidateTags(effects)
        }

        override fun visitFunction(function: CfirFunction, data: FlowState) {
            // 嵌套函数体不在声明点执行；真正调用时按 callable write effect 精确失效。
        }

        override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression, data: FlowState) {
            // Lambda body 不在表达式创建点执行。
        }

        override fun visitClass(klass: CfirClass, data: FlowState) {
            // 局部 class body 不在声明点执行。
        }

        private fun FlowState.updateTag(
            declaration: CfirCallableDeclaration,
            expression: CfirExpression?,
        ) {
            val tag = expression?.knownEnumTag(this)
            if (tag == null) enumTags.remove(declaration) else enumTags[declaration] = tag
        }

        private fun CfirExpression.knownEnumTag(state: FlowState): CfirEnumConstructorSymbol? {
            directEnumConstructorSymbolOrNull()?.let { return it }
            val declaration = resolvedCallableDeclarationOrNull() ?: return null
            return state.enumTags[declaration]
        }
    }

    /** 计算局部 callable 及其已知局部调用图的传递外层写效应。 */
    private fun transitiveWriteEffects(function: CfirFunction): Set<CfirCallableDeclaration> =
        callableWriteEffects.getOrPut(function) {
            val effects = linkedSetOf<CfirCallableDeclaration>()
            val visited = Collections.newSetFromMap(IdentityHashMap<CfirFunction, Boolean>())

            fun collect(current: CfirFunction) {
                if (!visited.add(current)) return
                effects += localVariableAssignmentAnalyzer.assignedOuterLocals(current)
                val targets = mutableSetOf<CfirFunction>()
                current.body?.accept(CallableEffectTargetCollector(), targets)
                targets.forEach(::collect)
            }

            collect(function)
            effects
        }

    /** 收集函数体中实际调用或可能由高阶调用执行的局部 callable。 */
    private inner class CallableEffectTargetCollector : CfirVisitor<Unit, MutableSet<CfirFunction>>() {
        override fun visitElement(element: CfirElement, data: MutableSet<CfirFunction>) {
            element.acceptChildren(this, data)
        }

        override fun visitFunction(function: CfirFunction, data: MutableSet<CfirFunction>) {
            // 嵌套 callable 的声明点不执行其 body。
        }

        override fun visitAnonymousFunctionExpression(
            anonymousFunctionExpression: CfirAnonymousFunctionExpression,
            data: MutableSet<CfirFunction>,
        ) {
            // Lambda 创建不执行 body；作为 receiver/argument 时由外层 call 显式加入。
        }

        override fun visitFunctionCall(functionCall: CfirFunctionCall, data: MutableSet<CfirFunction>) {
            data += functionCall.callableEffectTargets()
            functionCall.acceptChildren(this, data)
        }
    }

    /**
     * 返回一次调用可能执行的局部 callable。
     *
     * 直接具名局部函数、立即调用 lambda 以及作为高阶实参传入的局部 callable 都纳入；
     * 非局部函数无法捕获 caller 局部声明，不产生 caller-local write effect。
     */
    private fun CfirFunctionCall.callableEffectTargets(): Set<CfirFunction> = buildSet {
        resolvedFunctionOrNull()?.takeIf(CfirFunction::isLocal)?.let(::add)
        explicitReceiver.callableFunctionOrNull()?.takeIf(CfirFunction::isLocal)?.let(::add)
        dispatchReceiver.callableFunctionOrNull()?.takeIf(CfirFunction::isLocal)?.let(::add)
        argumentList.arguments.forEach { argument ->
            argument.callableFunctionOrNull()?.takeIf(CfirFunction::isLocal)?.let(::add)
        }
    }

    /** 从调用或函数值表达式中恢复其局部 callable 声明。 */
    private fun CfirExpression?.callableFunctionOrNull(): CfirFunction? = when (this) {
        is CfirAnonymousFunctionExpression -> anonymousFunction
        is CfirWrappedExpression -> expression.callableFunctionOrNull()
        is CfirQualifiedAccessExpression -> resolvedSymbolOrNull()?.takeIf { it.isBound }?.cfir as? CfirFunction
        else -> null
    }

    /** 从 function-call callee 恢复直接调用的函数。 */
    private fun CfirFunctionCall.resolvedFunctionOrNull(): CfirFunction? =
        resolvedSymbolOrNull()?.takeIf { it.isBound }?.cfir as? CfirFunction

    /** 提取表达式直接引用的、采用 payload-tag 表示的 enum constructor。 */
    private fun CfirExpression.directEnumConstructorSymbolOrNull(): CfirEnumConstructorSymbol? {
        val symbol = resolvedSymbolOrNull() as? CfirEnumConstructorSymbol ?: return null
        val owner = session.cfirProvider.getContainingClass(symbol)?.cfir as? CfirEnum ?: return null
        val hasPayloadConstructor = owner.declarations
            .filterIsInstance<CfirEnumConstructor>()
            .any { constructor -> constructor.valueParameters.isNotEmpty() }
        return symbol.takeIf { hasPayloadConstructor }
    }

    /** 从表达式引用中提取局部 callable 声明。 */
    private fun CfirExpression.resolvedCallableDeclarationOrNull(): CfirCallableDeclaration? =
        resolvedSymbolOrNull()?.takeIf { symbol -> symbol.isBound }?.cfir as? CfirCallableDeclaration

    /** 从已完成或候选引用中提取符号。 */
    private fun CfirExpression.resolvedSymbolOrNull(): CfirBasedSymbol<*>? {
        val access = this as? CfirQualifiedAccessExpression ?: return null
        return when (val reference = access.calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
            else -> null
        }
    }
}
