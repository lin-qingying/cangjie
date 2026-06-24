package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * 表达式是否已经拥有解析后的类型。
 */
val CfirExpression.hasResolvedType: Boolean get() = coneTypeOrNull != null


/**
 * 类型引用上的可空 cone type 视图。
 *
 * 未解析类型引用返回 `null`，已解析类型引用返回其 [CfirResolvedTypeRef.coneType]。
 */
val CfirTypeRef.coneTypeOrNull: ConeCangJieType?
    get() = coneTypeSafe()

/**
 * 类型引用上的非空 cone type 视图。
 *
 * 调用方应只在确认类型引用已经解析后使用该属性；否则会带 CFIR attachment 抛出内部错误。
 */
val CfirTypeRef.coneType: ConeCangJieType
    get() = coneTypeSafe()
        ?: errorWithAttachment("Expected ${CfirResolvedTypeRef::class.simpleName} with ${ConeCangJieType::class.simpleName} but was ${this::class.simpleName}") {
            withCfirEntry("typeRef", this@coneType)
        }

/**
 * 安全读取指定 cone type 子类型。
 *
 * 返回非空值时，Kotlin contract 会把接收者收窄为 [CfirResolvedTypeRef]。
 */
@OptIn(ExperimentalContracts::class)
inline fun <reified T : ConeCangJieType> CfirTypeRef.coneTypeSafe(): T? {
    contract {
        returnsNotNull() implies (this@coneTypeSafe is CfirResolvedTypeRef)
    }
    return (this as? CfirResolvedTypeRef)?.coneType as? T
}

/**
 * 表达式的非空解析类型。
 *
 * 未解析表达式会带 CFIR attachment 抛出内部错误，用于暴露错误的 phase 调用顺序。
 */
val CfirExpression.resolvedType: ConeCangJieType
    get() = coneTypeOrNull
        ?: errorWithAttachment("Expected expression '${this::class.simpleName}' to be resolved") {
            withCfirEntry("expression", this@resolvedType)
        }
