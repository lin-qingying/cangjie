package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjDeclarationWithBody
import org.cangnova.cangjie.psi.CjDeclarationWithInitializer
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypePattern
import org.cangnova.cangjie.analysis.api.renderer.base.CaPrettyPrinter

fun interface CaVariableInitializerRenderer {
    fun renderInitializer(symbol: CaVariableSymbol, printer: CaPrettyPrinter)

    companion object {
        val NO_INITIALIZER: CaVariableInitializerRenderer = CaVariableInitializerRenderer { _, _ -> }

        val AS_SOURCE: CaVariableInitializerRenderer = CaVariableInitializerRenderer { symbol, printer ->
            symbol.initializerText()?.let {
                printer.append(" = ")
                printer.append(it)
            }
        }

        val WITH_PLACEHOLDER: CaVariableInitializerRenderer = CaVariableInitializerRenderer { symbol, printer ->
            if (symbol.initializerText() != null) {
                printer.append(" = ...")
            }
        }
    }
}

fun interface CaFunctionLikeBodyRenderer {
    fun renderBody(
        analysisSession: CaSession,
        symbol: CaFunctionSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val NO_BODY: CaFunctionLikeBodyRenderer = CaFunctionLikeBodyRenderer { _, _, _, _ -> }

        val AS_SOURCE: CaFunctionLikeBodyRenderer = CaFunctionLikeBodyRenderer { _, symbol, _, printer ->
            symbol.bodyText()?.let { bodyText ->
                printer.append(" ")
                printer.append(bodyText)
            }
        }

        val WITH_PLACEHOLDER: CaFunctionLikeBodyRenderer = CaFunctionLikeBodyRenderer { _, symbol, _, printer ->
            val bodyText = symbol.bodyText() ?: return@CaFunctionLikeBodyRenderer
            printer.append(if (symbol.hasBlockBody()) " { ... }" else " = ...")
        }
    }
}

fun interface CaPropertyAccessorBodyRenderer {
    fun renderAccessorBody(symbol: CaPropertySymbol, printer: CaPrettyPrinter)

    companion object {
        val NO_BODY: CaPropertyAccessorBodyRenderer = CaPropertyAccessorBodyRenderer { _, _ -> }

        val AS_SOURCE: CaPropertyAccessorBodyRenderer = CaPropertyAccessorBodyRenderer { symbol, printer ->
            val getterText = symbol.getterText()
            val setterText = symbol.setterText()
            if (getterText == null && setterText == null) return@CaPropertyAccessorBodyRenderer

            printer.append(" {")
            getterText?.let {
                printer.append(" ")
                printer.append(it)
            }
            setterText?.let {
                printer.append(" ")
                printer.append(it)
            }
            printer.append(" }")
        }

        val WITH_PLACEHOLDER: CaPropertyAccessorBodyRenderer = CaPropertyAccessorBodyRenderer { symbol, printer ->
            val hasGetter = symbol.getterText() != null
            val hasSetter = symbol.setterText() != null
            if (!hasGetter && !hasSetter) return@CaPropertyAccessorBodyRenderer

            printer.append(" {")
            if (hasGetter) {
                printer.append(" get() { ... }")
            }
            if (hasSetter) {
                printer.append(" set(...) { ... }")
            }
            printer.append(" }")
        }
    }
}

fun interface CaPropertyAccessorsRenderer {
    fun renderAccessors(
        symbol: CaPropertySymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val NO_ACCESSORS: CaPropertyAccessorsRenderer = CaPropertyAccessorsRenderer { _, _, _ -> }

        val AS_RENDERED_ACCESSORS: CaPropertyAccessorsRenderer = CaPropertyAccessorsRenderer { symbol, declarationRenderer, printer ->
            declarationRenderer.accessorBodyRenderer.renderAccessorBody(symbol, printer)
        }
    }
}

private fun CaVariableSymbol.initializerText(): String? = when (val currentPsi = psi) {
    is CjDeclarationWithInitializer -> currentPsi.initializer?.text
    is CjBindingPattern -> currentPsi.variable?.initializer?.text
    is CjTypePattern -> currentPsi.variable?.initializer?.text
    else -> null
}

private fun CaFunctionSymbol.bodyText(): String? =
    (psi as? CjDeclarationWithBody)?.bodyExpression?.text

private fun CaFunctionSymbol.hasBlockBody(): Boolean =
    (psi as? CjDeclarationWithBody)?.hasBlockBody() == true

private fun CaPropertySymbol.getterText(): String? =
    (psi as? CjProperty)?.getter?.text

private fun CaPropertySymbol.setterText(): String? =
    (psi as? CjProperty)?.setter?.text
