package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirUnsafeExpression
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.platform.isCjNative

/**
 * cjnative 下的 native/CFunc 调用必须显式位于 `unsafe` 表达式内。
 *
 * 官方 `CFFICheck::CheckUnsafeInvoke` 在调用解析成功后消费最终函数类型：
 * foreign、`@C` 以及 CFunc 函数值都属于 native 调用；调用合法性不能从调用文本
 * 或函数短名推断。该 checker 只消费调用解析发布的 declaration/type identity，
 * 并保持 `unsafe` 作用域由 CFIR ancestor 栈表达。
 */
object CfirUnsafeFunctionInvokeChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        if (!context.session.moduleData.targetPlatform.isCjNative()) return
        if (!expression.requiresUnsafeNativeCall(context)) return
        if (context.containingElements.any { it is CfirUnsafeExpression } ||
            context.containingDeclarations.any { (it.cfir as? CfirFunction)?.status?.isUnsafe == true }
        ) return

        reporter.reportOn(
            source = expression.source ?: expression.calleeReference.source,
            factory = CfirErrors.UNSAFE_FUNCTION_INVOKE_FAILED,
        )
    }
}

/** 调用目标是否已经被解析为官方 native/CFunc 调用。 */
private fun CfirFunctionCall.requiresUnsafeNativeCall(context: CheckerContext): Boolean {
    if (isCFuncCall(context.session)) return true

    val receiverTypes = sequenceOf(explicitReceiver, dispatchReceiver)
        .mapNotNull { receiver ->
            receiver?.coneTypeOrNull?.fullyExpandedType(context.session) as? ConeFunctionType
        }
    return receiverTypes.any { it.isCFunc }
}
