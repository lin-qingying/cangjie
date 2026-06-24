package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirStatement

/** CFIR 表达式 checker 基类，按具体 `CfirStatement` 子类型执行表达式诊断。 */
abstract class CfirExpressionChecker<E : CfirStatement> {
    /** 在当前 checker 上下文中检查一个表达式或语句节点。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun check(expression: E)
}

