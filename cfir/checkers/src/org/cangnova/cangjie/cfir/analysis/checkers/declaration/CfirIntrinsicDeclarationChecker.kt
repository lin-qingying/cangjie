package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.cfir.containingClassForStaticMemberAttr
import org.cangnova.cangjie.cfir.containingExtend
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn

/**
 * `@Intrinsic` 的函数声明约束。
 *
 * 官方 parser 在函数声明解析完成后检查两个事实：intrinsic 函数必须位于顶层，
 * 且不能拥有函数体。class 等非函数声明不会进入这两个专用检查；target checker
 * 因此不应代替本 owner 产生通用的注解目标诊断。
 */
object CfirIntrinsicDeclarationChecker : CfirFunctionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFunction) {
        if (!declaration.hasBuiltinAnnotation(BuiltInAnnotationKind.INTRINSIC)) return
        val function = declaration as? CfirNamedFunction ?: return

        val isTopLevel = !function.isLocal &&
            function.dispatchReceiverType == null &&
            function.containingClassForStaticMemberAttr == null &&
            function.containingExtend == null
        if (!isTopLevel) {
            reporter.reportOn(
                source = function.source,
                factory = CfirErrors.INTRINSIC_FUNCTION_MUST_BE_TOPLEVEL,
            )
        }

        function.body?.let { body ->
            reporter.reportOn(
                source = body.source ?: function.source,
                factory = CfirErrors.INTRINSIC_FUNCTION_CANNOT_HAVE_BODY,
            )
        }
    }
}
