package org.cangnova.cangjie.cfir.resolve.framework

import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnostic

data class ResolveDiagnosticsArtifact(
    val fileName: String,
    val declarationPhase: String,
    val diagnostics: List<CfirDiagnostic>,
)

