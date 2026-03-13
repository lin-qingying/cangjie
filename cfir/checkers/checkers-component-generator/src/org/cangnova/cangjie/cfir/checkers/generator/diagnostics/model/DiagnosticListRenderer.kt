package org.cangjie.cfir.checkers.generator.diagnostics.model

import java.io.File

abstract class DiagnosticListRenderer {
    abstract fun render(
        file: File,
        diagnosticList: DiagnosticList,
        packageName: String,
        starImportsToAdd: Set<String>,
    )
}


