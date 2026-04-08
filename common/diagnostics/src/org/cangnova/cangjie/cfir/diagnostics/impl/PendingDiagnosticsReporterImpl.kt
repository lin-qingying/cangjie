package org.cangnova.cangjie.cfir.diagnostics.impl

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithSource
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.source.AbstractCjSourceElement

class PendingDiagnosticsReporterImpl(private val delegate: DiagnosticReporter) : PendingDiagnosticReporter() {
    private val pendingDiagnosticsByFilePath: MutableMap<String, MutableList<CjDiagnostic>> = mutableMapOf()

    override val hasErrors: Boolean
        get() = delegate.hasErrors

    override val hasWarningsForWError: Boolean
        get() = delegate.hasWarningsForWError

    override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
        if (diagnostic == null) return
        when (val filePath = context.containingFilePath) {
            null -> delegate.report(diagnostic, context)
            else -> {
                if (context.isDiagnosticSuppressed(diagnostic)) return
                val pendingDiagnostics = pendingDiagnosticsByFilePath.getOrPut(filePath) { mutableListOf() }
                pendingDiagnostics.add(diagnostic)
            }
        }
    }

    override fun checkAndCommitReportsOn(
        element: AbstractCjSourceElement,
        context: DiagnosticContext,
        commitEverything: Boolean,
    ) {
        if (pendingDiagnosticsByFilePath.isEmpty()) return
        val pathFromContext = context.containingFilePath
        val pendingIterator = pendingDiagnosticsByFilePath.iterator()
        while (pendingIterator.hasNext()) {
            val (path, pendingList) = pendingIterator.next()
            assert(pathFromContext == null || path == pathFromContext) {
                "Pending diagnostics for file $path are commited on file $pathFromContext"
            }

            val iterator = pendingList.iterator()
            while (iterator.hasNext()) {
                val diagnostic = iterator.next()
                val diagnosticElement = (diagnostic as? CjDiagnosticWithSource)?.element
                when {
                    context.isDiagnosticSuppressed(diagnostic) -> {
                        if (diagnosticElement != null &&
                            (diagnosticElement == element ||
                                    diagnosticElement.startOffset >= element.startOffset && diagnosticElement.endOffset <= element.endOffset)
                        ) {
                            iterator.remove()
                        }
                    }
                    diagnosticElement == element || commitEverything -> {
                        iterator.remove()
                        delegate.report(diagnostic, context)
                    }
                }
            }
            if (pendingList.isEmpty()) {
                pendingIterator.remove()
            }
        }
    }

    /**
     * 安全网：提交所有剩余的 pending 诊断，无论元素匹配。
     * 正常流程中 [CfirReportCommitterDiagnosticComponent.endOfFile] 会处理，
     * 此方法作为最终兜底。
     */
    fun commitAll(context: DiagnosticContext) {
        val iterator = pendingDiagnosticsByFilePath.iterator()
        while (iterator.hasNext()) {
            val (_, pendingList) = iterator.next()
            for (diagnostic in pendingList) {
                if (!context.isDiagnosticSuppressed(diagnostic)) {
                    delegate.report(diagnostic, context)
                }
            }
            iterator.remove()
        }
    }
}
