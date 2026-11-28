package org.cangnova.cangjie.cfir.resolve.dfa

import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral

/**
 * 官方 DesugarAssignExprRecursively 的目标序列：保留真实节点与完整槽位路径。
 * 根右值先完成一次，再按源序消费各叶；投影本身不创建 AST 或执行右值。
 * 复合赋值的 get/op 顺序由其真实降低产品提供，不能当作普通多重赋值重排。
 */
internal class CfirAssignmentExecution private constructor(
    val assignment: CfirAssignment,
    val targets: List<Target>,
) {
    class Target(val expression: CfirExpression, path: List<Int>) {
        val path: List<Int> = path.toPersistentList()
    }

    sealed class Completion<out Value> {
        data class Normal<Value>(val value: Value) : Completion<Value>()
        data object NoNormalValue : Completion<Nothing>()
    }

    enum class WriteResult { CONTINUE, TERMINATED }

    interface Visitor<Value> {
        fun evaluateRhs(expression: CfirExpression): Completion<Value>
        fun project(root: Value, path: List<Int>): Value
        fun write(target: Target, value: Value): WriteResult
    }

    fun <Value> accept(visitor: Visitor<Value>) {
        val root = when (val completion = visitor.evaluateRhs(assignment.rValue)) {
            is Completion.Normal -> completion.value
            Completion.NoNormalValue -> return
        }
        for (target in targets) {
            if (visitor.write(target, visitor.project(root, target.path)) == WriteResult.TERMINATED) return
        }
    }

    companion object {
        fun of(assignment: CfirAssignment): CfirAssignmentExecution {
            val targets = mutableListOf<Target>()
            fun visit(expression: CfirExpression, path: List<Int>) {
                if (expression is CfirTupleLiteral) {
                    expression.elements.forEachIndexed { index, element -> visit(element, path + index) }
                } else {
                    targets += Target(expression, path.toList())
                }
            }
            visit(assignment.lValue, emptyList())
            return CfirAssignmentExecution(assignment, targets.toList())
        }
    }
}

/** 同一声明的重复槽位属于不同写事件；owner 是真实语法/降低节点，不是位置猜测。 */
internal class AssignmentOccurrence(val owner: CfirElement, path: List<Int>) {
    val path: List<Int> = path.toPersistentList()

    fun withOwner(owner: CfirElement): AssignmentOccurrence = AssignmentOccurrence(owner, path)

    override fun equals(other: Any?): Boolean =
        other is AssignmentOccurrence && owner === other.owner && path == other.path

    override fun hashCode(): Int = 31 * System.identityHashCode(owner) + path.hashCode()
}
