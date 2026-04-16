package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.name.Name

/**
 * 声明名称渲染协议。
 *
 * 这一层只负责“名字本身如何输出”，不承载关键字、修饰符、签名等更高层格式决策，
 * 以便 hover、signature help、文档渲染共享同一套声明结构。
 */
fun interface CaDeclarationNameRenderer {
    public fun renderName(
        analysisSession: CaSession,
        symbol: CaNamedSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter
    ) {
        renderName(analysisSession, symbol.name, symbol, declarationRenderer, printer)
    }

    public fun renderName(
        analysisSession: CaSession,
        name: Name,
        symbol: CaNamedSymbol?,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val QUOTED: CaDeclarationNameRenderer = CaDeclarationNameRenderer { _, name, _, _, printer ->
            printer.append(name.asString())
        }
    }
}
