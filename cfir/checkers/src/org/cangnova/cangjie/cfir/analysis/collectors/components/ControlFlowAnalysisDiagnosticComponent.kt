package org.cangnova.cangjie.cfir.analysis.collectors.components

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.DeclarationCheckers
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 对齐 Kotlin `ControlFlowAnalysisDiagnosticComponent` 的 low-level 组装入口。
 *
 * 当前仓颉主干尚未把 CFA checker 家族正式拆入 `DeclarationCheckers` 主契约，
 * 因此该组件先承接统一入口与遍历边界，避免 low-level-api-cfir 继续私有绕开主干模块。
 * 具体 CFA checker 家族补齐后，只需要在 `analyze(...)` 中接入主干实现。
 */
class ControlFlowAnalysisDiagnosticComponent(
    session: CfirSession,
    reporter: PendingDiagnosticReporter,
    private val declarationCheckers: DeclarationCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    private fun analyze() {
        declarationCheckers.hashCode()
    }

    override fun visitFile(file: CfirFile, data: CheckerContext) {
        analyze()
    }

    override fun visitClass(klass: CfirClass, data: CheckerContext) {
        analyze()
    }

    override fun visitProperty(property: CfirProperty, data: CheckerContext) {
        analyze()
    }

    override fun visitFunction(function: CfirFunction, data: CheckerContext) {
        analyze()
    }

    override fun visitPropertyAccessor(propertyAccessor: CfirPropertyAccessor, data: CheckerContext) {
        analyze()
    }

    override fun visitConstructor(constructor: CfirConstructor, data: CheckerContext) {
        analyze()
    }
}
