package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

/**
 * 面向源码展示的修饰符渲染器预设。
 *
 * - [NO_IMPLICIT_MODIFIERS]: 仅渲染源码中显式书写的可见性/模态;
 * - [WITH_IMPLICIT_MODIFIERS]: 补全隐式默认值(便于调试与 stub 文本)。
 *
 * 对齐 Kotlin Analysis API 的 `KaDeclarationModifiersRendererForSource`。
 */
object CaDeclarationModifiersRendererForSource {
    /**
     * 预设: 不输出隐式修饰符。
     *
     * 仅显示源码中明确写出的可见性 / 模态, 其他修饰符(static/const/override 等)按实际值输出。
     * 适合贴近源码原貌的展示。
     */
    val NO_IMPLICIT_MODIFIERS: CaDeclarationModifiersRenderer = CaDeclarationModifiersRenderer {
        modifierListRenderer = CaModifierListRenderer.AS_LIST
        modifiersSorter = CaModifiersSorter.CANONICAL
        modalityProvider = CaRendererModalityModifierProvider.NO_IMPLICIT_MODALITY
        visibilityProvider = CaRendererVisibilityModifierProvider.NO_IMPLICIT_VISIBILITY
        otherModifiersProvider = CaRendererOtherModifiersProvider.ALL
    }

    /**
     * 预设: 补全隐式修饰符。
     *
     * 包括默认 `public` 可见性、默认 `final` 模态等; 用于 debug/stub 等需要完整信息的场景。
     */
    val WITH_IMPLICIT_MODIFIERS: CaDeclarationModifiersRenderer = NO_IMPLICIT_MODIFIERS.with {
        modalityProvider = CaRendererModalityModifierProvider.WITH_IMPLICIT_MODALITY
        visibilityProvider = CaRendererVisibilityModifierProvider.WITH_IMPLICIT_VISIBILITY
    }
}
