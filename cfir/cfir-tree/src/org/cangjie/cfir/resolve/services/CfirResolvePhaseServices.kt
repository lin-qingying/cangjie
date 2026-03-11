package org.cangjie.cfir.resolve.services

import org.cangjie.cfir.diagnostics.CfirDiagnosticReporter
import org.cangjie.cfir.session.CfirSession

/**
 * Shared service bundle for resolve phases.
 *
 * This is intentionally small now and will grow as each phase gets formal implementations.
 */
data class CfirResolvePhaseServices(
    val session: CfirSession,
    val diagnostics: CfirDiagnosticReporter,
)
