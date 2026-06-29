package org.cangnova.cangjie.chir.core.expression

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirNode
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirValue

/**
 * CHIR 表达式所属的语义域。
 */
enum class ChirExpressionDomain {
    UNARY,
    BINARY,
    MEMORY,
    CALL,
    OTHERS,
}

/**
 * CHIR 表达式节点公共接口。
 */
interface ChirExpression : ChirNode {
    /**
     * 表达式所属语义域。
     */
    val domain: ChirExpressionDomain

    /**
     * 表达式结果类型；无值表达式或未知类型时为 `null`。
     */
    val resultType: ChirTypeRef?
}

/**
 * 一元表达式。
 */
data class ChirUnaryExpression(
    /**
     * 表达式语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 一元操作符名称。
     */
    val operator: String,

    /**
     * 一元操作数。
     */
    val operand: ChirValue,

    /**
     * 一元表达式结果类型。
     */
    override val resultType: ChirTypeRef,
) : ChirExpression {
    /**
     * 一元表达式的固定语义域。
     */
    override val domain: ChirExpressionDomain = ChirExpressionDomain.UNARY
}

/**
 * 二元表达式。
 */
data class ChirBinaryExpression(
    /**
     * 表达式语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 二元操作符名称。
     */
    val operator: String,

    /**
     * 左操作数。
     */
    val left: ChirValue,

    /**
     * 右操作数。
     */
    val right: ChirValue,

    /**
     * 二元表达式结果类型。
     */
    override val resultType: ChirTypeRef,
) : ChirExpression {
    /**
     * 二元表达式的固定语义域。
     */
    override val domain: ChirExpressionDomain = ChirExpressionDomain.BINARY
}

/**
 * 内存操作表达式。
 */
data class ChirMemoryExpression(
    /**
     * 表达式语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 内存操作名称。
     */
    val operation: String,

    /**
     * 内存地址值。
     */
    val address: ChirValue,

    /**
     * 写入或交换等操作使用的值；纯读取操作可为空。
     */
    val value: ChirValue? = null,

    /**
     * 内存表达式结果类型。
     */
    override val resultType: ChirTypeRef? = null,
) : ChirExpression {
    /**
     * 内存表达式的固定语义域。
     */
    override val domain: ChirExpressionDomain = ChirExpressionDomain.MEMORY
}

/**
 * 调用表达式。
 */
data class ChirCallExpression(
    /**
     * 表达式语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 被调用目标值。
     */
    val callee: ChirValue,

    /**
     * 调用实参值列表。
     */
    val arguments: List<ChirValue>,

    /**
     * 调用表达式结果类型。
     */
    override val resultType: ChirTypeRef,
) : ChirExpression {
    /**
     * 调用表达式的固定语义域。
     */
    override val domain: ChirExpressionDomain = ChirExpressionDomain.CALL
}

/**
 * 其他后端或平台专用表达式。
 */
data class ChirOtherExpression(
    /**
     * 表达式语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 平台专用操作名称。
     */
    val operation: String,

    /**
     * 操作数列表。
     */
    val operands: List<ChirValue>,

    /**
     * 表达式结果类型。
     */
    override val resultType: ChirTypeRef? = null,
    /**
     * 后端专用表达式契约属性。
     *
     * CHIR 仍然是 JVM 后端的唯一输入；字段、构造器等 JVM ABI 信息必须通过结构化属性传入，
     * 后端禁止从展示名称或 operation 文本中猜测 owner/name/descriptor。
     */
    val attributes: Set<ChirAttribute> = emptySet(),
) : ChirExpression {
    /**
     * 其他表达式的固定语义域。
     */
    override val domain: ChirExpressionDomain = ChirExpressionDomain.OTHERS
}
