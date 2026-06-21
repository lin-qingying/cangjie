package org.cangnova.cangjie.chir.core.expression

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirNode
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.value.ChirValue

enum class ChirExpressionDomain {
    UNARY,
    BINARY,
    MEMORY,
    CALL,
    OTHERS,
}

interface ChirExpression : ChirNode {
    val domain: ChirExpressionDomain
    val resultType: ChirTypeRef?
}

data class ChirUnaryExpression(
    override val semanticId: ChirSemanticId,
    val operator: String,
    val operand: ChirValue,
    override val resultType: ChirTypeRef,
) : ChirExpression {
    override val domain: ChirExpressionDomain = ChirExpressionDomain.UNARY
}

data class ChirBinaryExpression(
    override val semanticId: ChirSemanticId,
    val operator: String,
    val left: ChirValue,
    val right: ChirValue,
    override val resultType: ChirTypeRef,
) : ChirExpression {
    override val domain: ChirExpressionDomain = ChirExpressionDomain.BINARY
}

data class ChirMemoryExpression(
    override val semanticId: ChirSemanticId,
    val operation: String,
    val address: ChirValue,
    val value: ChirValue? = null,
    override val resultType: ChirTypeRef? = null,
) : ChirExpression {
    override val domain: ChirExpressionDomain = ChirExpressionDomain.MEMORY
}

data class ChirCallExpression(
    override val semanticId: ChirSemanticId,
    val callee: ChirValue,
    val arguments: List<ChirValue>,
    override val resultType: ChirTypeRef,
) : ChirExpression {
    override val domain: ChirExpressionDomain = ChirExpressionDomain.CALL
}

data class ChirOtherExpression(
    override val semanticId: ChirSemanticId,
    val operation: String,
    val operands: List<ChirValue>,
    override val resultType: ChirTypeRef? = null,
    /**
     * 后端专用表达式契约属性。
     *
     * CHIR 仍然是 JVM 后端的唯一输入；字段、构造器等 JVM ABI 信息必须通过结构化属性传入，
     * 后端禁止从展示名称或 operation 文本中猜测 owner/name/descriptor。
     */
    val attributes: Set<ChirAttribute> = emptySet(),
) : ChirExpression {
    override val domain: ChirExpressionDomain = ChirExpressionDomain.OTHERS
}
