package org.cangnova.cangjie.chir.core.analysis

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirUnwindTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirValue

/**
 * CHIR 可达基本块分析。
 */
object ChirReachabilityAnalysis {
    /**
     * 计算函数入口可达的基本块标识集合。
     */
    fun reachableBlocks(function: ChirFunctionDeclaration): Set<ChirSemanticId> {
        val blockById = function.blocks.associateBy { it.semanticId }
        val visited = linkedSetOf<ChirSemanticId>()
        val worklist = ArrayDeque<ChirSemanticId>()
        worklist += function.entryBlockId

        while (worklist.isNotEmpty()) {
            val current = worklist.removeFirst()
            if (!visited.add(current)) continue
            val block = blockById[current] ?: continue
            successors(block).forEach { next ->
                if (next !in visited) worklist += next
            }
        }
        return visited
    }

    /**
     * 获取基本块终结指令指向的后继基本块集合。
     */
    private fun successors(block: ChirBlock): Set<ChirSemanticId> {
        return when (val terminator = block.terminator) {
            is ChirBranchTerminator -> setOf(terminator.targetBlockId)
            is ChirConditionalBranchTerminator -> setOf(terminator.trueTargetBlockId, terminator.falseTargetBlockId)
            is ChirUnwindTerminator -> setOf(terminator.targetBlockId)
            else -> emptySet()
        }
    }
}

/**
 * CHIR 表达式类型流分析。
 */
object ChirTypeFlowAnalysis {
    /**
     * 收集函数内所有有结果类型表达式的类型映射。
     */
    fun expressionTypes(function: ChirFunctionDeclaration): Map<ChirSemanticId, ChirTypeRef> {
        return function.blocks
            .flatMap { it.expressions }
            .mapNotNull { expression ->
                expression.resultType?.let { expression.semanticId to it }
            }
            .toMap()
    }
}

/**
 * CHIR 常量值分析。
 */
object ChirConstValueAnalysis {
    /**
     * 收集函数表达式操作数中出现的常量字面量。
     */
    fun constants(function: ChirFunctionDeclaration): Map<ChirSemanticId, String> {
        val map = linkedMapOf<ChirSemanticId, String>()
        function.blocks.forEach { block ->
            block.expressions.forEach { expression ->
                when (expression) {
                    is ChirUnaryExpression -> collectValue(expression.operand, map)
                    is ChirBinaryExpression -> {
                        collectValue(expression.left, map)
                        collectValue(expression.right, map)
                    }
                    is ChirCallExpression -> {
                        collectValue(expression.callee, map)
                        expression.arguments.forEach { collectValue(it, map) }
                    }
                    is ChirMemoryExpression -> {
                        collectValue(expression.address, map)
                        expression.value?.let { collectValue(it, map) }
                    }
                    is ChirOtherExpression -> expression.operands.forEach { collectValue(it, map) }
                }
            }
        }
        return map
    }

    /**
     * 如果 [value] 是常量值，则写入 [output]。
     */
    private fun collectValue(value: ChirValue, output: MutableMap<ChirSemanticId, String>) {
        if (value is ChirConstantValue) {
            output[value.semanticId] = value.literal
        }
    }
}
