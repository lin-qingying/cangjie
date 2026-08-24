package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContextForProvider
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile

/** 实际运行诊断组件的 CFIR visitor，将每个元素分发给 regular components 并提交 pending 报告。 */
open class CheckerRunningDiagnosticCollectorVisitor(
    /** 遍历期间持续更新的 checker context。 */
    context: CheckerContextForProvider,
    /** 当前 visitor 使用的诊断组件集合。 */
    protected val components: DiagnosticCollectorComponents
) : AbstractDiagnosticCollectorVisitor(context) {

    /** 执行不依赖具体元素的全局设置检查。 */
    override fun checkSettings() {
        components.regularComponents.forEach { it.checkSettings(context) }
    }

    /** 将当前元素交给所有常规组件检查，并在结束后提交该元素上的 pending 诊断。 */
    override fun checkElement(element: CfirElement) {
        components.regularComponents.forEach {
            element.accept(it, context)
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
}
