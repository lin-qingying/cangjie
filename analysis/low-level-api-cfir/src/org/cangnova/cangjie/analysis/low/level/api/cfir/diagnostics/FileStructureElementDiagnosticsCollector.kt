/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CaDiagnosticCheckerSet
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.cfir.analysis.collectors.CheckerRunningDiagnosticCollectorVisitor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.cfir.LLCfirStructureElementDiagnosticsCollector
import org.cangnova.cangjie.cfir.analysis.collectors.DiagnosticCollectorComponents
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.domains.LLDiagnosticsStatistics
import org.cangnova.cangjie.cfir.session.macroExpansionRegistry
import kotlin.time.TimeSource
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
    diagnosticsStatistics: LLDiagnosticsStatistics?,
): FileStructureElementDiagnosticList {
    val sourceSession = cfirDeclaration.moduleData.session
    val reporter = LLCfirDiagnosticReporter(
        sourceMapper = { source ->
            val origin = sourceSession.macroExpansionRegistry?.originSourceForGeneratedSource(source) ?: source
            cfirDeclaration.llCfirResolvableSession?.moduleComponents?.cache?.originalSource(origin) ?: origin
        },
    )
    reporter.beginDiagnosticCollection()
    val collector = LLCfirStructureElementDiagnosticsCollector(
        checkerComponents.session,
        createVisitor,
        filter,
        mppCheckerKind,
    )
    // 组合 filter（多个 checker 集合同时启用）在 checkerSet 上没有唯一取值，此时不上报
    // checkerPass，而不是把它算进某个集合里给出错误的归因。
    val checkerSet = filter.checkerSet
    val statistics = if (checkerSet != null) diagnosticsStatistics else null
    if (statistics != null && checkerSet != null) {
        return collectForStructureElementTimed(cfirDeclaration, checkerSet, collector, reporter, statistics)
    }

    collector.collectDiagnostics(cfirDeclaration, reporter)
    val source = cfirDeclaration.source
    if (source != null) {
        reporter.checkAndCommitReportsOn(source, context = DiagnosticContext.Default, commitEverything = true)
    }
    return FileStructureElementDiagnosticList(reporter.committedDiagnostics)
}

/**
 * [collectForStructureElement] 的计时路径：打开链路 span、执行收集、在 finally 中结束 span
 * 并记录耗时。失败同样记录——失败的收集也是用户等掉的耗时，与其余接缝的失败口径一致。
 */
private fun collectForStructureElementTimed(
    cfirDeclaration: CfirDeclaration,
    checkerSet: CaDiagnosticCheckerSet,
    collector: LLCfirStructureElementDiagnosticsCollector,
    reporter: LLCfirDiagnosticReporter,
    statistics: LLDiagnosticsStatistics,
): FileStructureElementDiagnosticList {
    statistics.onStructureElementCollectionStarted(checkerSet)
    val startedAt = TimeSource.Monotonic.markNow()
    try {
        collector.collectDiagnostics(cfirDeclaration, reporter)
        val source = cfirDeclaration.source
        if (source != null) {
            reporter.checkAndCommitReportsOn(source, context = DiagnosticContext.Default, commitEverything = true)
        }
        return FileStructureElementDiagnosticList(reporter.committedDiagnostics)
    } finally {
        statistics.onStructureElementCollectionFinished(
            checkerSet,
            startedAt.elapsedNow().inWholeNanoseconds,
            reporter.committedDiagnostics.size,
        )
    }
}
