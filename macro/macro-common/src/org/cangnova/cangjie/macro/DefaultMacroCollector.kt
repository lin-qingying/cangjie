package org.cangnova.cangjie.macro

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirMacroExpression
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid

/**
 * 旧 CFIR 宏表达式扫描器。
 *
 * 新宏构造流程应从 construction surface 的真实 token 捕获中收集调用，
 * 不允许继续从 provider-visible [CfirMacroExpression] 生成 single-token
 * [MacroCallInfo]。
 */
@Deprecated(
    message = "Use macro construction surface collection instead. " +
        "Collecting provider-visible CfirMacroExpression is forbidden as a production semantic path.",
    level = DeprecationLevel.ERROR,
)
class DefaultMacroCollector(
    private val callInfoFactory: MacroCallInfoFactory,
) : MacroCollector {
    override fun collect(files: List<CfirFile>): List<MacroCallSite> {
        val sites = mutableListOf<MacroCallSite>()

        for (file in files) {
            file.accept(object : CfirVisitorVoid() {
                override fun visitElement(element: CfirElement) {
                    element.acceptChildren(this, null)
                }

                override fun visitMacroExpression(macroExpression: CfirMacroExpression) {
                    sites += MacroCallSite(
                        expression = macroExpression,
                        file = file,
                        callInfo = callInfoFactory.create(file, macroExpression),
                    )
                }
            }, null)
        }

        return sites
    }
}
