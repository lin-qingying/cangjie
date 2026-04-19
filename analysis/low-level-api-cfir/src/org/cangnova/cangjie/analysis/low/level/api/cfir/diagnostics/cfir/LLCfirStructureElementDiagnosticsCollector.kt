/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter

import org.cangnova.cangjie.cfir.analysis.collectors.CheckerRunningDiagnosticCollectorVisitor
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.AbstractLLCfirDiagnosticsCollector
import org.cangnova.cangjie.cfir.analysis.collectors.DiagnosticCollectorComponents
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession

internal class LLCfirStructureElementDiagnosticsCollector(
    session: CfirSession,
    private val doCreateVisitor: (components: DiagnosticCollectorComponents) -> CheckerRunningDiagnosticCollectorVisitor,
    filter: DiagnosticCheckerFilter,
) : AbstractLLCfirDiagnosticsCollector(
    session,
    filter,
) {
    override fun createVisitor(
        components: DiagnosticCollectorComponents,
        reporter: PendingDiagnosticReporter,
    ): CheckerRunningDiagnosticCollectorVisitor {
        return doCreateVisitor(components)
    }
}
