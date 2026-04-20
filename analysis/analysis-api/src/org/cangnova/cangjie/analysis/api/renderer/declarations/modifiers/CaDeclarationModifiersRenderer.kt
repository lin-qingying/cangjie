package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol

/**
 * 声明级修饰符渲染器。
 *
 * 它统一承载可见性、模态与仓颉声明前缀修饰符的输出规则，
 * 避免各个 symbol renderer 分别拼接导致格式漂移。
 */
class CaDeclarationModifiersRenderer private constructor(
    val modifierListRenderer: CaModifierListRenderer,
    val modifiersSorter: CaModifiersSorter,
    val modalityProvider: CaRendererModalityModifierProvider,
    val visibilityProvider: CaRendererVisibilityModifierProvider,
    val otherModifiersProvider: CaRendererOtherModifiersProvider,
) {
    fun renderDeclarationModifiers(analysisSession: CaSession, symbol: CaDeclarationSymbol, printer: PrettyPrinter) {
        modifierListRenderer.renderModifiers(analysisSession, symbol, this, printer)
    }

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

    class Builder {
        lateinit var modifierListRenderer: CaModifierListRenderer
        lateinit var modifiersSorter: CaModifiersSorter
        lateinit var modalityProvider: CaRendererModalityModifierProvider
        lateinit var visibilityProvider: CaRendererVisibilityModifierProvider
        lateinit var otherModifiersProvider: CaRendererOtherModifiersProvider

        fun build(): CaDeclarationModifiersRenderer = CaDeclarationModifiersRenderer(
            modifierListRenderer = modifierListRenderer,
            modifiersSorter = modifiersSorter,
            modalityProvider = modalityProvider,
            visibilityProvider = visibilityProvider,
            otherModifiersProvider = otherModifiersProvider,
        )
    }

    companion object {
        operator fun invoke(action: Builder.() -> Unit): CaDeclarationModifiersRenderer =
            Builder().apply(action).build()
    }
}
