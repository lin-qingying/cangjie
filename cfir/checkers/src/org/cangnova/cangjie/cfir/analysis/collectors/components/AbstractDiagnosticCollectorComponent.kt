package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.AbstractSourceElementPositioningStrategy
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory0
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory1
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

abstract class AbstractDiagnosticCollectorComponent(
    protected val session: CfirSession,
    protected val reporter: PendingDiagnosticReporter,
) : CfirVisitor<Unit, CheckerContext>() {
    override fun visitElement(element: CfirElement, data: CheckerContext) {}

    open fun checkSettings(data: CheckerContext) {}

    protected fun checkAndCommitReportsOn(element: CfirElement, context: DiagnosticContext, commitEverything: Boolean) {
        val source = element.source as? AbstractCjSourceElement ?: return
        reporter.checkAndCommitReportsOn(source, context, commitEverything)
    }

    protected fun report(diagnostic: CjDiagnostic?, context: CheckerContext) {
        reporter.report(diagnostic, context)
    }

    protected fun reportOn(
        source: AbstractCjSourceElement?,
        factory: CjDiagnosticFactory0,
        context: CheckerContext,
        positioningStrategy: AbstractSourceElementPositioningStrategy? = null,
    ) {
        reporter.reportOn(source, factory, context, positioningStrategy)
    }

    protected fun <A> reportOn(
        source: AbstractCjSourceElement?,
        factory: CjDiagnosticFactory1<A>,
        a: A,
        context: CheckerContext,
        positioningStrategy: AbstractSourceElementPositioningStrategy? = null,
    ) {
        reporter.reportOn(source, factory, a, context, positioningStrategy)
    }
}

