package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.fullyExpandedTypeUsingAbbreviation
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/**
 * 返回 extend 声明在语义分析阶段使用的真实目标类型。
 *
 * extend 的索引、声明体接收者、候选适用性和 checker 必须共享同一类型身份：
 * 先恢复错误类型中仍可识别的声明类型，再以 typealias 声明 RHS 为模板代入真实实参并完全展开。
 * abbreviation 只保留源码层别名信息，不能形成独立的 extend 目标域。
 */
fun CfirExtend.semanticExtendedType(useSiteSession: CfirSession): ConeCangJieType? =
    extendedTypeRef.semanticExtendType(useSiteSession)

/**
 * 返回类型引用参与 extend 语义时的真实类型视图。
 */
fun CfirTypeRef.semanticExtendType(useSiteSession: CfirSession): ConeCangJieType? =
    coneTypeOrNull?.semanticExtendType(useSiteSession)

/**
 * 统一 extend 语义中的错误类型恢复与 declaration-backed typealias 展开。
 */
fun ConeCangJieType.semanticExtendType(useSiteSession: CfirSession): ConeCangJieType {
    val recoverableType = (this as? ConeErrorType)?.delegatedType ?: this
    return recoverableType.fullyExpandedTypeUsingAbbreviation(useSiteSession)
}
