/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.cfir.analysis.collectors.CheckerRunningDiagnosticCollectorVisitor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.cfir.LLCfirStructureElementDiagnosticsCollector
import org.cangnova.cangjie.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.analysis.collectors.DiagnosticCollectorComponents

internal fun collectForStructureElement(
    firDeclaration: CfirDeclaration,
    filter: DiagnosticCheckerFilter,
    createVisitor: (components: DiagnosticCollectorComponents) -> CheckerRunningDiagnosticCollectorVisitor,
): FileStructureElementDiagnosticList {
    val reporter = LLCfirDiagnosticReporter()
    val collector = LLCfirStructureElementDiagnosticsCollector(
        firDeclaration.moduleData.session,
        createVisitor,
        filter,
    )
    collector.collectDiagnostics(firDeclaration, reporter)
    val source = firDeclaration.source
    if (source != null) {
        reporter.checkAndCommitReportsOn(source, context = DiagnosticContext.Default, commitEverything = true)
    }
    return FileStructureElementDiagnosticList(reporter.committedDiagnostics)
}
