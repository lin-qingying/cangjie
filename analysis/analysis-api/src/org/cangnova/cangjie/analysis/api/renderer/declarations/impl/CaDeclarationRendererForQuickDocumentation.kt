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
                                // 对齐 Kotlin Quick Doc：local 是可见性修饰词，绑定名字另行标注。
                                printer.withSemanticAnnotation(CangJieSemanticKind.KEYWORD) {
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
