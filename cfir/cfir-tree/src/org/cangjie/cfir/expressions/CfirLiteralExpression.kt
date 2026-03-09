package org.cangjie.cfir.expressions

import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 字面量种类。
 */
enum class CfirLiteralKind {
    INT,
    FLOAT,
    BOOLEAN,
    RUNE,
    STRING,
    UNIT,
    NULL,
}

/**
 * 字面量表达式，对应仓颉编译器中的各种字面量 Expr。
 */
class CfirLiteralExpression(
    override val source: CfirSourceElement? = null,
    val kind: CfirLiteralKind,
    val value: Any?,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitLiteralExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirLiteralExpression = this

    override fun toString(): String = value?.toString() ?: "null"
}

/**
 * 字符串插值表达式，对应仓颉编译器中的 StringInterpolationExpr。
 *
 * 仓颉字符串插值："\(expr)"
 */
class CfirStringInterpolation(
    override val source: CfirSourceElement? = null,
    /** 交替出现的字符串常量和插值表达式 */
    val parts: List<CfirExpression> = emptyList(),
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitStringInterpolation(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirStringInterpolation = this
}
