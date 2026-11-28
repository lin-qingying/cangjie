package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.providers.hasPackageOrSubpackages
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider

/** 包图冲突使用叶 session；仓颉按官方 sema_conflict_with_sub_package 锚定声明。 */
internal object CfirPackageConflictsWithClassifierChecker : CfirFileChecker(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        val packageName = declaration.packageDirective.packageFqName
        for (member in declaration.declarations) {
            val name = member.declarationName() ?: continue
            if (context.session.symbolProvider.hasPackageOrSubpackages(packageName.child(name))) {
                reporter.reportOn(member.source, CfirErrors.CONFLICT_WITH_SUB_PACKAGE, name, name)
            }
        }
    }
}
