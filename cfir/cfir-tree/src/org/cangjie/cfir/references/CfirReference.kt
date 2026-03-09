package org.cangjie.cfir.references

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirSourceElement
import org.cangnova.cangjie.name.Name
import org.cangjie.cfir.symbols.CfirSymbol
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 引用基接口。用于在表达式中引用声明。
 */
sealed interface CfirReference : CfirElement

/**
 * 命名引用（未解析），保存名称但尚未绑定到符号。
 */
class CfirNamedReference(
    override val source: CfirSourceElement? = null,
    val name: Name,
) : CfirReference {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitNamedReference(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirNamedReference = this

    override fun toString(): String = name.asString()
}

/**
 * 已解析命名引用（指向 CfirSymbol）。
 */
class CfirResolvedNamedReference(
    override val source: CfirSourceElement? = null,
    val name: Name,
    val resolvedSymbol: CfirSymbol<*>,
) : CfirReference {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitResolvedNamedReference(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirResolvedNamedReference = this

    override fun toString(): String = "${name.asString()} -> $resolvedSymbol"
}

/**
 * 错误引用（解析失败时的占位符）。
 */
class CfirErrorReference(
    override val source: CfirSourceElement? = null,
    val reason: String,
) : CfirReference {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitErrorReference(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirErrorReference = this

    override fun toString(): String = "ERROR_REF($reason)"
}
