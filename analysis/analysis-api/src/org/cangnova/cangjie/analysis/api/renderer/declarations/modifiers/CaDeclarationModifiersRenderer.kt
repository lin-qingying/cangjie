package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol

/**
 * 声明级修饰符渲染器。
 *
 * 它统一承载可见性、模态与仓颉声明前缀修饰符的输出规则，
 * 避免各个 symbol renderer 分别拼接导致格式漂移。
 *
 * - 通过 [modifierListRenderer] 决定整体排版与分隔;
 * - 通过 [modifiersSorter] 决定排序;
 * - 通过 [modalityProvider] / [visibilityProvider] / [otherModifiersProvider]
 *   决定具体修饰符的提取方式。
 *
 * 对齐 Kotlin Analysis API 的 `KaDeclarationModifiersRenderer`。
 */
class CaDeclarationModifiersRenderer private constructor(
    val modifierListRenderer: CaModifierListRenderer,
    val modifiersSorter: CaModifiersSorter,
    val modalityProvider: CaRendererModalityModifierProvider,
    val visibilityProvider: CaRendererVisibilityModifierProvider,
    val otherModifiersProvider: CaRendererOtherModifiersProvider,
) {
    /** 渲染 [symbol] 的修饰符列表到 [printer]。 */
    fun renderDeclarationModifiers(analysisSession: CaSession, symbol: CaDeclarationSymbol, printer: PrettyPrinter) {
        modifierListRenderer.renderModifiers(analysisSession, symbol, this, printer)
    }

    /** 在当前 renderer 基础上派生新配置, 未覆盖字段沿用原值。 */
    fun with(action: Builder.() -> Unit): CaDeclarationModifiersRenderer {
        val current = this
        return Builder().apply {
            modifierListRenderer = current.modifierListRenderer
            modifiersSorter = current.modifiersSorter
            modalityProvider = current.modalityProvider
            visibilityProvider = current.visibilityProvider
            otherModifiersProvider = current.otherModifiersProvider
            action()
        }.build()
    }

    /** 修饰符渲染器构建器, 字段必须在 [build] 前赋值。 */
    class Builder {
        /** 整体修饰符列表渲染策略。 */
        lateinit var modifierListRenderer: CaModifierListRenderer
        /** 修饰符排序策略。 */
        lateinit var modifiersSorter: CaModifiersSorter
        /** 模态修饰符(open/sealed/abstract/final)提供者。 */
        lateinit var modalityProvider: CaRendererModalityModifierProvider
        /** 可见性修饰符(public/private/...)提供者。 */
        lateinit var visibilityProvider: CaRendererVisibilityModifierProvider
        /** 其他修饰符(static/const/override/unsafe 等)提供者。 */
        lateinit var otherModifiersProvider: CaRendererOtherModifiersProvider

        /** 构建最终的修饰符渲染器。 */
        fun build(): CaDeclarationModifiersRenderer = CaDeclarationModifiersRenderer(
            modifierListRenderer = modifierListRenderer,
            modifiersSorter = modifiersSorter,
            modalityProvider = modalityProvider,
            visibilityProvider = visibilityProvider,
            otherModifiersProvider = otherModifiersProvider,
        )
    }

    companion object {
        /** DSL 入口, 等价于 `Builder().apply(action).build()`。 */
        operator fun invoke(action: Builder.() -> Unit): CaDeclarationModifiersRenderer =
            Builder().apply(action).build()
    }
}
