/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.cfir.analysis.collectors.CheckerRunningDiagnosticCollectorVisitor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.cfir.LLCfirStructureElementDiagnosticsCollector
import org.cangnova.cangjie.cfir.analysis.collectors.DiagnosticCollectorComponents
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLDiagnosticsStatistics
import org.cangnova.cangjie.cfir.session.macroExpansionRegistry
import kotlin.time.TimeSource

/**
 * 对单个 CFIR structure element 执行 diagnostics 收集并返回按 PSI 元素索引的结果。
 */
internal fun collectForStructureElement(
    cfirDeclaration: CfirDeclaration,
    filter: DiagnosticCheckerFilter,
    createVisitor: (components: DiagnosticCollectorComponents) -> CheckerRunningDiagnosticCollectorVisitor,
    diagnosticsStatistics: LLDiagnosticsStatistics?,
): FileStructureElementDiagnosticList {
    val session = cfirDeclaration.moduleData.session
    val reporter = LLCfirDiagnosticReporter(
        sourceMapper = { source -> session.macroExpansionRegistry?.originSourceForGeneratedSource(source) },
    )
    val collector = LLCfirStructureElementDiagnosticsCollector(
        session,
        createVisitor,
        filter,
    )
    // 组合 filter（多个 checker 集合同时启用）在 checkerSet 上没有唯一取值，此时不上报
    // checkerPass，而不是把它算进某个集合里给出错误的归因。
    val checkerSet = filter.checkerSet
    val startedAt = if (diagnosticsStatistics != null && checkerSet != null) TimeSource.Monotonic.markNow() else null
    collector.collectDiagnostics(cfirDeclaration, reporter)
    val source = cfirDeclaration.source
    if (source != null) {
        reporter.checkAndCommitReportsOn(source, context = DiagnosticContext.Default, commitEverything = true)
    }
    val diagnostics = reporter.committedDiagnostics
    if (startedAt != null && checkerSet != null) {
        diagnosticsStatistics!!.onStructureElementCollectionFinished(checkerSet, startedAt.elapsedNow().inWholeNanoseconds, diagnostics.size)
    }
    return FileStructureElementDiagnosticList(diagnostics)
}
