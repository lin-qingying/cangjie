package org.cangnova.cangjie.analysis.api.renderer.declarations.impl

import org.cangnova.cangjie.analysis.api.renderer.declarations.CaCallableReturnTypeFilter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaPropertyAccessorsRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers.CaModifierListRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers.CaRendererVisibilityModifierProvider
import org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers.CaQuickDocumentationModifierPolicy
import org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers.CaQuickDocumentationModalityModifierProvider
import org.cangnova.cangjie.rendering.CangJieSemanticKind

/** 所有 Quick Doc 消费方共用的定义头渲染策略。 */
object CaDeclarationRendererForQuickDocumentation {
    /**
     * 所有 Quick Doc 消费方统一使用的语义和文本格式，不按消费方类型分支。
     *
     * 同时集中定义短类型名、有效 modality、可见性、local 参数、Unit 返回类型和访问器展开规则。
     */
    val WITH_SHORT_NAMES: CaDeclarationRenderer = CaDeclarationRendererForSource.WITH_SHORT_NAMES.with {
        modifiersRenderer = modifiersRenderer.with {
            visibilityProvider = CaRendererVisibilityModifierProvider.WITH_IMPLICIT_VISIBILITY
            modalityProvider = CaQuickDocumentationModalityModifierProvider
            modifierListRenderer = CaModifierListRenderer { analysisSession, symbol, renderer, printer ->
                printer {
                    " ".separated(
                        {
                            CaQuickDocumentationModifierPolicy.localModifier(symbol)?.let { localModifier ->
                                // `local` 也是可见性语义的一部分，必须进入语义标注，
                                // 否则 Quick Doc 里只有后面的名字有颜色，前缀 `local` 是黑色。
                                printer.withSemanticAnnotation(CangJieSemanticKind.LOCAL_VARIABLE) {
                                    append(localModifier)
                                }
                            }
                        },
                        {
                            val modifiers = buildList {
                                renderer.visibilityProvider.getVisibilityModifier(symbol)?.let(::add)
                                renderer.modalityProvider.getModalityModifier(symbol)?.let(::add)
                                addAll(renderer.otherModifiersProvider.getOtherModifiers(symbol))
                            }
                                .distinct()
                                .let { renderer.modifiersSorter.sort(analysisSession, it, symbol) }
                                .ifEmpty { return@CaModifierListRenderer }

                            printCollection(modifiers, separator = " ") { modifier -> append(modifier) }
                            append(" ")
                        },
                    )
                }
            }
        }
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.NONE
        returnTypeFilter = CaCallableReturnTypeFilter.ALWAYS
    }

}
