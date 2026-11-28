/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.cfir.analysis.collectors.CheckerRunningDiagnosticCollectorVisitor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.cfir.LLCfirStructureElementDiagnosticsCollector
import org.cangnova.cangjie.cfir.analysis.collectors.DiagnosticCollectorComponents
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.session.macroExpansionRegistry
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirResolvableSession

/**
 * 对单个 CFIR structure element 执行 diagnostics 收集并返回按 PSI 元素索引的结果。
 */
internal fun collectForStructureElement(
    cfirDeclaration: CfirDeclaration,
    checkerComponents: LLCfirModuleResolveComponents,
    filter: DiagnosticCheckerFilter,
    mppCheckerKind: MppCheckerKind?,
    createVisitor: (components: DiagnosticCollectorComponents) -> CheckerRunningDiagnosticCollectorVisitor,
): FileStructureElementDiagnosticList {
    val sourceSession = cfirDeclaration.moduleData.session
    val reporter = LLCfirDiagnosticReporter(
        sourceMapper = { source ->
            val origin = sourceSession.macroExpansionRegistry?.originSourceForGeneratedSource(source) ?: source
            cfirDeclaration.llCfirResolvableSession?.moduleComponents?.cache?.originalSource(origin) ?: origin
        },
    )
    val collector = LLCfirStructureElementDiagnosticsCollector(
        checkerComponents.session,
        createVisitor,
        filter,
        mppCheckerKind,
    )
    collector.collectDiagnostics(cfirDeclaration, reporter)
    val source = cfirDeclaration.source
    if (source != null) {
        reporter.checkAndCommitReportsOn(source, context = DiagnosticContext.Default, commitEverything = true)
    }
    return FileStructureElementDiagnosticList(reporter.committedDiagnostics)
}
