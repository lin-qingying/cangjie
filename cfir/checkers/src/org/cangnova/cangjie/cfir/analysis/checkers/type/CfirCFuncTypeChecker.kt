package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirCFuncTypeLegalityReporter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef

/** 所有类型使用位置共享 CFunc 签名规则，typealias 和普通变量不再各自重算。 */
object CfirCFuncTypeChecker : CfirResolvedTypeRefChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: CfirResolvedTypeRef) {
        CfirCFuncTypeLegalityReporter.reportNestedDiagnosticsIfCFunc(typeRef)
    }
}
