package org.cangnova.cangjie.analysis.api.renderer.types

import org.cangnova.cangjie.analysis.api.renderer.base.CaPrettyPrinter
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaUnionType

/**
 * 具名 class-like 类型的渲染器。
 */
fun interface CaUsualClassTypeRenderer {
    fun renderType(
        type: CaClassLikeType,
        typeRenderer: CaTypeRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS: CaUsualClassTypeRenderer = CaUsualClassTypeRenderer { type, typeRenderer, printer ->
            typeRenderer.renderClassId(type.classId, printer)
            typeRenderer.typeArgumentsRenderer.renderTypeArguments(type, typeRenderer, printer)
        }
    }
}

fun interface CaTupleTypeRenderer {
    fun renderType(
        type: CaTupleType,
        typeRenderer: CaTypeRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaTupleTypeRenderer = CaTupleTypeRenderer { type, typeRenderer, printer ->
            printer.append(
                type.elementTypes.joinToString(prefix = "(", postfix = ")") { elementType ->
                    typeRenderer.renderType(elementType)
                },
            )
        }
    }
}

fun interface CaIntersectionTypeRenderer {
    fun renderType(
        type: CaIntersectionType,
        typeRenderer: CaTypeRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_INTERSECTION: CaIntersectionTypeRenderer = CaIntersectionTypeRenderer { type, typeRenderer, printer ->
            printer.append(type.conjuncts.joinToString(" & ") { conjunct ->
                typeRenderer.renderType(conjunct)
            })
        }
    }
}

fun interface CaUnionTypeRenderer {
    fun renderType(
        type: CaUnionType,
        typeRenderer: CaTypeRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_UNION: CaUnionTypeRenderer = CaUnionTypeRenderer { type, typeRenderer, printer ->
            printer.append(type.alternatives.joinToString(" | ") { alternative ->
                typeRenderer.renderType(alternative)
            })
        }
    }
}
