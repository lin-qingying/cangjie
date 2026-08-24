package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.enumPatternConstructorAccessOrNull
import org.cangnova.cangjie.cfir.declarations.payloadArity
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirIncrementDecrementExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CFGNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.EdgeLabel
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.FunctionCallExitNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.IncrementDecrementNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.MatchBranchConditionEnterNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.MatchBranchFailure
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.MatchBranchFailureNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.MatchBranchSuccess
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.MatchPatternDecisionNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.VariableAssignmentNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.VariableDeclarationExitNode
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.name.OperatorNameConventions
import java.math.BigInteger
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
 * 抽象域严格对齐官方 `ValueAnalysis`：`ConstValue` 只有 UINT/INT/FLOAT/RUNE/BOOL/STRING，
 * 没有 enum 抽象值。非平凡 enum 的 constructor tag 之所以可知，是因为 `GetEnumIDValue`
 * 直接对 selector 取 `Field #0`；而 payload 需要先 `TypeCast` 成 tuple 类型，平凡 enum
 * 也要经 `TypeCast` 在整数与 enum 类型间往返，`TypeCast` 对 ValueAnalysis 不透明，
 * 两者都因此退化为未知。故本分析只跟踪"存在带参构造器"的 enum 的外层 tag。
 */
internal class CfirControlFlowConstAnalysis {
    /** 缓存 enum 是否在官方常量域中保留 constructor tag，避免重复查询宿主声明。 */
    private val constructorTagTracking = IdentityHashMap<CfirEnumConstructorSymbol, Boolean>()

    /** 返回由 CFG 常量分支效应证明不可达的 pattern source。 */
    fun collectUnreachablePatterns(graph: ControlFlowGraph): Set<CfirPattern> {
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
                    continue
                }

                val merged = incomingStates[successor]?.join(output) ?: output
                if (incomingStates[successor] != merged) {
                    incomingStates[successor] = merged
                    worklist += successor
                }
            }
        }

        /*
         * 先求到不动点，再读取被拒绝边。循环首轮可能用 `i = 0` 暂时裁掉一个 case，
         * 但回边会把入口状态与 `i = 1` 汇合为未知；在传播阶段直接报告会遗留这个已
         * 失效的结论。官方 ValueAnalysis 也只在其状态稳定后由 UnreachableBranchCheck
         * 消费最终 CFG 常量事实。
         */
        val unreachablePatterns = Collections.newSetFromMap(IdentityHashMap<CfirPattern, Boolean>())
        for ((node, input) in incomingStates) {
            val target = node.constantTargetSuccessor(input.transfer(node)) ?: continue
            val decisionNode = node as? MatchPatternDecisionNode ?: continue
            for (successor in node.followingNodes) {
                val edge = node.edgeTo(successor)
                if (!edge.kind.usedInCfa || edge.kind.isDead || successor === target) continue
                decisionNode.recordRejectedSuccessor(
                    edgeLabel = edge.label,
                    rejectedSuccessor = successor,
                    destination = unreachablePatterns,
                )
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
     * 把被常量分析杀死的 successor 还原为其对应的 pattern source。
     *
     * 官方 `UnreachableBranchCheck::RunOnFunc` 对 *每一个* 非 target successor 调用
     * `PrintWarning`，与被杀的是 success 还是 failure 边无关：
     * - success 边被杀时死块是本 case 的 trueBlock，它携带当前原子模式的位置；
     * - failure 边被杀时死块是下一 case 的判定块，它携带下一个 case 的模式位置。
     *
     * 因此这里必须先补上 success 方向自身的 [reportSource]（or-pattern 下它精确到被拒
     * 的 alternative，共享的 condition exit 无法表达这一点），再沿死区继续收集。
     */
    private fun MatchPatternDecisionNode.recordRejectedSuccessor(
        edgeLabel: EdgeLabel,
        rejectedSuccessor: CFGNode<*>,
        destination: MutableSet<CfirPattern>,
    ) {
        if (edgeLabel == MatchBranchSuccess) {
            destination += reportSource
        }
        rejectedSuccessor.collectDeadRegionPatterns(matchExpression, destination)
    }

    /**
     * 从被杀死的 successor 出发收集死区内承载模式位置的节点。
     *
     * 对位 `PrintWarning`：它按块去重，先递归进入死块的全部后继，再报告死块自身的
     * debug location；且只有以 `Branch`/`MultiBranch` 结尾的死块才继续递归。case body
     * 与 match 出口都由无条件跳转进入，递归到此为止，因此不会把仍可执行的分支体误判
     * 为不可达。
     */
    private fun CFGNode<*>.collectDeadRegionPatterns(
        owner: CfirMatchExpression,
        destination: MutableSet<CfirPattern>,
    ) {
        val visited = Collections.newSetFromMap(IdentityHashMap<CFGNode<*>, Boolean>())
        val worklist = ArrayDeque<CFGNode<*>>()
        worklist += this
        while (worklist.isNotEmpty()) {
            val node = worklist.removeFirst()
            if (!visited.add(node)) continue
            node.deadRegionPatternOrNull(owner)?.let { pattern -> destination += pattern }
            if (!node.continuesDeadRegion(owner)) continue
            node.followingNodes.forEach { successor ->
                if (node.edgeTo(successor).kind.usedInCfa) {
                    worklist += successor
                }
            }
        }
    }

    /**
     * 死块所承载的模式位置。
     *
     * CHIR 把 case 的模式位置写到该 case 的判定块上；没有原子判定的通配/纯绑定 case
     * 只生成一条 `GoTo`，其模式位置只存在于 case 入口块。因此有判定的 case 由判定节点的
     * [MatchPatternDecisionNode.reportSource] 表达（or-pattern 下精确到 alternative），
     * 无判定的 case 才回到分支入口节点取整条模式。
     */
    private fun CFGNode<*>.deadRegionPatternOrNull(owner: CfirMatchExpression): CfirPattern? = when {
        this is MatchPatternDecisionNode && matchExpression === owner -> reportSource
        this is MatchBranchConditionEnterNode && matchExpression === owner &&
            followingNodes.none { successor -> successor is MatchPatternDecisionNode } -> fir.pattern

        else -> null
    }

    /** 是否对应官方"以条件终结符结尾"的死块，只有这类块才继续向后递归。 */
    private fun CFGNode<*>.continuesDeadRegion(owner: CfirMatchExpression): Boolean = when (this) {
        is MatchPatternDecisionNode -> matchExpression === owner
        is MatchBranchFailureNode -> matchExpression === owner
        is MatchBranchConditionEnterNode -> matchExpression === owner
        else -> false
    }

    /** 对 CFG 节点执行常量状态 transfer；只在节点的正规求值位置读写局部事实。 */
    private fun ConstState.transfer(node: CFGNode<*>): ConstState = when (node) {
        is VariableDeclarationExitNode -> assign(node.fir.symbol, node.fir.initializer)
        is VariableAssignmentNode -> assign(node.fir.assignedVariableSymbolOrNull(), node.fir.rValue)
        is IncrementDecrementNode -> incrementOrDecrement(node.fir)
        // 官方 ConstAnalysis 不会因普通 APPLY 擦除其他局部变量的已知事实。
        is FunctionCallExitNode -> this
        else -> this
    }

    /** 把一个已知表达式值写入局部变量；未知值会清除旧事实。 */
    private fun ConstState.assign(variable: CfirVariableSymbol<*>?, expression: CfirExpression?): ConstState {
        if (variable == null) return this
        val value = expression?.constantValue(this) ?: ConstValue.Unknown
        return copy().also { state -> state.put(variable, value) }
    }

    /**
     * 将 `++` / `--` 作为局部 Store 处理。
     *
     * 官方先把它降糖成 `x += 1`，再由 CHIR 的 Store 写回局部存储位置。这里对可解析的
     * 整数字面量保留精确值；未知、非局部目标或可能溢出的结果则仅移除旧事实，绝不伪造值。
     */
    private fun ConstState.incrementOrDecrement(expression: CfirIncrementDecrementExpression): ConstState {
        val variable = expression.expression.referencedVariableSymbolOrNull() ?: return this
        val nextValue = (get(variable) as? ConstValue.Literal)
            ?.incrementedOrDecremented(expression)
            ?: ConstValue.Unknown
        return copy().also { state -> state.put(variable, nextValue) }
    }

    /** 计算官方 `x += 1` 降糖的整数结果；不能精确表示时回退为未知状态。 */
    private fun ConstValue.Literal.incrementedOrDecremented(
        expression: CfirIncrementDecrementExpression,
    ): ConstValue.Literal? {
        if (kind != CfirLiteralKind.INT) return null
        val delta = when (expression.operationName) {
            OperatorNameConventions.INC -> BigInteger.ONE
            OperatorNameConventions.DEC -> BigInteger.ONE.negate()
            else -> return null
        }
        val parsed = CfirIntConstantEvalUtils.parseIntLiteralValue(value) ?: return null
        val next = parsed.value + delta
        val range = CfirIntConstantEvalUtils.rangeForLiteralTargetType(expression.expression.coneTypeOrNull)
            ?: return null
        if (!range.contains(next)) return null
        return ConstValue.Literal(
            kind = CfirLiteralKind.INT,
            value = next.toString() + parsed.explicitSuffix.orEmpty(),
        )
    }

    /** 常量 bool 仅接受已知 Boolean 字面量。 */
    private fun CfirExpression.constantBoolean(state: ConstState): Boolean? =
        (constantValue(state) as? ConstValue.Literal)?.takeIf { it.kind == CfirLiteralKind.BOOLEAN }?.value as? Boolean

    /**
     * 比较 lowering 中可用的已知 abstract value 与已解析 pattern。
     *
     * 只有 selector 自身的 constructor tag 属于抽象域；payload 在官方 ValueAnalysis 中
     * 已退化为未知，因此嵌套判定只能返回 null，由调用方保留全部后继。
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

    /**
     * 常量 pattern 和 expression pattern 都按已知常量值精确比较。
     *
     * 任一侧未知时结果必须是"未确定"。把已知字面量与 [ConstValue.Unknown] 判成不等会
     * 凭空造出死边，从而误报本可执行的分支。
     */
    private fun CfirExpression.matchesConstantExpression(value: ConstValue, state: ConstState): Boolean? {
        if (!value.isFullyKnown()) return null
        val expected = constantValue(state)?.takeIf { it.isFullyKnown() } ?: return null
        return expected == value
    }

    /** 从 CFG 节点拥有的已解析表达式计算常量 domain。 */
    private fun CfirExpression.constantValue(state: ConstState): ConstValue? = when (this) {
        is CfirWrappedExpression -> expression.constantValue(state)
        is CfirBlock -> (statements.singleOrNull() as? CfirExpression)?.constantValue(state)
        is CfirLiteralExpression -> ConstValue.Literal(kind, value)
        is CfirTupleLiteral -> ConstValue.Tuple(elements.map { element -> element.constantValue(state) ?: ConstValue.Unknown })
        is CfirFunctionCall -> trackedEnumConstructorOrNull()?.let(ConstValue::Enum)

        else -> trackedEnumConstructorOrNull()?.let(ConstValue::Enum)
            ?: referencedVariableSymbolOrNull()?.let(state::get)
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

    /** 只有其 constructor tag 仍在官方常量域内的 enum 才进入抽象值。 */
    private fun CfirExpression.trackedEnumConstructorOrNull(): CfirEnumConstructorSymbol? =
        enumConstructorSymbolOrNull()?.takeIf { constructor -> constructor.tracksConstructorTag() }

    /**
     * 判断 enum 的 constructor tag 是否仍可被常量分析恢复。
     *
     * 官方以 `EnumDecl::hasArguments` 区分两种 lowering：存在带参构造器时 enum 值是
     * `Tuple(tag, payload...)`，`GetEnumIDValue` 直接取 `Field #0` 即得 tag；全部构造器
     * 都是零参时 enum 退化为整数，构造与判定两侧都要经 `TypeCast` 在整数与 enum 类型
     * 之间往返，tag 随之离开抽象域。
     */
    private fun CfirEnumConstructorSymbol.tracksConstructorTag(): Boolean =
        constructorTagTracking.getOrPut(this) {
            if (!isBound) return@getOrPut false
            val enumClassId = callableId.classId ?: return@getOrPut false
            val enumSymbol = cfir.moduleData.session.symbolProvider
                .getClassLikeSymbolByClassId(enumClassId)
                ?.takeIf { symbol -> symbol.isBound }
                ?: return@getOrPut false
            enumSymbol.cfir.declarations
                .filterIsInstance<CfirEnumConstructor>()
                .any { constructor -> constructor.payloadArity() > 0 }
        }

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

    /** CFG 传播使用的常量抽象域。 */
    private sealed interface ConstValue {
        /** 已知 enum constructor tag；payload 不属于抽象域。 */
        data class Enum(val constructor: CfirEnumConstructorSymbol) : ConstValue

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

    /** 值本身及其全部分量是否都已确定，只有此时才允许做相等判定。 */
    private fun ConstValue.isFullyKnown(): Boolean = when (this) {
        is ConstValue.Unknown -> false
        is ConstValue.Tuple -> elements.all { element -> element.isFullyKnown() }
        else -> true
    }

    /**
     * 根据原子模式节点记录的 payload 路径读取对应 abstract value。
     *
     * enum payload 需要经 `TypeCast` 才能取出，官方常量域到此即止，因此任何进入 enum 的
     * 路径都返回 null；tuple 的分量则由 `Field` 直接读取，可以继续下探。
     */
    private fun ConstValue.payloadAt(path: List<Int>): ConstValue? {
        var current = this
        for (index in path) {
            current = when (current) {
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
}
