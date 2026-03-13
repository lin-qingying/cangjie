package org.cangjie.analysis.api.cfir.resolve

import org.cangjie.analysis.api.CaModule
import org.cangjie.cfir.diagnostics.CfirDiagnosticCollector
import org.cangjie.cfir.session.CfirSession

/**
 * 最小可用的 CFIR 解析外观实现。
 *
 * 仅负责持有 session 与诊断收集器，供测试和最小解析入口使用。
 */
class CaCfirResolutionFacadeImpl(
    override val useSiteModule: CaModule,
    override val useSiteFirSession: CfirSession,
    val diagnostics: CfirDiagnosticCollector,
) : CaCfirResolutionFacade
