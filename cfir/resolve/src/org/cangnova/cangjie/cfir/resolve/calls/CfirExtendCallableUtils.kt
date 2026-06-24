package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides

/**
 * 仓颉 extend 成员判定的调用解析公共入口。
 *
 * Kotlin FIR 在 callable 上直接保存 extension receiver parameter；仓颉 CFIR
 * 通过 owner extend 索引表达同一语义，因此 tower 和 candidate 层必须共用
 * 这条 provider-backed 判定，避免普通顶层 callable 被误当作点调用扩展成员。
 */
internal fun CfirCallableSymbol<*>.containingAccessibleExtendOrNull(session: CfirSession): CfirExtend? {
    val extendProvider = session.extendProviderOrNull ?: return null
    return extendProvider.getContainingExtend(unwrapSubstitutionOverrides())
        ?.takeIf(extendProvider::isExtendAccessible)
}

/** 判断 callable symbol 是否是可作为实例 extend 成员参与点调用解析的候选。 */
internal fun CfirCallableSymbol<*>.isInstanceExtendMemberCandidate(session: CfirSession): Boolean {
    if (cfir.status.isStatic) return false
    return containingAccessibleExtendOrNull(session) != null
}
