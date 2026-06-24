package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 检查值参数默认值是否满足参数声明类型。
 *
 * Kotlin FIR 对应实现是 `FirValueParameterDefaultValueTypeMismatchChecker`：
 * 默认值表达式按参数类型作为期望类型检查，诊断落在默认值表达式上。
 */
object CfirValueParameterDefaultValueTypeMismatchChecker : CfirValueParameterChecker() {
    /**
     * 检查值参数默认值表达式是否兼容参数声明类型。
     *
     * 默认值为错误表达式时跳过，避免重复上报解析阶段已经产生的诊断。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirValueParameter) {
        val expectedType = (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        val defaultValue = declaration.defaultValue?.takeIf { it !is CfirErrorExpression } ?: return
        val actualType = defaultValue.coneTypeOrNull ?: return
        val source = declaration.source as? AbstractCjSourceElement ?: return

        checkTypeMismatch(
            expectedType = expectedType,
            actualType = actualType,
            source = source,
            preferredSpecializedSource = defaultValue.source as? AbstractCjSourceElement,
            diagnosticFactory = CfirErrors.TYPE_MISMATCH,
        )
    }
}
