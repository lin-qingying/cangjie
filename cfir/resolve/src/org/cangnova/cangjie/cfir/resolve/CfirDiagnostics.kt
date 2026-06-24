package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

/** CFIR resolve 使用的诊断报告器类型。 */
typealias CfirDiagnosticReporter = DiagnosticReporter

/**
 * 诊断上报器的会话组件包装。
 * 对齐 Kotlin 的做法：诊断能力由 session 持有，而不是在 resolve 组件注册时单独传参。
 */
class CfirDiagnosticReporterComponent(
    /** 当前 session 使用的诊断报告器。 */
    val reporter: CfirDiagnosticReporter,
) : CfirSessionComponent

/** 已解析并可展示的 CFIR 诊断摘要。 */
data class CfirResolvedDiagnostic(
    /** 诊断 factory 名称。 */
    val factoryName: String,
    /** 渲染后的诊断消息。 */
    val message: String,
    /** 诊断严重级别。 */
    val severity: Severity,
)

/** 收集诊断到内存列表的诊断报告器实现。 */
class CfirDiagnosticCollector : DiagnosticReporter() {
    /** 原始诊断存储。 */
    private val storage = mutableListOf<CjDiagnostic>()

    /** 未加工的原始诊断列表。 */
    val rawDiagnostics: List<CjDiagnostic>
        get() = storage

    /** 渲染后的诊断摘要列表。 */
    val diagnostics: List<CfirResolvedDiagnostic>
        get() = storage.map {
            CfirResolvedDiagnostic(
                factoryName = it.factoryName,
                message = it.renderMessage(),
                severity = it.severity,
            )
        }

    override val hasErrors: Boolean
        get() = storage.any { it.severity.isError }

    override val hasWarningsForWError: Boolean
        get() = storage.any { it.severity.isErrorWhenWError }

    /** 收集未被抑制的诊断。 */
    override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
        if (diagnostic != null && !context.isDiagnosticSuppressed(diagnostic)) {
            storage += diagnostic
        }
    }
}
