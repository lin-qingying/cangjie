package org.cangnova.cangjie.cfir.analysis.checkers.config

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter

abstract class CfirLanguageVersionSettingsChecker {
    /**
     * This API allows us to check language version settings independently of particular code pieces.
     */
    context(context: CheckerContext)
    abstract fun check(reporter: DiagnosticReporter)
}

