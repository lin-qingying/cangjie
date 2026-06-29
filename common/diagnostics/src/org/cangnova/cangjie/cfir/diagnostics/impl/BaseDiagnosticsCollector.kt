package org.cangnova.cangjie.cfir.diagnostics.impl

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic

/**
 * [BaseDiagnosticsCollector] is a [DiagnosticReporter] which stores all reported diagnostics inside itself.
 */
abstract class BaseDiagnosticsCollector : DiagnosticReporter() {
    /**
     * 已收集的全部诊断。
     */
    abstract val diagnostics: List<CjDiagnostic>
    /**
     * 按文件路径分组的诊断集合。
     */
    abstract val diagnosticsByFilePath: Map<String?, List<CjDiagnostic>>

    /**
     * 忽略所有诊断的空 collector。
     */
    object DoNothing : BaseDiagnosticsCollector() {
        /**
         * 空 collector 不保存诊断。
         */
        override val diagnostics: List<CjDiagnostic>
            get() = emptyList()
        /**
         * 空 collector 没有文件分组诊断。
         */
        override val diagnosticsByFilePath: Map<String?, List<CjDiagnostic>>
            get() = emptyMap()

        /**
         * 丢弃传入诊断。
         */
        override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {}

        /**
         * 空 collector 永远没有错误。
         */
        override val hasErrors: Boolean
            get() = false
        /**
         * 空 collector 永远没有 Werror 警告。
         */
        override val hasWarningsForWError: Boolean
            get() = false
    }
}

