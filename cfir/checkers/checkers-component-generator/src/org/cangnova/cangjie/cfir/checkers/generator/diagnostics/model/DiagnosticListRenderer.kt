package org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model

import java.io.File

/**
 * 诊断列表源码渲染器抽象基类。
 */
abstract class DiagnosticListRenderer {
    /**
     * 将诊断列表渲染到目标文件。
     */
    abstract fun render(
        file: File,
        diagnosticList: DiagnosticList,
        packageName: String,
        starImportsToAdd: Set<String>,
    )
}


