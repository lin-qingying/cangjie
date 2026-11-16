package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol

/**
 * typealias 渲染器。
 *
 * 对齐 Kotlin Analysis API 的 `KaTypeAliasSymbolRenderer`。
 */
fun interface CaTypeAliasSymbolRenderer {
    /** 渲染 typealias [symbol] 到 [printer]。 */
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaTypeAliasSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 使用仓颉源码语法输出 `type Name<Tp> = T` 形式。
         *
         * `typealias` 是类型别名的语义名称，不是仓颉源码关键字。
         */
        val AS_SOURCE: CaTypeAliasSymbolRenderer = CaTypeAliasSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                append("type")
                append(" ")
                symbol.name?.let { name ->
                    declarationRenderer.nameRenderer.renderName(analysisSession, name, symbol, declarationRenderer, this)
                } ?: append("<anonymous-alias>")
                declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, this)
                append(" = ")
                declarationRenderer.typeRenderer.renderType(analysisSession, symbol.expandedType, this)
            }
        }
    }
}
