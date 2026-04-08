package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.CaPrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrint
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRendererPosition
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.psi.CjParameter

fun interface CaTypeParametersRenderer {
    fun renderTypeParameters(
        analysisSession: CaSession,
        owner: CaTypeParameterOwnerSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )
}

fun interface CaTypeParametersFilter {
    fun shouldRenderTypeParameter(
        analysisSession: CaSession,
        owner: CaTypeParameterOwnerSymbol,
        typeParameter: org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol,
    ): Boolean

    companion object {
        val ALL: CaTypeParametersFilter = CaTypeParametersFilter { _, _, _ -> true }
        val NONE: CaTypeParametersFilter = CaTypeParametersFilter { _, _, _ -> false }
    }
}

object CaTypeParametersRendererForSource {
    val WITH_BOUNDS_IN_WHERE_CLAUSE: CaTypeParametersRenderer = CaTypeParametersRenderer { analysisSession, owner, declarationRenderer, printer ->
        val renderedTypeParameters = owner.typeParameters.filter { typeParameter ->
            declarationRenderer.typeParametersFilter.shouldRenderTypeParameter(analysisSession, owner, typeParameter)
        }
        if (renderedTypeParameters.isEmpty()) return@CaTypeParametersRenderer

        printer.append(
            renderedTypeParameters.joinToString(prefix = "<", postfix = ">") { typeParameter ->
                typeParameter.name.asString()
            },
        )

        val boundedParameters = renderedTypeParameters.filter { it.upperBounds.isNotEmpty() }
        if (boundedParameters.isEmpty()) return@CaTypeParametersRenderer

        printer.append(" where ")
        printer.append(
            boundedParameters.joinToString(", ") { typeParameter ->
                val boundsText = typeParameter.upperBounds.joinToString(" & ") { upperBound ->
                    declarationRenderer.typeRenderer.renderType(
                        declarationRenderer.declarationTypeApproximator.approximateType(
                            upperBound,
                            CaTypeRendererPosition.OUT_VARIANCE,
                        ),
                    )
                }
                "${typeParameter.name.asString()} <: $boundsText"
            },
        )
    }
}

fun interface CaCallableParameterRenderer {
    fun renderParameters(
        analysisSession: CaSession,
        symbol: CaValueParameterOwnerSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )
}

object CaCallableParameterRendererForSource {
    val PARAMETERS_IN_PARENS: CaCallableParameterRenderer = CaCallableParameterRenderer { analysisSession, symbol, declarationRenderer, printer ->
        val parameterSeparator = if (declarationRenderer.codeStyle.spaceAfterComma) ", " else ","
        printer.append(
            symbol.valueParameters.joinToString(prefix = "(", postfix = ")", separator = parameterSeparator) { parameter ->
                prettyPrint {
                    renderValueParameterSource(
                        analysisSession = analysisSession,
                        symbol = parameter,
                        declarationRenderer = declarationRenderer,
                        printer = this,
                    )
                }.trim()
            },
        )
    }
}

fun interface CaCallableReceiverRenderer {
    fun renderReceiver(
        analysisSession: CaSession,
        symbol: CaCallableSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )
}

object CaCallableReceiverRendererForSource {
    val AS_TYPE_WITH_IN_APPROXIMATION: CaCallableReceiverRenderer = CaCallableReceiverRenderer { _, symbol, declarationRenderer, printer ->
        val receiverType = symbol.receiverType ?: return@CaCallableReceiverRenderer
        printer.append(
            declarationRenderer.typeRenderer.renderType(
                declarationRenderer.declarationTypeApproximator.approximateType(
                    receiverType,
                    CaTypeRendererPosition.IN_VARIANCE,
                ),
            ),
        )
        printer.append(".")
    }
}

fun interface CaCallableReturnTypeRenderer {
    fun renderReturnType(
        analysisSession: CaSession,
        symbol: CaCallableSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )
}

object CaCallableReturnTypeRendererForSource {
    val WITH_OUT_APPROXIMATION: CaCallableReturnTypeRenderer = CaCallableReturnTypeRenderer { analysisSession, symbol, declarationRenderer, printer ->
        if (!declarationRenderer.returnTypeFilter.shouldRenderReturnType(analysisSession, symbol)) return@CaCallableReturnTypeRenderer
        val renderedType = declarationRenderer.typeRenderer.renderType(
            declarationRenderer.declarationTypeApproximator.approximateType(
                symbol.returnType,
                CaTypeRendererPosition.OUT_VARIANCE,
            ),
        )
        printer.append(if (declarationRenderer.codeStyle.spaceAfterColon) ": " else ":")
        printer.append(renderedType)
    }
}

fun interface CaCallableSignatureRenderer {
    fun renderSignature(
        analysisSession: CaSession,
        symbol: CaCallableSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )
}

object CaCallableSignatureRendererForSource {
    val FOR_SOURCE: CaCallableSignatureRenderer = CaCallableSignatureRenderer { analysisSession, symbol, declarationRenderer, printer ->
        declarationRenderer.callableReceiverRenderer.renderReceiver(analysisSession, symbol, declarationRenderer, printer)
        declarationRenderer.nameRenderer.renderName(symbol.renderNameText(), printer)
        if (symbol is CaTypeParameterOwnerSymbol) {
            declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, printer)
        }
        if (symbol is CaValueParameterOwnerSymbol) {
            declarationRenderer.valueParametersRenderer.renderParameters(analysisSession, symbol, declarationRenderer, printer)
        }
        declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, printer)
    }
}

fun interface CaParameterDefaultValueRenderer {
    fun renderDefaultValue(symbol: CaValueParameterSymbol, printer: CaPrettyPrinter)

    companion object {
        val NO_DEFAULT_VALUE: CaParameterDefaultValueRenderer = CaParameterDefaultValueRenderer { _, _ -> }

        val AS_SOURCE: CaParameterDefaultValueRenderer = CaParameterDefaultValueRenderer { symbol, printer ->
            val defaultValue = (symbol.psi as? CjParameter)?.defaultValue?.text ?: return@CaParameterDefaultValueRenderer
            printer.append(" = ")
            printer.append(defaultValue)
        }

        val WITH_PLACEHOLDER: CaParameterDefaultValueRenderer = CaParameterDefaultValueRenderer { symbol, printer ->
            if (symbol.hasDefaultValue) {
                printer.append(" = ...")
            }
        }
    }
}

internal fun renderValueParameterSource(
    analysisSession: CaSession,
    symbol: CaValueParameterSymbol,
    declarationRenderer: CaDeclarationRenderer,
    printer: CaPrettyPrinter,
) {
    declarationRenderer.annotationRenderer.renderAnnotations(with(analysisSession) { symbol.annotations }, printer)
    declarationRenderer.nameRenderer.renderName(symbol.name.asString(), printer)
    if (symbol.isNamed) {
        printer.append("!")
    }
    printer.append(if (declarationRenderer.codeStyle.spaceAfterColon) ": " else ":")
    printer.append(
        declarationRenderer.typeRenderer.renderType(
            declarationRenderer.declarationTypeApproximator.approximateType(
                symbol.returnType,
                CaTypeRendererPosition.OUT_VARIANCE,
            ),
        ),
    )
    declarationRenderer.parameterDefaultValueRenderer.renderDefaultValue(symbol, printer)
}

private fun CaCallableSymbol.renderNameText(): String = when (this) {
    is CaConstructorSymbol -> "init"
    is CaFinalizerSymbol -> "finalizer"
    is CaNamedSymbol -> name.asString()
    else -> "<anonymous>"
}
