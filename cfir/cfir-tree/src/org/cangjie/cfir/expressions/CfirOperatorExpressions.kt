package org.cangjie.cfir.expressions

import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.references.CfirReference
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 赋值表达式，对应仓颉编译器中的 AssignExpr。
 */
class CfirAssignment(
    override val source: CfirSourceElement? = null,
    /** 赋值目标（左值） */
    var lValue: CfirExpression,
    /** 赋值源（右值） */
    var rValue: CfirExpression,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitAssignment(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirAssignment {
        lValue = lValue.accept(transformer, data) as CfirExpression
        rValue = rValue.accept(transformer, data) as CfirExpression
        return this
    }
}

/**
 * 二元运算符种类（逻辑运算等不可重载的运算符）。
 */
enum class CfirBinaryOpKind {
    AND,    // &&
    OR,     // ||
    COALESCING,  // ??（仓颉空合运算符）
    PIPELINE,    // |>（仓颉管道运算符）
}

/**
 * 二元运算表达式（逻辑运算等不可重载的运算符）。
 */
class CfirBinaryOp(
    override val source: CfirSourceElement? = null,
    val kind: CfirBinaryOpKind,
    var left: CfirExpression,
    var right: CfirExpression,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitBinaryOp(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirBinaryOp {
        left = left.accept(transformer, data) as CfirExpression
        right = right.accept(transformer, data) as CfirExpression
        return this
    }
}

/**
 * 比较表达式，对应仓颉编译器中的比较运算。
 */
class CfirComparisonExpression(
    override val source: CfirSourceElement? = null,
    val operation: CfirComparisonOp,
    var left: CfirExpression,
    var right: CfirExpression,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitComparisonExpression(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirComparisonExpression {
        left = left.accept(transformer, data) as CfirExpression
        right = right.accept(transformer, data) as CfirExpression
        return this
    }
}

enum class CfirComparisonOp {
    LT, GT, LE, GE, EQ, NE,
}
