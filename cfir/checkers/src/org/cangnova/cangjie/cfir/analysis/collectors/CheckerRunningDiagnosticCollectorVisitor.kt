package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContextForProvider
import org.cangnova.cangjie.cfir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile

/** 实际运行诊断组件的 CFIR visitor，将每个元素分发给 regular components 并提交 pending 报告。 */
open class CheckerRunningDiagnosticCollectorVisitor(
    /** 遍历期间持续更新的 checker context。 */
    context: CheckerContextForProvider,
    /** 当前 visitor 使用的诊断组件集合。 */
    protected val components: DiagnosticCollectorComponents
) : AbstractDiagnosticCollectorVisitor(context) {

    /**
     * 逐组件耗时观察者；宿主未注册时为 `null`，遍历路径据此跳过计时。
     *
     * 在构造期解析一次而不是每次分发时去查 session 组件表：分发按「元素 × 组件」发生，
     * 是诊断遍历里频次最高的循环。
     */
    protected val checkerComponentTimingObserver: CfirCheckerComponentTimingObserver? =
        context.session.checkerComponentTimingObserverOrNull

    /** 执行不依赖具体元素的全局设置检查。 */
    override fun checkSettings() {
        components.regularComponents.forEach { it.checkSettings(context) }
    }

    /** 将当前元素交给所有常规组件检查，并在结束后提交该元素上的 pending 诊断。 */
    override fun checkElement(element: CfirElement) {
        forEachTimedCheckerComponent { component ->
            element.accept(component, context)
        }
        element.accept(components.reportCommitter, context)
    }

    /** 在文件声明遍历结束时提交仍挂起的文件级诊断。 */
    override fun onDeclarationExit(declaration: CfirDeclaration) {
        components.regularComponents.forEach { component ->
            component.onDeclarationExit(declaration, context)
        }
        if (declaration !is CfirFile) return
        components.reportCommitter.endOfFile(declaration, context)
    }

    /**
     * 按组件种类依次把当前元素交给每个常规组件，逐组件计时。
     *
     * 子类覆写 [checkElement] 时应走这里而不是自己再写一遍循环，否则会漏掉耗时归因。
     * [action] 的耗时整体计入所属组件——`checkCanceled` 与异常兜底都是这次检查的真实开销。
     */
    protected inline fun forEachTimedCheckerComponent(
        action: (component: AbstractDiagnosticCollectorComponent) -> Unit,
    ) {
        val kinds = components.checkerComponentKinds
        components.regularComponents.forEachIndexed { index, component ->
            checkerComponentTimingObserver.measureCheckerComponent(kinds[index]) {
                action(component)
            }
        }
    }
}
