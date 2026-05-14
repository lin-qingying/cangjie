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
 *
 * 对齐 Kotlin Analysis API 的 `KaDeclarationNameRenderer`。
 */
fun interface CaDeclarationNameRenderer {
    /** 用 symbol 自身的 [CaNamedSymbol.name] 进行渲染。 */
    fun renderName(
        analysisSession: CaSession,
        symbol: CaNamedSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter
    ) {
        renderName(analysisSession, symbol.name, symbol, declarationRenderer, printer)
    }

    /**
     * 渲染给定的 [name]。
     *
     * 当存在归属 symbol 时一并传入, 便于实现根据 symbol 元信息做特殊处理(如反引号转义)。
     */
    fun renderName(
        analysisSession: CaSession,
        name: Name,
        symbol: CaNamedSymbol?,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 预设: 直接输出名字字符串。
         *
         * 不做任何转义/反引号处理, 适合不需要消歧义的场景。
         */
        val QUOTED: CaDeclarationNameRenderer = CaDeclarationNameRenderer { _, name, _, _, printer ->
            printer.append(name.asString())
        }
    }
}
