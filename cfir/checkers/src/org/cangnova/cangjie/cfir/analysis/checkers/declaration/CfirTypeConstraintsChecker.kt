package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.declarations.typeConstraintDiagnosticData
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn

object CfirTypeConstraintsChecker : CfirBasicDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val owner = declaration as? CfirTypeParameterRefsOwner ?: return
        val diagnosticData = declaration.attributes.typeConstraintDiagnosticData ?: return

        reportDanglingTypeConstraints(owner, diagnosticData)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportDanglingTypeConstraints(
        owner: CfirTypeParameterRefsOwner,
        diagnosticData: org.cangnova.cangjie.cfir.declarations.CfirTypeConstraintDiagnosticData,
    ) {
        val declaredTypeParameters = owner.typeParameters
            .map { it.symbol.name }
            .toSet()

        diagnosticData.typeConstraints.forEach { constraint ->
            if (constraint.parameterName in declaredTypeParameters) return@forEach

            reporter.reportOn(
                source = constraint.source,
                factory = CfirErrors.NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER,
                a = constraint.parameterName,
            )
        }
    }
}
