package org.cangjie.analysis.api.cfir.resolve

import org.cangjie.analysis.api.CaModule
import org.cangjie.cfir.session.CfirSession

/**
 * CFIR 解析外观（对齐 Kotlin 的 LLResolutionFacade / LLFirResolutionFacade）。
 *
 * Analysis API 与 CFIR 编译器之间的桥梁。
 * 封装底层 CfirSession 和解析能力，避免 Analysis API 直接依赖编译器内部。
 */
interface CaCfirResolutionFacade {
    /** 用途模块 */
    val useSiteModule: CaModule

    /** 底层 CFIR session */
    val useSiteFirSession: CfirSession
}
