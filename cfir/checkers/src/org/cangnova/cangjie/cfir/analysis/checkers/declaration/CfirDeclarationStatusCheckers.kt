package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.name.Name

object CfirStaticModifierCompatibilityChecker : CfirMemberDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirMemberDeclaration) {
        val status = declaration.status
        if (!status.isStatic) return
        if (!status.isOpen && !status.isAbstract && !status.isOverride) return

        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.STATIC_CANNOT_BE_OPEN_ABSTRACT_OVERRIDE,
            a = declaration.declarationNameOrNull(),
        )
    }
}

object CfirMutModifierApplicabilityChecker : CfirMemberDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirMemberDeclaration) {
        if (!declaration.status.isMut) return
        if (declaration is CfirProperty) return
        if (declaration is CfirNamedFunction && context.isStructMemberFunction(declaration)) return

        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.MUT_ONLY_ON_FUNCTION,
            a = declaration.declarationNameOrNull(),
        )
    }
}

private fun CheckerContext.isStructMemberFunction(function: CfirNamedFunction): Boolean {
    val ownerClassId = function.symbol.callableId.classId
    if (ownerClassId != null) {
        val owner = session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir
        if (owner != null) {
            return owner is CfirStruct
        }
    }

    return findClosestDeclaration<CfirClassLikeDeclaration>() is CfirStruct
}

private fun CfirMemberDeclaration.declarationNameOrNull(): Name? = when (this) {
    is CfirNamedFunction -> name
    is CfirProperty -> name
    is CfirFieldVariable -> name
    is CfirClass -> name
    is CfirInterface -> name
    is CfirStruct -> name
    is CfirEnum -> name
    is CfirEnumConstructor -> name
    is CfirMacroDeclaration -> name
    is CfirTypeAlias -> name
    is CfirTypeParameter -> name
    is CfirValueParameter -> name
    else -> null
}
