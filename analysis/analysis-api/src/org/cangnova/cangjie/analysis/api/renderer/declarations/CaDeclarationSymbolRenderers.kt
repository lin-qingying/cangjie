package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.CaPrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumEntrySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaScriptSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjExtend

fun interface CaClassLikeSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaClassSymbol,
        keyword: String,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaClassLikeSymbolRenderer = CaClassLikeSymbolRenderer { analysisSession, symbol, keyword, declarationRenderer, printer ->
            declarationRenderer.modifiersRenderer.renderModifiers(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.keywordsRenderer.renderKeyword(keyword, printer)
            printer.append(" ")
            val renderedName = symbol.classId?.let(declarationRenderer.typeRenderer::renderClassId)
                ?: symbol.name?.asString()
                ?: "<anonymous-class>"
            printer.append(renderedName)
            declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.superTypeListRenderer.renderSuperTypeList(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.classifierBodyRenderer.renderBody(analysisSession, symbol, declarationRenderer, printer)
        }
    }
}

fun interface CaTypeAliasSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaTypeAliasSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaTypeAliasSymbolRenderer = CaTypeAliasSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.modifiersRenderer.renderModifiers(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.keywordsRenderer.renderKeyword("typealias", printer)
            printer.append(" ")
            val renderedName = symbol.classId?.let(declarationRenderer.typeRenderer::renderClassId)
                ?: symbol.name?.asString()
                ?: "<anonymous-alias>"
            printer.append(renderedName)
            declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, printer)
            printer.append(" = ")
            printer.append(declarationRenderer.typeRenderer.renderType(symbol.expandedType))
        }
    }
}

fun interface CaNamedFunctionSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaNamedFunctionSymbolRenderer = CaNamedFunctionSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.functionLikeKeywordRenderer.renderFunctionLike(
                analysisSession = analysisSession,
                symbol = symbol,
                keyword = "func",
                declarationRenderer = declarationRenderer,
                printer = printer,
            )
        }

        val AS_RAW_SIGNATURE: CaNamedFunctionSymbolRenderer = CaNamedFunctionSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.callableReceiverRenderer.renderReceiver(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.nameRenderer.renderName(symbol.name.asString(), printer)
            declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.valueParametersRenderer.renderParameters(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, printer)
        }
    }
}

fun interface CaConstructorSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaConstructorSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaConstructorSymbolRenderer = CaConstructorSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.modifiersRenderer.renderModifiers(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.keywordsRenderer.renderKeyword("init", printer)
            declarationRenderer.valueParametersRenderer.renderParameters(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.functionLikeBodyRenderer.renderBody(analysisSession, symbol, declarationRenderer, printer)
        }

        val AS_RAW_SIGNATURE: CaConstructorSymbolRenderer = CaConstructorSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.keywordsRenderer.renderKeyword("init", printer)
            declarationRenderer.valueParametersRenderer.renderParameters(analysisSession, symbol, declarationRenderer, printer)
        }
    }
}

fun interface CaPropertySymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaPropertySymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaPropertySymbolRenderer = CaPropertySymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.modifiersRenderer.renderModifiers(analysisSession, symbol, declarationRenderer, printer)
            if (symbol.isMutating) {
                declarationRenderer.keywordsRenderer.renderKeyword("mut", printer)
                printer.append(" ")
            }
            if (symbol.isConst) {
                declarationRenderer.keywordsRenderer.renderKeyword("const", printer)
                printer.append(" ")
            }
            declarationRenderer.keywordsRenderer.renderKeyword("prop", printer)
            printer.append(" ")
            declarationRenderer.callableReceiverRenderer.renderReceiver(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.nameRenderer.renderName(symbol.name.asString(), printer)
            declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.variableInitializerRenderer.renderInitializer(symbol, printer)
            declarationRenderer.propertyAccessorsRenderer.renderAccessors(symbol, declarationRenderer, printer)
        }

        val AS_RAW_SIGNATURE: CaPropertySymbolRenderer = CaPropertySymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.callableReceiverRenderer.renderReceiver(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.nameRenderer.renderName(symbol.name.asString(), printer)
            declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, printer)
        }
    }
}

fun interface CaFieldSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaFieldSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaFieldSymbolRenderer = CaFieldSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.modifiersRenderer.renderModifiers(analysisSession, symbol, declarationRenderer, printer)
            when {
                symbol.isConst -> declarationRenderer.keywordsRenderer.renderKeyword("const", printer)
                symbol.isVal -> declarationRenderer.keywordsRenderer.renderKeyword("let", printer)
                else -> declarationRenderer.keywordsRenderer.renderKeyword("var", printer)
            }
            printer.append(" ")
            declarationRenderer.nameRenderer.renderName(symbol.name.asString(), printer)
            declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.variableInitializerRenderer.renderInitializer(symbol, printer)
        }
    }
}

fun interface CaLocalVariableSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaVariableSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaLocalVariableSymbolRenderer = CaLocalVariableSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.modifiersRenderer.renderModifiers(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.keywordsRenderer.renderKeyword(if (symbol.isVal) "let" else "var", printer)
            printer.append(" ")
            declarationRenderer.nameRenderer.renderName(symbol.name.asString(), printer)
            declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.variableInitializerRenderer.renderInitializer(symbol, printer)
        }
    }
}

fun interface CaEnumEntrySymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaEnumEntrySymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaEnumEntrySymbolRenderer = CaEnumEntrySymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.modifiersRenderer.renderModifiers(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.nameRenderer.renderName(symbol.name.asString(), printer)
            val enumPsi = symbol.psi as? org.cangnova.cangjie.psi.CjEnumConstructor
            val payloadTypes = enumPsi?.typeReferences.orEmpty()
            if (payloadTypes.isNotEmpty()) {
                printer.append(payloadTypes.joinToString(prefix = "(", postfix = ")") { it.text })
            }
        }
    }
}

fun interface CaValueParameterSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaValueParameterSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaValueParameterSymbolRenderer = CaValueParameterSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            renderValueParameterSource(analysisSession, symbol, declarationRenderer, printer)
        }
    }
}

fun interface CaTypeParameterSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaTypeParameterSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaTypeParameterSymbolRenderer = CaTypeParameterSymbolRenderer { _, symbol, declarationRenderer, printer ->
            declarationRenderer.nameRenderer.renderName(symbol.name.asString(), printer)
            if (symbol.upperBounds.isNotEmpty()) {
                printer.append(" <: ")
                printer.append(symbol.upperBounds.joinToString(" & ") { upperBound ->
                    declarationRenderer.typeRenderer.renderType(
                        declarationRenderer.declarationTypeApproximator.approximateType(
                            upperBound,
                            org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRendererPosition.OUT_VARIANCE,
                        ),
                    )
                })
            }
        }
    }
}

fun interface CaScriptSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaScriptSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaScriptSymbolRenderer = CaScriptSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.modifiersRenderer.renderModifiers(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.keywordsRenderer.renderKeyword("script", printer)
            printer.append(" ")
            declarationRenderer.nameRenderer.renderName(symbol.name.asString(), printer)
        }
    }
}

fun interface CaExtendSymbolRenderer {
    fun renderSymbol(
        analysisSession: CaSession,
        symbol: CaExtendSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaExtendSymbolRenderer = CaExtendSymbolRenderer { analysisSession, symbol, declarationRenderer, printer ->
            declarationRenderer.modifiersRenderer.renderModifiers(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.keywordsRenderer.renderKeyword("extend", printer)
            printer.append(" ")
            printer.append(declarationRenderer.typeRenderer.renderType(symbol.extendedType))
            if (symbol.superTypes.isNotEmpty()) {
                printer.append(" <: ")
                printer.append(symbol.superTypes.joinToString(" & ") { superType ->
                    declarationRenderer.typeRenderer.renderType(superType)
                })
            }
            val members = with(analysisSession) {
                val extendPsi = symbol.getOriginalPsi() as? CjExtend
                extendPsi?.declarations
                    ?.mapNotNull { declaration ->
                        (declaration as? CjDeclaration)?.let { it.symbol as? org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol }
                    }
                    .orEmpty()
            }
            if (members.isEmpty()) {
                printer.append(" {}")
                return@CaExtendSymbolRenderer
            }
            printer.appendLine(" {")
            printer.withIndent {
                members.forEachIndexed { index, member ->
                    if (index > 0) printer.appendLine()
                    printer.append(declarationRenderer.renderDeclaration(analysisSession, member))
                }
            }
            printer.append("}")
        }
    }
}

fun interface CaFunctionLikeKeywordRenderer {
    fun renderFunctionLike(
        analysisSession: CaSession,
        symbol: CaFunctionSymbol,
        keyword: String,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )

    companion object {
        val AS_SOURCE: CaFunctionLikeKeywordRenderer = CaFunctionLikeKeywordRenderer { analysisSession, symbol, keyword, declarationRenderer, printer ->
            declarationRenderer.modifiersRenderer.renderModifiers(analysisSession, symbol, declarationRenderer, printer)
            if (symbol.isMutating) {
                declarationRenderer.keywordsRenderer.renderKeyword("mut", printer)
                printer.append(" ")
            }
            if (symbol.isConst) {
                declarationRenderer.keywordsRenderer.renderKeyword("const", printer)
                printer.append(" ")
            }
            declarationRenderer.keywordsRenderer.renderKeyword(keyword, printer)
            printer.append(" ")
            declarationRenderer.callableReceiverRenderer.renderReceiver(analysisSession, symbol, declarationRenderer, printer)
            val nameText = when (symbol) {
                is CaConstructorSymbol -> "init"
                is org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol -> "finalizer"
                is org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol -> symbol.name.asString()
                else -> "<anonymous>"
            }
            declarationRenderer.nameRenderer.renderName(nameText, printer)
            if (symbol is CaTypeParameterOwnerSymbol) {
                declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, printer)
            }
            if (symbol is CaValueParameterOwnerSymbol) {
                declarationRenderer.valueParametersRenderer.renderParameters(analysisSession, symbol, declarationRenderer, printer)
            }
            declarationRenderer.returnTypeRenderer.renderReturnType(analysisSession, symbol, declarationRenderer, printer)
            declarationRenderer.functionLikeBodyRenderer.renderBody(analysisSession, symbol, declarationRenderer, printer)
        }
    }
}
