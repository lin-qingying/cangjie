package org.cangnova.cangjie.macro

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirMacroExpression
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid

class DefaultMacroCollector(
    private val callInfoFactory: MacroCallInfoFactory = DefaultMacroCallInfoFactory(),
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
