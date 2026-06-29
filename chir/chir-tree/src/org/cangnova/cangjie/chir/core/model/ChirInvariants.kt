package org.cangnova.cangjie.chir.core.model

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirUnwindTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration

/**
 * 校验函数的最小控制流不变量。
 */
fun validateMinimalControlFlow(function: ChirFunctionDeclaration): List<String> {
    if (function.blocks.isEmpty()) return listOf("function ${function.name} has no blocks")

    val blockIndex = function.blocks.associateBy(ChirBlock::semanticId)
    val errors = mutableListOf<String>()

    if (function.entryBlockId !in blockIndex) {
        errors += "entry block ${function.entryBlockId} does not exist"
    }

    function.blocks.forEach { block ->
        when (val terminator = block.terminator) {
            is ChirBranchTerminator -> if (terminator.targetBlockId !in blockIndex) {
                errors += "block ${block.semanticId} branches to missing block ${terminator.targetBlockId}"
            }
            is ChirConditionalBranchTerminator -> {
                if (terminator.trueTargetBlockId !in blockIndex) {
                    errors += "block ${block.semanticId} has missing true target ${terminator.trueTargetBlockId}"
                }
                if (terminator.falseTargetBlockId !in blockIndex) {
                    errors += "block ${block.semanticId} has missing false target ${terminator.falseTargetBlockId}"
                }
            }
            is ChirUnwindTerminator -> if (terminator.targetBlockId !in blockIndex) {
                errors += "block ${block.semanticId} unwinds to missing block ${terminator.targetBlockId}"
            }
        }
    }

    return errors
}
