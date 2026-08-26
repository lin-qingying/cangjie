package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirConstEvalArithmeticChecker
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * CHIR 常量算术诊断的收集组件。
 *
 * 官方编译器只在 Sema 阶段没有错误时才执行 CHIR 常量传播并报告算术溢出、除零和非法移位；
 * 此组件由 [org.cangnova.cangjie.cfir.analysis.collectors.AbstractDiagnosticCollector]
 * 在通过同一文件的 Sema gate 后单独遍历。
 */
class CfirChirArithmeticDiagnosticCollectorComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    override fun visitFunctionCall(functionCall: CfirFunctionCall, data: CheckerContext) {
        with(data) {
            with(reporter) {
                CfirConstEvalArithmeticChecker.check(functionCall)
            }
        }
    }
}
