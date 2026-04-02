package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.checkers.SourceModifier
import org.cangnova.cangjie.cfir.analysis.checkers.checkModifiersCompatibility
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.enclosingTypeProjectionSource
import org.cangnova.cangjie.cfir.analysis.checkers.realSourceModifiers
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.types.CfirTypeRef

object CfirTypeProjectionModifierChecker : CfirTypeRefChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: CfirTypeRef) {
        val projectionSource = typeRef.source?.enclosingTypeProjectionSource() ?: return
        val modifiers = projectionSource.realSourceModifiers() ?: return
        val reportedNodes = hashSetOf<SourceModifier>()
        checkModifiersCompatibility(typeRef, modifiers, reportedNodes)
    }
}
