package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.declarations.enumPatternConstructorAccessOrNull
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CFGNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.EdgeLabel
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.FunctionCallExitNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.MatchBranchFailure
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.MatchBranchSuccess
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.MatchPatternDecisionNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.MatchExitNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.VariableAssignmentNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.VariableDeclarationExitNode
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import java.util.ArrayDeque
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 函数 CFG 上的保守常量分支分析。
 *
 * 对位官方 CHIR `ConstAnalysis` 的 terminator 效应：只有 CFG 中已有的 match 判定
 * 能被当前状态确定时，才传播到唯一的 success/failure 后继；未知条件必须同时保留全部
 * 后继。这里的常量值只存在于分析状态，不回写到 tree，也不会传给 Sema Maranget matrix。
 *
 * CHIR enum 抽象值保留 constructor tag；payload 只保留其中嵌套 enum 的 tag，不能把
 * 普通字面量 payload 当作完整编译期值。这一点保证本分析选择的是 lowering 已经建立的
 * 分支后继，而不是重新实现一套递归 pattern matcher。
 */
internal class CfirControlFlowConstAnalysis {
    /** 返回由 CFG 常量分支效应证明不可达的 pattern source。 */
    fun collectUnreachablePatterns(graph: ControlFlowGraph): Set<CfirPattern> {
        val unreachablePatterns = Collections.newSetFromMap(IdentityHashMap<CfirPattern, Boolean>())
        val incomingStates = IdentityHashMap<CFGNode<*>, ConstState>()
        val worklist = ArrayDeque<CFGNode<*>>()
        incomingStates[graph.enterNode] = ConstState.EMPTY
        worklist += graph.enterNode

        while (worklist.isNotEmpty()) {
            val node = worklist.removeFirst()
            val input = incomingStates[node] ?: continue
            val output = input.transfer(node)
            val target = node.constantTargetSuccessor(output)

            for (successor in node.followingNodes) {
                val edge = node.edgeTo(successor)
                if (!edge.kind.usedInCfa || edge.kind.isDead) continue

                if (target != null && successor !== target) {
                    (node as? MatchPatternDecisionNode)?.recordRejectedSuccessor(
                        edgeLabel = edge.label,
                        rejectedSuccessor = successor,
                        destination = unreachablePatterns,
                    )
                    continue
                }

                val merged = incomingStates[successor]?.join(output) ?: output
                if (incomingStates[successor] != merged) {
                    incomingStates[successor] = merged
                    worklist += successor
                }
            }
        }
        return unreachablePatterns
    }

    /**
     * 仅在 CFG 明确标记了原子模式判定的 success/failure 对时选择唯一后继。
     *
     * 这与官方 `PropagateTerminatorEffect` 一致：未知 domain 不得杀边，已知条件才把
     * state join 到唯一 target successor。
     */
    private fun CFGNode<*>.constantTargetSuccessor(
        state: ConstState,
    ): CFGNode<*>? {
        val decisionNode = this as? MatchPatternDecisionNode ?: return null
        val guard = decisionNode.guard
        val decision = if (guard != null) {
            guard.constantBoolean(state)
        } else {
            val pattern = decisionNode.pattern ?: return null
            val subject = decisionNode.matchExpression.subject ?: return null
            val subjectValue = subject.constantValue(state) ?: return null
            val selectedValue = subjectValue.payloadAt(decisionNode.subjectPath) ?: return null
            pattern.matchesAtomicConstant(selectedValue, state)
        } ?: return null
        val targetLabel = if (decision) MatchBranchSuccess else MatchBranchFailure
        return followingNodes.singleOrNull { successor -> edgeTo(successor).label == targetLabel }
    }

    /**
     * 把被拒绝 success successor 还原为其对应的 pattern source。
     *
     * 官方 CHIR 只从常量分析拒绝的 successor 开始沿 block 图报告 debug location。模式判定
     * 成功而拒绝 failure successor 时，当前 case 本身仍可执行，不能报告；只有失败而拒绝
     * success successor 时，才从该死路径收集模式 source。
     */
    private fun MatchPatternDecisionNode.recordRejectedSuccessor(
        edgeLabel: EdgeLabel,
        rejectedSuccessor: CFGNode<*>,
        destination: MutableSet<CfirPattern>,
    ) {
        if (edgeLabel != MatchBranchSuccess) return
        System.err.println(
            "CFIR_CFA_REJECT pattern=${pattern?.javaClass?.simpleName} " +
                "source=${reportSource.javaClass.simpleName}:${reportSource.source} " +
                "path=$subjectPath switch=${matchExpression.usesSwitchLowering()}",
        )
        if (pattern != null) {
            destination += reportSource
        }
        // 表驱动 lowering 的 tag/payload switch 后继彼此独立，不能把一个被拒绝表项当作
        // 顺序模式的残余 failure 链继续扫描。非 switch 模式则与 CHIR 的嵌套 Branch 图一致。
        if (!matchExpression.usesSwitchLowering()) {
            rejectedSuccessor.collectRejectedDecisionSources(matchExpression, destination)
        }
    }

    /**
     * 从被杀死的 successor 沿 CFG 收集同一个 match 的原子模式 source。
     *
     * 不能跨越该 match 的 exit：成功结果与 failure 链会在该点重新汇合，exit 之后的
     * 节点仍然可由唯一 target 到达，不能误报为不可达。
     */
    private fun CFGNode<*>.collectRejectedDecisionSources(
        owner: CfirMatchExpression,
        destination: MutableSet<CfirPattern>,
    ) {
        val visited = Collections.newSetFromMap(IdentityHashMap<CFGNode<*>, Boolean>())
        val worklist = ArrayDeque<CFGNode<*>>()
        worklist += this
        while (worklist.isNotEmpty()) {
            val node = worklist.removeFirst()
            if (!visited.add(node)) continue
            if (node is MatchExitNode && node.fir === owner) continue
            if (node is MatchPatternDecisionNode && node.matchExpression === owner) {
                destination += node.reportSource
            }
            node.followingNodes.forEach { successor ->
                val edge = node.edgeTo(successor)
                if (edge.kind.usedInCfa) {
                    worklist += successor
                }
            }
        }
    }

    /** 对 CFG 节点执行常量状态 transfer；只在节点的正规求值位置读写局部事实。 */
    private fun ConstState.transfer(node: CFGNode<*>): ConstState = when (node) {
        is VariableDeclarationExitNode -> assign(node.fir.symbol, node.fir.initializer)
        is VariableAssignmentNode -> assign(node.fir.assignedVariableSymbolOrNull(), node.fir.rValue)
        is FunctionCallExitNode -> if (node.fir.enumConstructorSymbolOrNull() == null) clear() else this
        else -> this
    }

    /** 把一个已知表达式值写入局部变量；未知值会清除旧事实。 */
    private fun ConstState.assign(variable: CfirVariableSymbol<*>?, expression: CfirExpression?): ConstState {
        if (variable == null) return this
        val value = expression?.constantValue(this) ?: ConstValue.Unknown
        return copy().also { state -> state.put(variable, value) }
    }

    /** 常量 bool 仅接受已知 Boolean 字面量。 */
    private fun CfirExpression.constantBoolean(state: ConstState): Boolean? =
        (constantValue(state) as? ConstValue.Literal)?.takeIf { it.kind == CfirLiteralKind.BOOLEAN }?.value as? Boolean

    /**
     * 比较 lowering 中可用的已知 abstract value 与已解析 pattern。
     *
     * enum 的普通 payload literal 在官方 ValueAnalysis 中不进入抽象域，所以它只能给出
     * unknown；嵌套 enum constructor tag 可以继续决定下一层 enum pattern。
     */
    private fun CfirPattern.matchesAtomicConstant(value: ConstValue, state: ConstState): Boolean? {
        return when (this) {
            is CfirEnumPattern -> {
                val enumValue = value as? ConstValue.Enum ?: return null
                val resolvedConstructor = constructorReference.resolvedEnumConstructorSymbolOrNull()
                if (resolvedConstructor != null && resolvedConstructor != enumValue.constructor) return false
                if (resolvedConstructor == null &&
                    constructorReference.enumPatternConstructorAccessOrNull()?.constructorName != enumValue.constructor.name
                ) return false
                if (resolvedConstructor == null && constructorReference.enumPatternConstructorAccessOrNull() == null) return null
                true
            }

            is CfirConstPattern -> expression.matchesConstantExpression(value, state)
            is CfirExpressionPattern -> expression.matchesConstantExpression(value, state)
            else -> null
        }
    }

    /** 常量 pattern 和 expression pattern 都按已知常量值精确比较。 */
    private fun CfirExpression.matchesConstantExpression(value: ConstValue, state: ConstState): Boolean? {
        val expected = constantValue(state) ?: return null
        return expected == value
    }

    /** 从 CFG 节点拥有的已解析表达式计算常量 domain。 */
    private fun CfirExpression.constantValue(state: ConstState): ConstValue? = when (this) {
        is CfirWrappedExpression -> expression.constantValue(state)
        is CfirBlock -> (statements.singleOrNull() as? CfirExpression)?.constantValue(state)
        is CfirLiteralExpression -> ConstValue.Literal(kind, value)
        is CfirTupleLiteral -> ConstValue.Tuple(elements.map { element -> element.constantValue(state) ?: ConstValue.Unknown })
        is CfirFunctionCall -> enumConstructorSymbolOrNull()?.let { constructor ->
            ConstValue.Enum(
                constructor = constructor,
                payload = argumentList.arguments.map { argument -> argument.enumPayloadValue() },
            )
        }

        else -> enumConstructorSymbolOrNull()?.let { constructor ->
            ConstValue.Enum(constructor, emptyList())
        } ?: referencedVariableSymbolOrNull()?.let(state::get)
    }

    /** 常量状态中的局部变量引用。 */
    private fun CfirExpression.referencedVariableSymbolOrNull(): CfirVariableSymbol<*>? =
        (this as? CfirQualifiedAccessExpression)?.resolvedVariableSymbolOrNull()

    /** 赋值左侧所引用的局部变量。 */
    private fun CfirAssignment.assignedVariableSymbolOrNull(): CfirVariableSymbol<*>? =
        lValue.referencedVariableSymbolOrNull()

    /** 从解析引用恢复变量符号，作为 CFG 常量事实跨节点共享的稳定 owner。 */
    private fun CfirQualifiedAccessExpression.resolvedVariableSymbolOrNull(): CfirVariableSymbol<*>? =
        when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirVariableSymbol<*>
            is CfirResolvedErrorReference -> reference.resolvedSymbol as? CfirVariableSymbol<*>
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirVariableSymbol<*>
            is CfirResolvedAppliedCallableReference -> reference.resolvedSymbol as? CfirVariableSymbol<*>
            else -> null
        }?.takeIf { it.isBound }

    /** 已解析 enum constructor 可出现在直接访问或 function call 中。 */
    private fun CfirExpression.enumConstructorSymbolOrNull(): CfirEnumConstructorSymbol? =
        (this as? CfirQualifiedAccessExpression)
            ?.calleeReference
            ?.resolvedEnumConstructorSymbolOrNull()

    /** 读取完成前后引用中已确定的符号。 */
    private fun CfirReference.resolvedSymbolOrNull(): CfirBasedSymbol<*>? = when (this) {
        is CfirResolvedNamedReference -> resolvedSymbol
        is CfirResolvedErrorReference -> resolvedSymbol
        is CfirNamedReferenceWithCandidateBase -> candidateSymbol
        is CfirResolvedAppliedCallableReference -> resolvedSymbol
        else -> null
    }

    /** 读取完成前后引用中已确定的 enum constructor。 */
    private fun CfirReference.resolvedEnumConstructorSymbolOrNull(): CfirEnumConstructorSymbol? =
        resolvedSymbolOrNull() as? CfirEnumConstructorSymbol

    /** enum payload 只传播嵌套 enum tag；普通字面量不属于 enum value-domain。 */
    private fun CfirExpression.enumPayloadValue(): ConstValue =
        enumConstructorSymbolOrNull()?.let { constructor ->
            val arguments = (this as? CfirFunctionCall)?.argumentList?.arguments.orEmpty()
            ConstValue.Enum(
                constructor = constructor,
                payload = arguments.map { argument -> argument.enumPayloadValue() },
            )
        } ?: ConstValue.Unknown

    /** 是否与官方 `CanOptimizeMatchToSwitch` 的已覆盖子集相同。 */
    private fun CfirMatchExpression.usesSwitchLowering(): Boolean =
        branches.size > 1 &&
            branches.firstOrNull()?.pattern !is CfirWildcardPattern &&
            branches.all { branch -> branch.guard == null && branch.pattern.isSwitchCompatiblePattern() }

    /** switch lowering 只接受 enum tag、整数/Rune 常量和通配的组合。 */
    private fun CfirPattern.isSwitchCompatiblePattern(): Boolean = when (this) {
        is CfirWildcardPattern -> true
        is CfirOrPattern -> alternatives.all { alternative -> alternative.isSwitchCompatiblePattern() }
        is CfirEnumPattern -> arguments.all { argument ->
            argument is CfirWildcardPattern || argument.isSwitchCompatibleConstant()
        }

        else -> false
    }

    private fun CfirPattern.isSwitchCompatibleConstant(): Boolean =
        when (this) {
            is CfirConstPattern -> (expression as? CfirLiteralExpression)?.kind in SWITCH_LITERAL_KINDS
            is CfirExpressionPattern -> (expression as? CfirLiteralExpression)?.kind in SWITCH_LITERAL_KINDS
            else -> false
        }

    /** CFG 传播使用的常量抽象域。 */
    private sealed interface ConstValue {
        /** 已知 enum constructor 及其 payload 常量域。 */
        data class Enum(
            val constructor: CfirEnumConstructorSymbol,
            val payload: List<ConstValue>,
        ) : ConstValue

        /** 已知字面量。 */
        data class Literal(
            val kind: CfirLiteralKind,
            val value: Any?,
        ) : ConstValue

        /** 已知 tuple；元素可以是 [Unknown]。 */
        data class Tuple(val elements: List<ConstValue>) : ConstValue

        /** 当前 CFG 状态无法确定的值。 */
        data object Unknown : ConstValue
    }

    /** 根据原子模式节点记录的 payload 路径读取对应 abstract value。 */
    private fun ConstValue.payloadAt(path: List<Int>): ConstValue? {
        var current = this
        for (index in path) {
            current = when (current) {
                is ConstValue.Enum -> current.payload.getOrNull(index) ?: return null
                is ConstValue.Tuple -> current.elements.getOrNull(index) ?: return null
                else -> return null
            }
        }
        return current
    }

    /**
     * CFG 程序点上的局部常量状态。
     *
     * 多前驱汇合只保留每条路径都相同的事实，因而循环、异常或未知调用不会错误地
     * 把某一路径的局部常量推广到另一条路径。
     */
    private class ConstState private constructor(
        private val values: IdentityHashMap<CfirVariableSymbol<*>, ConstValue>,
    ) {
        operator fun get(variable: CfirVariableSymbol<*>): ConstValue? = values[variable]

        fun put(variable: CfirVariableSymbol<*>, value: ConstValue) {
            if (value === ConstValue.Unknown) values.remove(variable) else values[variable] = value
        }

        fun clear(): ConstState = EMPTY

        fun copy(): ConstState = ConstState(IdentityHashMap(values))

        fun join(other: ConstState): ConstState {
            val result = copy()
            result.values.entries.removeIf { (variable, value) -> other.values[variable] != value }
            return result
        }

        override fun equals(other: Any?): Boolean =
            other is ConstState && values.size == other.values.size && values.all { (variable, value) -> other.values[variable] == value }

        override fun hashCode(): Int = values.entries.fold(0) { result, (variable, value) ->
            result + System.identityHashCode(variable) xor value.hashCode()
        }

        companion object {
            val EMPTY: ConstState = ConstState(IdentityHashMap())
        }
    }

    private companion object {
        val SWITCH_LITERAL_KINDS = setOf(
            CfirLiteralKind.INT,
            CfirLiteralKind.RUNE,
            CfirLiteralKind.BYTE,
        )
    }
}
