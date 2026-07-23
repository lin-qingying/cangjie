package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic

/**
 * 声明父类型错误造成的成员查找不完整性来源。
 *
 * [ownerSymbol] 是包含无效父边的声明，[rootDiagnostic] 是该父边已经拥有的主诊断。
 * 该信息只描述 lookup completeness，不授予错误父边成员可见性。
 */
data class CfirMemberLookupBlocker(
    val ownerSymbol: CfirClassLikeSymbol<*>,
    val rootDiagnostic: ConeDiagnostic,
)

/**
 * 能够向调用解析暴露成员查找完整性的 scope。
 *
 * resolver 只在最终候选为空时消费 blocker；成功候选以及普通成员集合不受影响。
 */
interface CfirMemberLookupCompletenessScope {
    val memberLookupBlockers: List<CfirMemberLookupBlocker>
}
