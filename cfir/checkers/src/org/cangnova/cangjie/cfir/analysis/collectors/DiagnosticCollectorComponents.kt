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
    /** 诊断提交组件，在元素或文件遍历结束时提交 pending 诊断。 */
    val reportCommitter: ReportCommitterDiagnosticComponent,
)
