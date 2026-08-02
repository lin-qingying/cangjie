package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponentImplementationDetail
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 顶层桥接:在当前 [CaSession] 上下文中,使用给定 [renderer] 把声明符号渲染为字符串。
 */
context(session: CaSession)
fun CaDeclarationSymbol.render(
    renderer: CaDeclarationRenderer = CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES,
): String {
    return with(session) {
        render(
            renderer = renderer,
        )
    }
}

/**
 * 声明与类型渲染协议。
 *
 * 设计要点/职责:
 * - 暴露把 [CaDeclarationSymbol] 与 [CaType] 渲染为字符串的统一入口,
 *   具体策略由调用方传入的 renderer 决定。
 * - 协议本身不参与样式选择,仅做"接收 renderer + 渲染目标"的薄薄一层。
 *
 * 对齐 Kotlin Analysis API 的 `KaRenderer`。
 */
@OptIn(CaSessionComponentImplementationDetail::class)
interface CaRenderer : CaSessionComponent {
    /**
     * 按 [renderer] 描述的策略把声明符号渲染为字符串。
     */
    fun CaDeclarationSymbol.render(
        renderer: CaDeclarationRenderer = CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES,
    ): String

    /**
     * 按 [renderer] 描述的策略把类型渲染为字符串。
     */
    fun CaType.render(
        renderer: CaTypeRenderer = CaTypeRendererForSource.WITH_QUALIFIED_NAMES,
    ): String
}
