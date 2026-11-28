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
    if (!function.hasBody) {
        return if (function.isImported && function.blocks.isEmpty() && function.entryBlockId == null) {
            emptyList()
        } else {
            listOf("only imported functions may omit a body, blocks, and entry block")
        }
    }
    if (function.isImported) return listOf("imported function ${function.name} must not contain a body")
    if (function.blocks.isEmpty()) return listOf("function ${function.name} has no blocks")
    val entryBlockId = function.entryBlockId
        ?: return listOf("function ${function.name} has no entry block")

    val blockIndex = function.blocks.associateBy(ChirBlock::semanticId)
    val errors = mutableListOf<String>()
    val successorIds = function.blocks.flatMap { block ->
        when (val terminator = block.terminator) {
            is ChirBranchTerminator -> listOf(terminator.targetBlockId)
            is ChirConditionalBranchTerminator -> listOf(terminator.trueTargetBlockId, terminator.falseTargetBlockId)
            is ChirUnwindTerminator -> listOf(terminator.targetBlockId)
            else -> emptyList()
        }
    }.toSet()

    if (entryBlockId !in blockIndex) {
        errors += "entry block $entryBlockId does not exist"
    }

    function.blocks.forEach { block ->
        if (block.terminator == null && (block.semanticId == entryBlockId || block.semanticId in successorIds)) {
            errors += "reachable block ${block.semanticId} has no terminator"
        }
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
