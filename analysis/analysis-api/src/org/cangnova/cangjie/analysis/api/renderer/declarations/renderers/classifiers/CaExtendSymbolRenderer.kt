package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjExtend

/**
 * extend 块渲染器(仓颉特有的"为类型补充成员"语法)。
 *
 * 对齐概念上接近 Kotlin Analysis API 的 extension/companion 渲染, 但语义贴合仓颉。
 */
fun interface CaExtendSymbolRenderer {
    /** 渲染 extend 符号 [symbol] 到 [printer]。 */
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaExtendSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 预设: 按源码风格输出。
         *
         * - 修饰符 / `extend` / 被扩展类型 / 可选 `<:` 超类型列表;
         * - 主体为空时输出 ` {}`, 非空时换行并缩进逐个渲染成员。
         */
        val AS_SOURCE: CaExtendSymbolRenderer = CaExtendSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, this)
                append("extend")
                append(" ")
                declarationRenderer.typeRenderer.renderType(analysisSession, symbol.extendedType, this)
                if (symbol.superTypes.isNotEmpty()) {
                    append(" <: ")
                    printCollection(
                        symbol.superTypes,
                        separator = " & ",
                    ) { superType ->
                        declarationRenderer.typeRenderer.renderType(analysisSession, superType, this)
                    }
                }
            }
            val members = with(analysisSession) {
                val extendPsi = symbol.psi as? CjExtend
                extendPsi?.declarations
                    ?.mapNotNull { declaration ->
                        declaration?.let { it.symbol }
                    }
                    .orEmpty()
            }
            if (members.isEmpty()) {
                printer.append(" {}")
                return@CaExtendSymbolRenderer
            }
            printer.appendLine(" {")
            printer.withIndent {
                members.forEachIndexed { index, member ->
                    if (index > 0) printer.appendLine()
                    printer.append(declarationRenderer.renderDeclaration(analysisSession, member))
                }
            }
            printer.append("}")
        }
    }
}
