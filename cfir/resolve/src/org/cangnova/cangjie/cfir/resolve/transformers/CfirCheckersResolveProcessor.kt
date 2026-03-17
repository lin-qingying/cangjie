package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.collectors.components.CfirDiagnosticComponentsFactory
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.diagnostics.impl.PendingDiagnosticsReporterImpl
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * CHECKERS 闃舵澶勭悊鍣ㄣ€? *
 * 閬嶅巻宸插畬鎴?BODY_RESOLVE 鐨?CFIR 鏍戯紝閫氳繃瀹屾暣鐨? * Collector 鈫?Visitor 鈫?Components 涓夊眰绠＄嚎鎵ц绫诲瀷妫€鏌ュ櫒锛屾敹闆嗚瘖鏂俊鎭€? *
 * 瀵归綈 K2 `FirCheckersResolveProcessor`銆? */
internal class CfirCheckersResolveProcessor(
    private val diagnosticReporter: CfirDiagnosticReporter,
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirGlobalResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.CHECKERS,
) {
    private val collector = CfirDiagnosticComponentsFactory.create(session, CheckerDispatchKind.Common)

    override fun process(files: Collection<CfirFile>) {
        val pendingReporter = PendingDiagnosticsReporterImpl(diagnosticReporter)
        for (file in files) {
            collector.collectDiagnostics(file, pendingReporter)
        }
    }
}

