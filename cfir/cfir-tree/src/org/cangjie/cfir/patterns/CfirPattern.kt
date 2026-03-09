package org.cangjie.cfir.patterns

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirSourceElement
import org.cangnova.cangjie.name.Name
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.references.CfirReference
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 模式基接口。仓颉拥有丰富的模式匹配系统，
 * 对应仓颉编译器中的 Pattern 子类层次。
 */
interface CfirPattern : CfirElement

/**
 * 常量模式，匹配常量值。
 */
class CfirConstPattern(
    override val source: CfirSourceElement? = null,
    val expression: CfirExpression,
) : CfirPattern {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitConstPattern(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirConstPattern = this
}

/**
 * 通配符模式（ _ ），匹配任意值。
 */
class CfirWildcardPattern(
    override val source: CfirSourceElement? = null,
) : CfirPattern {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitWildcardPattern(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirWildcardPattern = this
}

/**
 * 绑定模式，将匹配的值绑定到变量。
 */
class CfirBindingPattern(
    override val source: CfirSourceElement? = null,
    val name: Name,
    /** 可选的类型约束 */
    val typeRef: CfirTypeRef? = null,
    /** 嵌套的子模式 */
    val nestedPattern: CfirPattern? = null,
) : CfirPattern {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitBindingPattern(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirBindingPattern = this
}

/**
 * 元组模式，匹配元组结构。
 */
class CfirTuplePattern(
    override val source: CfirSourceElement? = null,
    val elements: List<CfirPattern>,
) : CfirPattern {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitTuplePattern(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTuplePattern = this
}

/**
 * 枚举模式，匹配枚举构造器（ADT 解构）。
 *
 * 例如：case RGB(r, g, b) => ...
 */
class CfirEnumPattern(
    override val source: CfirSourceElement? = null,
    /** 引用的枚举构造器 */
    val constructorReference: CfirReference,
    /** 构造器参数的子模式 */
    val arguments: List<CfirPattern> = emptyList(),
) : CfirPattern {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitEnumPattern(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirEnumPattern = this
}

/**
 * 类型模式，匹配特定类型。
 */
class CfirTypePattern(
    override val source: CfirSourceElement? = null,
    val typeRef: CfirTypeRef,
    /** 绑定名称（可选） */
    val bindingName: Name? = null,
) : CfirPattern {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitTypePattern(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTypePattern = this
}
