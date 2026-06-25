package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 调用解析使用的 SAM 转换元数据持有者。
 *
 * 当前 CFIR 流水线只在 candidate/completion 路径需要转换 payload 形状；
 * 声明放在这里是为了让依赖边界对齐 Kotlin body-resolve 架构，而不是隐藏在 Candidate 的局部 stub 中。
 */
class CfirSamResolver(
    /** 当前 CFIR session。 */
    override val session: CfirSession,
    /** 当前 scope session。 */
    override val scopeSession: ScopeSession,
) : SessionAndScopeSessionHolder {
    /** 单次 SAM 转换的源函数类型与目标 SAM 类型。 */
    data class SamConversionInfo(
        /** 可被转换的函数类型。 */
        val functionalType: ConeCangJieType,
        /** 转换后的 SAM 目标类型。 */
        val samType: ConeCangJieType,
    )
}
