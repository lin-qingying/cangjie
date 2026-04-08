package org.cangnova.cangjie.analysis.api.renderer.types

import org.cangnova.cangjie.analysis.api.renderer.base.CaPrettyPrinter
import org.cangnova.cangjie.analysis.api.types.CaFunctionType

/**
 * 函数类型 kind 关键字渲染协议。
 *
 * 仓颉函数类型不仅有普通 `(P) -> R`，还携带 `cfunc`、`closure`、可变参数等公开语义。
 * 这些语义属于 type renderer 的职责，不应再被默认渲染忽略掉。
 */
fun interface CaFunctionalTypeKindRenderer {
    fun renderKind(typeRenderer: CaTypeRenderer, type: CaFunctionType, printer: CaPrettyPrinter)

    companion object {
        val NO_KEYWORDS: CaFunctionalTypeKindRenderer = CaFunctionalTypeKindRenderer { _, _, _ -> }

        val WITH_KIND_KEYWORDS: CaFunctionalTypeKindRenderer = CaFunctionalTypeKindRenderer { typeRenderer, type, printer ->
            if (type.isCFunction) {
                typeRenderer.keywordsRenderer.renderKeyword("cfunc", printer)
                printer.append(" ")
            }
            if (type.isClosureType) {
                typeRenderer.keywordsRenderer.renderKeyword("closure", printer)
                printer.append(" ")
            }
        }
    }
}

/**
 * 函数类型参数列表渲染协议。
 */
fun interface CaFunctionalTypeParameterListRenderer {
    fun renderParameters(typeRenderer: CaTypeRenderer, type: CaFunctionType, printer: CaPrettyPrinter)

    companion object {
        val AS_SOURCE: CaFunctionalTypeParameterListRenderer = CaFunctionalTypeParameterListRenderer { renderer, type, printer ->
            printer.append("(")
            type.parameterTypes.forEachIndexed { index, parameterType ->
                if (index > 0) {
                    printer.append(", ")
                }
                printer.append(renderer.renderType(parameterType))
            }
            if (type.hasVariableLengthArgument) {
                if (type.parameterTypes.isNotEmpty()) {
                    printer.append(", ")
                }
                printer.append("...")
            }
            printer.append(")")
        }
    }
}

/**
 * 函数类型返回类型渲染协议。
 */
fun interface CaFunctionalTypeReturnTypeRenderer {
    fun renderReturnType(typeRenderer: CaTypeRenderer, type: CaFunctionType, printer: CaPrettyPrinter)

    companion object {
        val AS_SOURCE: CaFunctionalTypeReturnTypeRenderer = CaFunctionalTypeReturnTypeRenderer { renderer, type, printer ->
            printer.append(" -> ")
            printer.append(renderer.renderType(type.returnType))
        }
    }
}

/**
 * 仓颉函数类型 renderer preset。
 *
 * 这里把函数类型拆成 kind / 参数列表 / 返回类型三个稳定子组件，
 * 让 source/debug preset 可以在不改动总控 `CaTypeRenderer` 的情况下继续细分。
 */
object CaFunctionalTypeRendererForSource {
    val WITH_KIND_KEYWORDS: CaFunctionalTypeRenderer = CaFunctionalTypeRenderer { renderer, type, printer ->
        CaFunctionalTypeKindRenderer.WITH_KIND_KEYWORDS.renderKind(renderer, type, printer)
        CaFunctionalTypeParameterListRenderer.AS_SOURCE.renderParameters(renderer, type, printer)
        CaFunctionalTypeReturnTypeRenderer.AS_SOURCE.renderReturnType(renderer, type, printer)
    }

    val WITHOUT_KIND_KEYWORDS: CaFunctionalTypeRenderer = CaFunctionalTypeRenderer { renderer, type, printer ->
        CaFunctionalTypeKindRenderer.NO_KEYWORDS.renderKind(renderer, type, printer)
        CaFunctionalTypeParameterListRenderer.AS_SOURCE.renderParameters(renderer, type, printer)
        CaFunctionalTypeReturnTypeRenderer.AS_SOURCE.renderReturnType(renderer, type, printer)
    }
}
