package org.cangnova.cangjie.cfir.diagnostics.impl

import org.cangnova.cangjie.cfir.diagnostics.*
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 按文件、源码元素和诊断工厂去重后再转发的 reporter。
 */
class DeduplicatingDiagnosticReporter(
    /**
     * 实际接收去重后诊断的 reporter。
     */
    private val delegate: DiagnosticReporter,
) : DiagnosticReporter() {
    /**
     * 是否已有错误，直接透传委托 reporter 状态。
     */
    override val hasErrors: Boolean get() = delegate.hasErrors
    /**
     * 是否已有 Werror 警告，直接透传委托 reporter 状态。
     */
    override val hasWarningsForWError: Boolean get() = delegate.hasWarningsForWError

    /**
     * 已转发诊断的去重键集合。
     */
    private val reported = mutableSetOf<Triple<String?, AbstractCjSourceElement, CjDiagnosticFactoryN>>()

    /**
     * 转发无源码诊断，并对有源码诊断执行去重。
     */
    override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
        when (diagnostic) {
            null -> {}
            is CjDiagnosticWithoutSource -> delegate.report(diagnostic, context)
            is CjDiagnosticWithSource -> {
                if (reported.add(Triple(context.containingFilePath, diagnostic.element, diagnostic.factory))) {
                    delegate.report(diagnostic, context)
                }
            }
        }
    }
}

/**
 * 将 reporter 包装为去重 reporter；若已经是去重 reporter 则直接返回自身。
 */
fun DiagnosticReporter.deduplicating(): DeduplicatingDiagnosticReporter {
    if (this is DeduplicatingDiagnosticReporter) return this
    return DeduplicatingDiagnosticReporter(this)
}

