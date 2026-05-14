package org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes

import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrint

/**
 * 面向源码风格的超类型列表预设。
 *
 * 对齐 Kotlin Analysis API 的 `KaSuperTypeListRendererForSource`。
 */
object CaSuperTypeListRendererForSource {
    /**
     * 预设: 按仓颉源码风格输出超类型列表。
     *
     * - 以 ` <: ` 作为引导(对齐仓颉语法);
     * - 多个超类型之间用 ` & ` 连接(交集语义);
     * - 经过 [CaDeclarationRenderer.superTypesFilter] 过滤后再渲染;
     * - 列表为空时不输出任何文本。
     */
    val AS_LIST: CaSuperTypeListRenderer = CaSuperTypeListRenderer { analysisSession, symbol, declarationRenderer, printer ->
        val superTypes = symbol.superTypes.filter { superType ->
            declarationRenderer.superTypesFilter.shouldRenderSuperType(analysisSession, symbol, superType)
        }
        if (superTypes.isEmpty()) return@CaSuperTypeListRenderer
        printer.append(" <: ")
        printer.append(
            superTypes.joinToString(" & ") { superType ->
                prettyPrint {
                    declarationRenderer.superTypeRenderer.renderSuperType(
                        analysisSession,
                        superType,
                        declarationRenderer,
                        this,
                    )
                }
            },
        )
    }
}
