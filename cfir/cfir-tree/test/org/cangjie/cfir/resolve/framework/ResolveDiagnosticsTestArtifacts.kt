package org.cangjie.cfir.resolve.framework

import org.cangjie.cfir.diagnostics.CfirDiagnostic

data class ResolveDiagnosticsArtifact(
    val fileName: String,
    val declarationPhase: String,
    val diagnostics: List<CfirDiagnostic>,
)

