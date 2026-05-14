package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirSuperReceiverExpression

/**
 * 非法 `super` 引用检查器。
 *
 * 统一处理 `extend`、`struct`、`enum`、`interface` 中不允许出现的 `super`。
 */
object CfirIllegalSuperReferenceChecker : CfirSuperReceiverExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirSuperReceiverExpression) {
        if (!CfirExtendSemantics.isSuperReference(expression.calleeReference)) return

        val source = expression.calleeReference.source ?: expression.source

        if (context.findClosestDeclaration<CfirExtend>() != null) {
            reporter.reportOn(source = source, factory = CfirErrors.EXTEND_SUPER_NOT_ALLOWED)
            return
        }

        if (context.findClosestDeclaration<CfirStruct>() != null) {
            reporter.reportOn(source = source, factory = CfirErrors.STRUCT_SUPER_NOT_ALLOWED)
            return
        }

        if (context.findClosestDeclaration<CfirEnum>() != null) {
            reporter.reportOn(source = source, factory = CfirErrors.ENUM_SUPER_NOT_ALLOWED)
            return
        }

        if (context.findClosestDeclaration<CfirInterface>() != null) {
            reporter.reportOn(source = source, factory = CfirErrors.INTERFACE_SUPER_NOT_ALLOWED)
            return
        }
    }
}
