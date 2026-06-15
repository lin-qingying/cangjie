package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.checkers.CfirVArraySizeLiteralUtils
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirVArrayTypeRef

/**
 * VArray 长度字面量范围检查。
 *
 * 对齐官方 `PreCheck.cpp#CheckVArrayParam`：`VArray<T, $N>` 的长度字面量
 * 在类型语义层按 Int64 初始化，超出 `[0, Int64.MAX_VALUE]` 的值报告数字
 * 范围错误，随后该 VArray 类型进入错误类型路径。
 */
object CfirVArraySizeLiteralChecker : CfirTypeRefChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: CfirTypeRef) {
        val varrayTypeRef = typeRef as? CfirVArrayTypeRef ?: return
        val parsed = CfirVArraySizeLiteralUtils.overflowingSizeLiteral(varrayTypeRef.sizeLiteral) ?: return

        reporter.reportOn(
            source = CfirVArraySizeLiteralUtils.sizeLiteralDiagnosticSource(
                source = varrayTypeRef.source,
                sizeLiteral = varrayTypeRef.sizeLiteral,
            ),
            factory = CfirErrors.LITERAL_NUMERIC_OVERFLOW,
            a = parsed.originalText,
            b = CfirVArraySizeLiteralUtils.targetType,
        )
    }
}
