package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.isOption
import org.cangnova.cangjie.cfir.types.type

/**
 * 取元组字面量在目标类型下应使用的元组类型。
 *
 * 官方 `ChkTupleLit`（`Sema/TypeCheckExpr/TupleLit.cpp:23`）先做 `UnboxOptionType(target)`，
 * 目标仍是元组时才逐元素检查，因此这里对 `Option<T>` 目标继续取其实参。
 *
 * 该判定同时被 resolve（把元素期望类型下推到 `CfirTupleLiteral` 元素）与 checker
 * （按同一目标类型逐元素报告不匹配）消费，只在此处定义一次。
 */
fun ConeCangJieType.tupleLiteralTargetTypeOrNull(
    session: CfirSession,
): ConeTupleType? {
    val expanded = fullyExpandedType(session)
    if (expanded is ConeTupleType) return expanded
    if (expanded is ConeClassLikeType && expanded.isOption) {
        return expanded.typeArguments.singleOrNull()?.type as? ConeTupleType
    }
    return null
}
