package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.types.ConeVArrayType

/**
 * VArray 构造器参数个数检查。
 *
 * 对齐 C++ DiagKind::sema_varray_args_number_mismatch:
 * `VArray<T, N>(...)` 构造器只接受一个参数（初始化 lambda 或 repeat/item）。
 */
object CfirVArrayConstructorArgChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val resultType = expression.coneTypeOrNull ?: return
        if (resultType !is ConeVArrayType) return
        val argCount = expression.argumentList.arguments.size
        if (argCount == 1) return
        reporter.reportOn(
            source = expression.source,
            factory = CfirErrors.VARRAY_ARGS_NUMBER_MISMATCH,
        )
    }
}
