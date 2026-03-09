package org.cangjie.cfir.expressions

import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.types.ConeCangjieType
import org.cangjie.cfir.visitors.CfirTransformer
import org.cangjie.cfir.visitors.CfirVisitor

/**
 * 类型操作种类。
 */
enum class CfirTypeOperationKind {
    /** is 类型检查 */
    IS,
    /** as 类型转换 */
    AS,
}

/**
 * 类型操作表达式（is / as），对应仓颉编译器中的类型检查和转换。
 */
class CfirTypeOperator(
    override val source: CfirSourceElement? = null,
    val operation: CfirTypeOperationKind,
    var argument: CfirExpression,
    val typeRef: CfirTypeRef,
) : CfirExpression() {
    override var coneTypeOrNull: ConeCangjieType? = null

    override fun <R, D> accept(visitor: CfirVisitor<R, D>, data: D): R =
        visitor.visitTypeOperator(this, data)

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirTypeOperator {
        argument = argument.accept(transformer, data) as CfirExpression
        return this
    }
}
