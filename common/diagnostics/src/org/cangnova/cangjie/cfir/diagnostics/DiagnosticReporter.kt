package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.source.AbstractCjSourceElement


/**
 * The diagnostic context is required for creating `CjDiagnostic` instances, and it is used for two purposes:
 * 1. To compute the proper diagnostic factory based on language features for [CjDiagnosticFactoryForDeprecation]
 * 2. To be stored inside the diagnostic instances and later accessed by the implementations of the
 *    [org.cangnova.cangjie.cfir.diagnostics.rendering.RenderingContext.Key]. To be precise, some fir-specific renderers
 *    downcast the [DiagnosticBaseContext] to context from the FIR to access the FIR session.
 */
interface DiagnosticBaseContext {
    /**
     * 当前诊断创建和渲染使用的语言版本设置。
     */
    val languageVersionSettings: LanguageVersionSettings
}

/**
 * 诊断上报时使用的完整上下文。
 */
interface DiagnosticContext : DiagnosticBaseContext {
    /**
     * 当前诊断创建和渲染使用的语言版本设置。
     */
    override val languageVersionSettings: LanguageVersionSettings
    /**
     * 当前诊断所属文件路径。
     */
    val containingFilePath: String?

    /**
     * 判断指定诊断是否被当前上下文 suppress。
     */
    fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean

    /**
     * 默认诊断上下文，不绑定文件且不 suppress 任何诊断。
     */
    object Default : DiagnosticContext {
        /**
         * 默认语言版本设置。
         */
        override val languageVersionSettings: LanguageVersionSettings get() = LanguageVersionSettings.DEFAULT
        /**
         * 默认上下文没有文件路径。
         */
        override val containingFilePath: String? get() = null
        /**
         * 默认上下文不屏蔽任何诊断。
         */
        override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean = false
    }
}

/**
 * 诊断上报器基类。
 */
abstract class DiagnosticReporter {
    /**
     * 上报一个诊断；diagnostic 为 null 时表示当前配置禁用了该诊断。
     */
    abstract fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext)

    /**
     * 当前 reporter 是否已接收错误级别诊断。
     */
    abstract val hasErrors: Boolean
    /**
     * 当前 reporter 是否已有会因 Werror 被提升为错误的警告。
     */
    abstract val hasWarningsForWError: Boolean
}

/**
 * No diagnostic reported with [DiagnosticReporter.report] wouldn't be commited to resulting diagnostic
 * storage until [checkAndCommitReportsOn] function for the corresponding source element would be called.
 *
 * This is required for proper work of diagnostic suppressions in the frontend environment:
 * in the frontend the checkers stage visits files and computes the suppression information for the subtree
 * upon visiting the annotated element. But in some cases the diagnostic on some element could be reported
 * before this element was visited (e.g. class checkers could report diagnostics on members).
 *
 * ```
 * class Some {
 *     @Suppress("ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS")
 *     abstract val x: Int // ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS is reported during visiting class `Some`, not property `x`
 * }
 * ```
 *
 * So to work around this situation the pending reporter just records that some diagnostics were reported during [report]
 * call. And when the visitor reaches the underlying elements, the [checkAndCommitReportsOn] is called with [context]
 * which is aware of the suppressions on the particular element.
 *
 * This whole situation is not the case for backend diagnostics reporting, as
 * [org.jetbrains.kotlin.ir.CjDiagnosticReporterWithImplicitIrBasedContext] creates the suppression mapping once for the whole
 * file, so it doesn't matter when the diagnostic is reported, the suppressions would be already computed.
 *
 * @param commitEverything whether to commit all pending reports regardless of the element
 */
abstract class PendingDiagnosticReporter : DiagnosticReporter() {
    /**
     * 检查指定元素上的挂起诊断，并根据 suppress 上下文提交可见诊断。
     */
    abstract fun checkAndCommitReportsOn(element: AbstractCjSourceElement, context: DiagnosticContext, commitEverything: Boolean)
}

