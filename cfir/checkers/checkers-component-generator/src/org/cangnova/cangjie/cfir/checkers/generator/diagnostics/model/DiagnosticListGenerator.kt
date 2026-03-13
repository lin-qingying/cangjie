package org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model

import org.cangnova.cangjie.generators.util.getGenerationPath
import java.io.File

fun generateDiagnostics(rootPath: File, packageName: String, diagnosticList: DiagnosticList, starImportsToAdd: Set<String>) {
    val generationPath = getGenerationPath(rootPath, packageName)
    ErrorListDiagnosticListRenderer.render(
        generationPath.resolve("${diagnosticList.objectName}.kt"),
        diagnosticList,
        packageName,
        starImportsToAdd,
    )
}



