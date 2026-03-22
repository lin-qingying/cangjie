package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.collectors.components.CfirDiagnosticComponentsFactory
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.diagnostics.impl.PendingDiagnosticsReporterImpl
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * CHECKERS 阶段处理器。
 * 它遍历已经完成 `BODY_RESOLVE` 的 CFIR 树，
 * 通过 Collector -> Visitor -> Components 三层管线执行检查器并收集诊断。
 * 对齐 K2 `FirCheckersResolveProcessor`。
 */
internal class CfirCheckersResolveProcessor(
    private val diagnosticReporter: CfirDiagnosticReporter,
    session: CfirSession,
    scopeSession: ScopeSession,
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

