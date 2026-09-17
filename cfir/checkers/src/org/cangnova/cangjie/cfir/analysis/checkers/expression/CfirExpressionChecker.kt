package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirStatement

/** CFIR 表达式 checker 基类，按具体 `CfirStatement` 子类型执行表达式诊断。 */
abstract class CfirExpressionChecker<E : CfirStatement> {
    /**
     * 表达式检查是否依赖可执行实现（函数体、lambda 或局部 CFG）。
     *
     * 统一分派器使用该能力标记处理没有实现体的声明输入；默认值保持现有
     * checker 行为，只有明确依赖实现的 checker 才需要覆写。
     */
    open val requiresImplementation: Boolean get() = false

    /** 在当前 checker 上下文中检查一个表达式或语句节点。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun check(expression: E)
}
