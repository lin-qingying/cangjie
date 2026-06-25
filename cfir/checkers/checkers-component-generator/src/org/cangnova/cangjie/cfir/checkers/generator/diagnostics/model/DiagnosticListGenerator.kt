package org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model

import org.cangnova.cangjie.generators.util.getGenerationPath
import java.io.File

/**
 * 根据诊断 DSL 列表生成诊断工厂源码文件。
 */
fun generateDiagnostics(rootPath: File, packageName: String, diagnosticList: DiagnosticList, starImportsToAdd: Set<String>) {
    val generationPath = getGenerationPath(rootPath, packageName)
    ErrorListDiagnosticListRenderer.render(
        generationPath.resolve("${diagnosticList.objectName}.kt"),
        diagnosticList,
        packageName,
        starImportsToAdd,
    )
}



