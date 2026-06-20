package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef

/**
 * 检查 type alias 展开链中的环。
 *
 * 对齐官方 Cangjie `TypeAliasCircleCheck`：在别名替换前完成环检查，
 * 对环上的每个 type alias 声明节点报告 `sema_typealias_cycle`。
 */
object CfirTypeAliasCycleChecker : CfirTypeAliasChecker() {
    private const val RECURSIVE_TYPEALIAS_PREFIX = "Recursive typealias expansion"

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirTypeAlias) {
        val errorTypeRef = declaration.expandedTypeRef as? CfirErrorTypeRef ?: return
        val diagnostic = errorTypeRef.diagnostic as? ConeSimpleDiagnostic ?: return
        if (!diagnostic.reason.startsWith(RECURSIVE_TYPEALIAS_PREFIX)) return

        reporter.reportOn(
            source = declaration.source?.firstCharacterDiagnosticSource(),
            factory = CfirErrors.TYPEALIAS_CYCLE,
            a = declaration.name.asString(),
        )
    }
}
