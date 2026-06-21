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

object ChirReachabilityAnalysis {
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

    private fun successors(block: ChirBlock): Set<ChirSemanticId> {
        return when (val terminator = block.terminator) {
            is ChirBranchTerminator -> setOf(terminator.targetBlockId)
            is ChirConditionalBranchTerminator -> setOf(terminator.trueTargetBlockId, terminator.falseTargetBlockId)
            is ChirUnwindTerminator -> setOf(terminator.targetBlockId)
            else -> emptySet()
        }
    }
}

object ChirTypeFlowAnalysis {
    fun expressionTypes(function: ChirFunctionDeclaration): Map<ChirSemanticId, ChirTypeRef> {
        return function.blocks
            .flatMap { it.expressions }
            .mapNotNull { expression ->
                expression.resultType?.let { expression.semanticId to it }
            }
            .toMap()
    }
}

object ChirConstValueAnalysis {
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

    private fun collectValue(value: ChirValue, output: MutableMap<ChirSemanticId, String>) {
        if (value is ChirConstantValue) {
            output[value.semanticId] = value.literal
        }
    }
}
