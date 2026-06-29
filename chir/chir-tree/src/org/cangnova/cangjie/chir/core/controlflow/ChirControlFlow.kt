package org.cangnova.cangjie.chir.core.controlflow

import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirNode
import org.cangnova.cangjie.chir.core.value.ChirValue

/**
 * CHIR 基本块终结指令公共接口。
 */
interface ChirTerminator : ChirNode

/**
 * return 终结指令。
 */
data class ChirReturnTerminator(
    /**
     * 终结指令语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 返回值；返回 Unit 或无值返回时为 `null`。
     */
    val returnValue: ChirValue? = null,
) : ChirTerminator

/**
 * 无条件跳转终结指令。
 */
data class ChirBranchTerminator(
    /**
     * 终结指令语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 目标基本块标识。
     */
    val targetBlockId: ChirSemanticId,
) : ChirTerminator

/**
 * 条件跳转终结指令。
 */
data class ChirConditionalBranchTerminator(
    /**
     * 终结指令语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 条件值。
     */
    val condition: ChirValue,

    /**
     * 条件为真时的目标基本块标识。
     */
    val trueTargetBlockId: ChirSemanticId,

    /**
     * 条件为假时的目标基本块标识。
     */
    val falseTargetBlockId: ChirSemanticId,
) : ChirTerminator

/**
 * throw 终结指令。
 */
data class ChirThrowTerminator(
    /**
     * 终结指令语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 被抛出的异常值。
     */
    val exceptionValue: ChirValue,

    /**
     * unwind 目标基本块标识；无异常恢复路径时为 `null`。
     */
    val unwindTargetBlockId: ChirSemanticId? = null,
) : ChirTerminator

/**
 * 异常 unwind 终结指令。
 */
data class ChirUnwindTerminator(
    /**
     * 终结指令语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * unwind 目标基本块标识。
     */
    val targetBlockId: ChirSemanticId,
) : ChirTerminator

/**
 * CHIR 控制流基本块。
 */
data class ChirBlock(
    /**
     * 基本块语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 基本块名称。
     */
    val name: String,

    /**
     * 基本块内顺序执行的表达式列表。
     */
    val expressions: List<ChirExpression>,

    /**
     * 基本块终结指令。
     */
    val terminator: ChirTerminator,
) : ChirNode
