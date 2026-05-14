package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

/**
 * 面向源码风格的类型形参 renderer 预设。
 *
 * 对齐 Kotlin Analysis API 的 `KaTypeParametersRendererForSource`。
 */
object CaTypeParametersRendererForSource {
    /**
     * 预设: 名字写在 `<...>` 内, 上界统一放到尾部 `where` 子句, 多上界以 ` & ` 分隔。
     *
     * 这贴近仓颉源码的常见排版, 避免在尖括号内堆积过多约束。
     */
    val WITH_BOUNDS_IN_WHERE_CLAUSE: CaTypeParametersRenderer = CaTypeParametersRenderer { analysisSession, owner, declarationRenderer, printer ->
        val renderedTypeParameters = owner.typeParameters.filter { typeParameter ->
            declarationRenderer.typeParametersFilter.shouldRenderTypeParameter(analysisSession, owner, typeParameter)
        }
        if (renderedTypeParameters.isEmpty()) return@CaTypeParametersRenderer

        val boundedParameters = renderedTypeParameters.filter { it.upperBounds.isNotEmpty() }
        printer {
            printCollection(
                renderedTypeParameters,
                prefix = "<",
                postfix = ">",
            ) { typeParameter ->
                append(typeParameter.name.asString())
            }

            if (boundedParameters.isNotEmpty()) {
                append(" where ")
                printCollection(
                    boundedParameters,
                ) { typeParameter ->
                    append(typeParameter.name.asString())
                    append(" <: ")
                    printCollection(
                        typeParameter.upperBounds,
                        separator = " & ",
                    ) { upperBound ->
                        declarationRenderer.typeRenderer.renderType(
                            analysisSession,
                            declarationRenderer.declarationTypeApproximator.approximateType(
                                analysisSession,
                                upperBound,
                            ),
                            this,
                        )
                    }
                }
            }
        }
    }
}
