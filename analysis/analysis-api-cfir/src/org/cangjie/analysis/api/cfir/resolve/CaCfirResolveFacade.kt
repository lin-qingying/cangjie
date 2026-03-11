package org.cangjie.analysis.api.cfir.resolve

import org.cangjie.cfir.declarations.CfirFile
import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.diagnostics.CfirDiagnosticCollector
import org.cangjie.cfir.resolve.CfirTotalResolveProcessor
import org.cangjie.cfir.session.diagnosticCollector
import org.cangjie.cfir.session.phaseResolverRegistry

/**
 * 最小 CFIR_RESOLVE 调用入口，负责把 Raw CFIR 推进到指定阶段。
 */
class CaCfirResolveFacade(
    private val resolutionFacade: CaCfirResolutionFacade,
) {
    fun resolveTo(file: CfirFile, targetPhase: CfirResolvePhase = CfirResolvePhase.CHECKERS): CfirDiagnosticCollector {
        val session = resolutionFacade.useSiteFirSession
        val processor = CfirTotalResolveProcessor(session, session.phaseResolverRegistry)
        processor.processToPhase(file, targetPhase)
        return session.diagnosticCollector
    }
}
