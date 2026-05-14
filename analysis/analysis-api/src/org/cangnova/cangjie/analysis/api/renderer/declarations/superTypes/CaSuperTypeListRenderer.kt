package org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol

/**
 * 超类型列表 renderer。
 *
 * 决定类/接口/struct/enum 头部"继承/实现部分"的整体排版:
 * 是否输出 `<:` 前缀, 多个超类型如何分隔等。具体单个超类型的渲染交由
 * [CaDeclarationRenderer.superTypeRenderer] 负责。
 *
 * 对齐 Kotlin Analysis API 的 `KaSuperTypeListRenderer`。
 */
fun interface CaSuperTypeListRenderer {
    /** 写出 [symbol] 的超类型列表到 [printer]。 */
    fun renderSuperTypeList(
        analysisSession: CaSession,
        symbol: CaClassSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )
}
