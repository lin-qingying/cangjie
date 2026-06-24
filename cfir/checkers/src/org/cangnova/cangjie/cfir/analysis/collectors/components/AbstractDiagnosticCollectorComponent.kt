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
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * 诊断收集组件基类，封装 session、pending reporter 以及常用上报工具。
 *
 * @property session 当前组件所属的 CFIR session。
 * @property reporter 当前诊断收集流程的 pending reporter。
 */
abstract class AbstractDiagnosticCollectorComponent(
    /** 当前组件所属的 CFIR session。 */
    protected val session: CfirSession,
    /** 当前诊断收集流程的 pending reporter。 */
    protected val reporter: PendingDiagnosticReporter,
) : CfirVisitor<Unit, CheckerContext>() {
    /** 默认元素访问逻辑为空，具体组件按需要覆盖特定节点访问方法。 */
    override fun visitElement(element: CfirElement, data: CheckerContext) {}

    /** 执行不依赖具体 CFIR 元素的全局设置检查。 */
    open fun checkSettings(data: CheckerContext) {}

    /** 根据元素 source 提交 pending 诊断。 */
    protected fun checkAndCommitReportsOn(element: CfirElement, context: DiagnosticContext, commitEverything: Boolean) {
        val source = element.source as? AbstractCjSourceElement ?: return
        reporter.checkAndCommitReportsOn(source, context, commitEverything)
    }

    /** 将已经构造完成的诊断交给 pending reporter。 */
    protected fun report(diagnostic: CjDiagnostic?, context: CheckerContext) {
        reporter.report(diagnostic, context)
    }

    /** 上报不带额外参数的诊断工厂。 */
    protected fun reportOn(
        source: AbstractCjSourceElement?,
        factory: CjDiagnosticFactory0,
        context: CheckerContext,
        positioningStrategy: AbstractSourceElementPositioningStrategy? = null,
    ) {
        reporter.reportOn(source, factory, context, positioningStrategy)
    }

    /** 上报携带一个参数的诊断工厂。 */
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
