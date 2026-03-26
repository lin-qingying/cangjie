package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContextForProvider
import org.cangnova.cangjie.cfir.analysis.checkers.context.MutableCheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile

/**
 * Runs checker components on each element and commits produced diagnostics.
 */
open class CfirCheckerRunningDiagnosticCollectorVisitor(
    context: CheckerContextForProvider,

    protected val components: DiagnosticCollectorComponents,
) : AbstractDiagnosticCollectorVisitor(context) {

    override fun checkSettings() {
        components.regularComponents.forEach { it.checkSettings(context) }
    }

    override fun checkElement(element: CfirElement) {
        components.regularComponents.forEach { element.accept(it, context) }
        element.accept(components.reportCommitter, context)
    }

    override fun onDeclarationExit(declaration: CfirDeclaration) {
        if (declaration !is CfirFile) return
        components.reportCommitter.endOfFile(declaration, context)
    }
}
