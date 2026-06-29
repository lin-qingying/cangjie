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

/**
 * 针对文件结构元素创建 checker-running visitor 的 diagnostics collector。
 */
internal class LLCfirStructureElementDiagnosticsCollector(
    session: CfirSession,
    /**
     * 根据 diagnostics 组件创建实际 checker-running visitor 的工厂。
     */
    private val doCreateVisitor: (components: DiagnosticCollectorComponents) -> CheckerRunningDiagnosticCollectorVisitor,
    filter: DiagnosticCheckerFilter,
) : AbstractLLCfirDiagnosticsCollector(
    session,
    filter,
) {
    /**
     * 使用外部注入的 visitor 工厂创建 diagnostics visitor。
     */
    override fun createVisitor(
        components: DiagnosticCollectorComponents,
        reporter: PendingDiagnosticReporter,
    ): CheckerRunningDiagnosticCollectorVisitor {
        return doCreateVisitor(components)
    }
}
