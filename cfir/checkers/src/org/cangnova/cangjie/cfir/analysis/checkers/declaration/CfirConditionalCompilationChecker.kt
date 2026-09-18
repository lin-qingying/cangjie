package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationCatalog
import org.cangnova.cangjie.cfir.session.CfirConditionalCompilationError

/**
 * 消费 raw conditional owner 发布的失败事实，并映射为官方条件编译诊断。
 *
 * raw pipeline 不依赖 checker 模块，也不抛异常；失败先挂在 CfirFile 上，
 * 由这里统一负责源码位置、诊断名称和参数。
 */
object CfirConditionalCompilationChecker : CfirFileChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        for (failure in declaration.conditionalCompilationFailures) {
            val source = failure.source ?: declaration.source ?: continue
            when (failure.reason) {
                CfirConditionalCompilationError.NO_CONDITION ->
                    reporter.reportOn(source, CfirErrors.CONDITIONAL_COMPILATION_NOT_HAVE_CONDITION_EXPR)

                CfirConditionalCompilationError.INVALID_EXPRESSION ->
                    reporter.reportOn(source, CfirErrors.CONDITIONAL_COMPILATION_INVALID_CONDITION_EXPR)

                CfirConditionalCompilationError.UNKNOWN_CONDITION ->
                    reporter.reportOn(
                        source,
                        CfirErrors.CONDITIONAL_COMPILATION_NOT_SUPPORT_THIS_CONDITION,
                        failure.conditionName.orEmpty(),
                    )

                CfirConditionalCompilationError.INVALID_VALUE ->
                    reporter.reportOn(source, CfirErrors.CONDITIONAL_COMPILATION_INVALID_CONDITION_VALUE)

                CfirConditionalCompilationError.UNSUPPORTED_VALUE -> {
                    val name = failure.conditionName.orEmpty()
                    val supported = CfirConditionalCompilationCatalog.builtin(name)
                        ?.supportedValues
                        ?.joinToString(" ")
                        .orEmpty()
                    reporter.reportOn(
                        source,
                        CfirErrors.CONDITIONAL_COMPILATION_NOT_SUPPORT_BUILTIN_VALUE,
                        name,
                        failure.rightValue.orEmpty(),
                        supported,
                    )
                }

                CfirConditionalCompilationError.UNSUPPORTED_OPERATOR ->
                    reporter.reportOn(
                        source,
                        CfirErrors.CONDITIONAL_COMPILATION_NOT_SUPPORT_OP,
                        failure.conditionName.orEmpty(),
                        failure.operator?.sourceText.orEmpty(),
                    )

                CfirConditionalCompilationError.INVALID_VERSION ->
                    reporter.reportOn(
                        source,
                        CfirErrors.CONDITIONAL_COMPILATION_NOT_SUPPORT_CJC_VERSION_FORMAT,
                    )
            }
        }
    }
}

/** 条件编译比较运算符的官方源码拼写，用于诊断消息中回显原始运算符。 */
private val org.cangnova.cangjie.cfir.session.CfirConditionalCompilationOperator.sourceText: String
    get() = when (this) {
        org.cangnova.cangjie.cfir.session.CfirConditionalCompilationOperator.EQ -> "=="
        org.cangnova.cangjie.cfir.session.CfirConditionalCompilationOperator.NE -> "!="
        org.cangnova.cangjie.cfir.session.CfirConditionalCompilationOperator.LT -> "<"
        org.cangnova.cangjie.cfir.session.CfirConditionalCompilationOperator.GT -> ">"
        org.cangnova.cangjie.cfir.session.CfirConditionalCompilationOperator.LE -> "<="
        org.cangnova.cangjie.cfir.session.CfirConditionalCompilationOperator.GE -> ">="
    }
