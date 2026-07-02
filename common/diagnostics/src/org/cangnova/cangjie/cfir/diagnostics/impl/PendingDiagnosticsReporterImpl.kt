package org.cangnova.cangjie.cfir.diagnostics.impl

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithSource
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters1
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters2
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters3
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters4
import org.cangnova.cangjie.cfir.diagnostics.CjOffsetsOnlyDiagnosticWithParameters1
import org.cangnova.cangjie.cfir.diagnostics.CjOffsetsOnlyDiagnosticWithParameters2
import org.cangnova.cangjie.cfir.diagnostics.CjOffsetsOnlyDiagnosticWithParameters3
import org.cangnova.cangjie.cfir.diagnostics.CjOffsetsOnlyDiagnosticWithParameters4
import org.cangnova.cangjie.cfir.diagnostics.CjOffsetsOnlySimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.CjSimpleDiagnostic
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 支持延迟提交和 suppress 二次判断的诊断 reporter 实现。
 */
class PendingDiagnosticsReporterImpl(
    /**
     * 最终接收已提交诊断的 reporter。
     */
    private val delegate: DiagnosticReporter,
    /**
     * 可选的源码元素映射函数，用于把诊断重定向到 offset-only 元素。
     */
    private val sourceMapper: (AbstractCjSourceElement) -> AbstractCjSourceElement? = { null },
) : PendingDiagnosticReporter() {
    private val pendingDiagnosticsByFilePath: MutableMap<String, MutableList<CjDiagnostic>> = mutableMapOf()
    private val committedDiagnosticsByFilePath: MutableMap<String, MutableList<CjDiagnostic>> = mutableMapOf()

    override val hasErrors: Boolean
        get() = delegate.hasErrors

    override val hasWarningsForWError: Boolean
        get() = delegate.hasWarningsForWError

    override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
        if (diagnostic == null) return
        val remappedDiagnostic = diagnostic.remapSourceIfNeeded()
        when (val filePath = context.containingFilePath) {
            null -> delegate.report(remappedDiagnostic, context)
            else -> {
                if (context.isDiagnosticSuppressed(remappedDiagnostic)) return
                val pendingDiagnostics = pendingDiagnosticsByFilePath.getOrPut(filePath) { mutableListOf() }
                pendingDiagnostics.add(remappedDiagnostic)
            }
        }
    }

    private fun CjDiagnostic.remapSourceIfNeeded(): CjDiagnostic {
        val currentElement = (this as? CjDiagnosticWithSource)?.element ?: return this
        val mappedElement = sourceMapper(currentElement) ?: return this
        if (mappedElement == currentElement) return this
        return when (this) {
            is CjSimpleDiagnostic -> CjOffsetsOnlySimpleDiagnostic(
                element = mappedElement,
                severity = severity,
                factory = factory,
                positioningStrategy = positioningStrategy,
                context = context,
            )
            is CjDiagnosticWithParameters1<*> -> CjOffsetsOnlyDiagnosticWithParameters1(
                element = mappedElement,
                a = a,
                severity = severity,
                factory = factory,
                positioningStrategy = positioningStrategy,
                context = context,
            )
            is CjDiagnosticWithParameters2<*, *> -> CjOffsetsOnlyDiagnosticWithParameters2(
                element = mappedElement,
                a = a,
                b = b,
                severity = severity,
                factory = factory,
                positioningStrategy = positioningStrategy,
                context = context,
            )
            is CjDiagnosticWithParameters3<*, *, *> -> CjOffsetsOnlyDiagnosticWithParameters3(
                element = mappedElement,
                a = a,
                b = b,
                c = c,
                severity = severity,
                factory = factory,
                positioningStrategy = positioningStrategy,
                context = context,
            )
            is CjDiagnosticWithParameters4<*, *, *, *> -> CjOffsetsOnlyDiagnosticWithParameters4(
                element = mappedElement,
                a = a,
                b = b,
                c = c,
                d = d,
                severity = severity,
                factory = factory,
                positioningStrategy = positioningStrategy,
                context = context,
            )
            else -> this
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
                    diagnostic.isCfirDceWarning() && !commitEverything -> {
                        // CHIR DCE 类 warning 在官方编译器中位于 sema 成功之后。
                        // 因此这里延迟到文件结束，再根据同文件是否存在 error 统一决定提交或丢弃。
                    }
                    diagnosticElement == element || commitEverything -> {
                        iterator.remove()
                        if (diagnostic.isCfirDceWarning() && hasErrorInFile(path, pendingList)) {
                            continue
                        }
                        if (diagnostic.isGenericTypeMismatchSupersededByReturnMismatch(
                                pendingDiagnostics = pendingList,
                                committedDiagnostics = committedDiagnosticsByFilePath[path],
                            )
                        ) {
                            continue
                        }
                        commitDiagnostic(path, diagnostic, context)
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
                    commitDiagnostic(
                        filePath = (diagnostic.context as? DiagnosticContext)?.containingFilePath,
                        diagnostic = diagnostic,
                        context = context,
                    )
                }
            }
            iterator.remove()
        }
    }

    private fun commitDiagnostic(
        filePath: String?,
        diagnostic: CjDiagnostic,
        context: DiagnosticContext,
    ) {
        val committedDiagnostics = filePath?.let {
            committedDiagnosticsByFilePath.getOrPut(it) { mutableListOf() }
        }
        if (committedDiagnostics != null) {
            if (committedDiagnostics.any { it.hasSameDiagnosticIdentity(diagnostic) }) return
        }

        delegate.report(diagnostic, context)
        committedDiagnostics?.add(diagnostic)
    }

    private fun hasErrorInFile(filePath: String, pendingList: List<CjDiagnostic>): Boolean =
        committedDiagnosticsByFilePath[filePath]?.any { it.severity.isError } == true ||
            pendingList.any { it.severity.isError }
}

/**
 * 判断两个诊断是否在名称、消息和首范围上等价。
 */
private fun CjDiagnostic.hasSameDiagnosticIdentity(other: CjDiagnostic): Boolean =
    factoryName == other.factoryName &&
        renderMessage() == other.renderMessage() &&
        firstRange.startOffset == other.firstRange.startOffset &&
        firstRange.endOffset == other.firstRange.endOffset

/**
 * 判断诊断是否是 CFIR DCE 产生的 unused warning。
 */
private fun CjDiagnostic.isCfirDceWarning(): Boolean =
    factoryName == "CFIR_UNUSED_VARIABLE" || factoryName == "CFIR_UNUSED_EXPRESSION"

/**
 * RETURN_TYPE_MISMATCH 是 return 根表达式上的专用分类；同一起点且覆盖通用
 * TYPE_MISMATCH 范围时，专用诊断拥有该位置，避免重复输出通用类型不匹配。
 */
private fun CjDiagnostic.isGenericTypeMismatchSupersededByReturnMismatch(
    pendingDiagnostics: List<CjDiagnostic>,
    committedDiagnostics: List<CjDiagnostic>?,
): Boolean {
    if (factoryName != "CFIR_TYPE_MISMATCH") return false
    return (pendingDiagnostics.asSequence() + committedDiagnostics.orEmpty().asSequence())
        .any { candidate ->
            candidate.factoryName == "CFIR_RETURN_TYPE_MISMATCH" &&
                candidate.firstRange.startOffset == firstRange.startOffset &&
                candidate.firstRange.endOffset >= firstRange.endOffset
        }
}
