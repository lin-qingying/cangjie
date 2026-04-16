package org.cangnova.cangjie.analysis.api.renderer.types.renderers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType

fun interface CaUsualClassTypeRenderer {

    fun renderType(
        analysisSession: CaSession,
        type: CaUsualClassType,
        typeRenderer: CaTypeRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS: CaUsualClassTypeRenderer =
            CaUsualClassTypeRenderer { analysisSession, type, typeRenderer, printer ->
                printer {
                    " ".separated(
                        { typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, this) },
                        {
                            typeRenderer.classIdRenderer.renderClassTypeQualifier(analysisSession, type, type.qualifiers, typeRenderer, this)
                            typeRenderer.typeNameRenderer.renderName(
                                analysisSession,
                                type.classId.shortClassName,
                                type,
                                typeRenderer,
                                this,
                            )
                            printCollectionIfNotEmpty(
                                type.typeArguments,
                                prefix = "<",
                                postfix = ">",
                            ) { typeArgument ->
                                typeRenderer.renderType(analysisSession, typeArgument, this)
                            }
                        },
                    )
                }
            }

        val AS_CLASS_TYPE_WITHOUT_TYPE_ARGUMENTS: CaUsualClassTypeRenderer =
            CaUsualClassTypeRenderer { analysisSession, type, typeRenderer, printer ->
                printer {
                    " ".separated(
                        { typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, this) },
                        {
                            typeRenderer.classIdRenderer.renderClassTypeQualifier(analysisSession, type, type.qualifiers, typeRenderer, this)
                            typeRenderer.typeNameRenderer.renderName(
                                analysisSession,
                                type.classId.shortClassName,
                                type,
                                typeRenderer,
                                this,
                            )
                        },
                    )
                }
            }
    }
}
