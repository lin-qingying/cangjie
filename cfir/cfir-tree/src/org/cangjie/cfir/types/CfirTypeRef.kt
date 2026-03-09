package org.cangjie.cfir.types

import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirSourceElement
import org.cangnova.cangjie.name.Name
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 类型引用基接口。
 *
 * 区别于 ConeCangjieType（语义类型），CfirTypeRef 是语法层面的类型引用，
 * 在解析过程中会逐步被替换为 CfirResolvedTypeRef。
 */
sealed interface CfirTypeRef : CfirElement

/**
 * 已解析类型引用（携带解析后的 ConeCangjieType）。
 */
class CfirResolvedTypeRef(
    override val source: CfirSourceElement? = null,
    val coneType: ConeCangjieType,
) : CfirTypeRef {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitResolvedTypeRef(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirResolvedTypeRef = this

    override fun toString(): String = coneType.toString()
}

/**
 * 用户编写的未解析类型引用。
 *
 * 例如：`List<Int64>` 在解析前表示为 UserTypeRef。
 */
class CfirUserTypeRef(
    override val source: CfirSourceElement? = null,
    /** 类型名的限定路径（如 `a.b.MyClass` → [a, b, MyClass]） */
    val qualifier: List<Name>,
    /** 泛型实参引用列表 */
    val typeArguments: List<CfirTypeRef> = emptyList(),
) : CfirTypeRef {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitUserTypeRef(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirUserTypeRef = this

    override fun toString(): String = buildString {
        qualifier.joinTo(this, ".")
        if (typeArguments.isNotEmpty()) {
            typeArguments.joinTo(this, prefix = "<", postfix = ">")
        }
    }
}

/**
 * 基础类型引用。
 *
 * 与 CfirUserTypeRef 区分：该节点仅用于 RAW 阶段可由语法直接识别的内建基础类型。
 */
class CfirBasicTypeRef(
    override val source: CfirSourceElement? = null,
    val name: Name,
) : CfirTypeRef {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitBasicTypeRef(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirBasicTypeRef = this

    override fun toString(): String = name.asString()
}

/**
 * 隐式类型引用（待推断）。
 *
 * 当声明省略了显式类型时使用，由类型推断阶段替换为 CfirResolvedTypeRef。
 */
class CfirImplicitTypeRef(
    override val source: CfirSourceElement? = null,
) : CfirTypeRef {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitImplicitTypeRef(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirImplicitTypeRef = this

    override fun toString(): String = "<implicit>"

    companion object {
        val INSTANCE = CfirImplicitTypeRef()
    }
}

/**
 * 函数类型引用。
 *
 * 例如：`(Int64, String) -> Bool`
 */
class CfirFunctionTypeRef(
    override val source: CfirSourceElement? = null,
    val parameterTypeRefs: List<CfirTypeRef>,
    val returnTypeRef: CfirTypeRef,
) : CfirTypeRef {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitFunctionTypeRef(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirFunctionTypeRef = this

    override fun toString(): String = buildString {
        parameterTypeRefs.joinTo(this, prefix = "(", postfix = ")")
        append(" -> ")
        append(returnTypeRef)
    }
}

/**
 * 元组类型引用。
 *
 * 例如：`(Int64, String, Bool)`
 */
class CfirTupleTypeRef(
    override val source: CfirSourceElement? = null,
    val elementTypeRefs: List<CfirTypeRef>,
) : CfirTypeRef {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitTupleTypeRef(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTupleTypeRef = this

    override fun toString(): String =
        elementTypeRefs.joinToString(prefix = "(", postfix = ")")
}

/**
 * 定长数组类型引用。
 *
 * 例如：`VArray<String, $4>`
 */
class CfirVArrayTypeRef(
    override val source: CfirSourceElement? = null,
    val elementTypeRef: CfirTypeRef,
    val sizeLiteral: String,
) : CfirTypeRef {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitVArrayTypeRef(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirVArrayTypeRef = this

    override fun toString(): String = buildString {
        append("VArray<")
        append(elementTypeRef)
        append(", $")
        append(sizeLiteral)
        append(">")
    }
}

/**
 * 错误类型引用（解析失败时的占位符）。
 */
class CfirErrorTypeRef(
    override val source: CfirSourceElement? = null,
    val reason: String,
) : CfirTypeRef {
    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitErrorTypeRef(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirErrorTypeRef = this

    override fun toString(): String = "ERROR_TYPE($reason)"
}
