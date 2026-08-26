package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.cangnova.cangjie.cfir.analysis.collectors.components.ReportCommitterDiagnosticComponent

/**
 * 诊断收集组件容器。
 * 对齐 K2 `DiagnosticCollectorComponents`。
 * @param regularComponents 常规检查组件，如声明检查器、表达式检查器等
 * @param reportCommitter 诊断提交组件，在每个元素检查完成后提交 pending 诊断
 */
class  DiagnosticCollectorComponents(
    /** 常规检查组件，如声明检查器、表达式检查器、类型检查器等。 */
    val regularComponents: Array<AbstractDiagnosticCollectorComponent>,
    /**
     * 只在当前文件 Sema 阶段无错误时运行的后续组件。
     *
     * 官方编译器不会在 Sema 已失败的源文件上继续执行 CHIR 常量检查；把这类组件与
     * 常规诊断遍历分开，避免遍历顺序导致前面表达式的 CHIR 诊断抢先泄露。
     */
    val postSemaComponents: Array<AbstractDiagnosticCollectorComponent>,
    /** 诊断提交组件，在元素或文件遍历结束时提交 pending 诊断。 */
    val reportCommitter: ReportCommitterDiagnosticComponent,
) {
    /** 创建只执行后续阶段检查器的一次诊断遍历配置。 */
    fun postSemaPass(): DiagnosticCollectorComponents = DiagnosticCollectorComponents(
        regularComponents = postSemaComponents,
        postSemaComponents = emptyArray(),
        reportCommitter = reportCommitter,
    )
}
