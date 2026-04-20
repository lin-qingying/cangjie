package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

object CaDeclarationModifiersRendererForSource {
    val NO_IMPLICIT_MODIFIERS: CaDeclarationModifiersRenderer = CaDeclarationModifiersRenderer {
        modifierListRenderer = CaModifierListRenderer.AS_LIST
        modifiersSorter = CaModifiersSorter.CANONICAL
        modalityProvider = CaRendererModalityModifierProvider.NO_IMPLICIT_MODALITY
        visibilityProvider = CaRendererVisibilityModifierProvider.NO_IMPLICIT_VISIBILITY
        otherModifiersProvider = CaRendererOtherModifiersProvider.ALL
    }

    val WITH_IMPLICIT_MODIFIERS: CaDeclarationModifiersRenderer = NO_IMPLICIT_MODIFIERS.with {
        modalityProvider = CaRendererModalityModifierProvider.WITH_IMPLICIT_MODALITY
        visibilityProvider = CaRendererVisibilityModifierProvider.WITH_IMPLICIT_VISIBILITY
    }
}
