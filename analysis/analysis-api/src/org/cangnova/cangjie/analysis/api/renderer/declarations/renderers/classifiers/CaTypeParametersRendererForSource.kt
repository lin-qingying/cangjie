package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

object CaTypeParametersRendererForSource {
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
