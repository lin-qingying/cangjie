package org.cangnova.cangjie.chir.core.controlflow

import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirNode
import org.cangnova.cangjie.chir.core.value.ChirValue

interface ChirTerminator : ChirNode

data class ChirReturnTerminator(
    override val semanticId: ChirSemanticId,
    val returnValue: ChirValue? = null,
) : ChirTerminator

data class ChirBranchTerminator(
    override val semanticId: ChirSemanticId,
    val targetBlockId: ChirSemanticId,
) : ChirTerminator

data class ChirConditionalBranchTerminator(
    override val semanticId: ChirSemanticId,
    val condition: ChirValue,
    val trueTargetBlockId: ChirSemanticId,
    val falseTargetBlockId: ChirSemanticId,
) : ChirTerminator

data class ChirThrowTerminator(
    override val semanticId: ChirSemanticId,
    val exceptionValue: ChirValue,
    val unwindTargetBlockId: ChirSemanticId? = null,
) : ChirTerminator

data class ChirUnwindTerminator(
    override val semanticId: ChirSemanticId,
    val targetBlockId: ChirSemanticId,
) : ChirTerminator

data class ChirBlock(
    override val semanticId: ChirSemanticId,
    val name: String,
    val expressions: List<ChirExpression>,
    val terminator: ChirTerminator,
) : ChirNode
