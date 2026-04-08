package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.CaPrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrint
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRendererPosition
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumEntrySymbol
import org.cangnova.cangjie.analysis.api.symbols.name

fun interface CaSuperTypeRenderer {
    fun renderSuperType(
        analysisSession: CaSession,
        superType: org.cangnova.cangjie.analysis.api.types.CaType,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )
}

fun interface CaSuperTypeListRenderer {
    fun renderSuperTypeList(
        analysisSession: CaSession,
        symbol: CaClassSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )
}

fun interface CaSuperTypesFilter {
    fun shouldRenderSuperType(
        analysisSession: CaSession,
        owner: CaClassSymbol,
        superType: org.cangnova.cangjie.analysis.api.types.CaType,
    ): Boolean

    companion object {
        val ALL: CaSuperTypesFilter = CaSuperTypesFilter { _, _, _ -> true }
        val NONE: CaSuperTypesFilter = CaSuperTypesFilter { _, _, _ -> false }
    }
}

object CaSuperTypeRendererForSource {
    val WITH_OUT_APPROXIMATION: CaSuperTypeRenderer = CaSuperTypeRenderer { _, superType, declarationRenderer, printer ->
        printer.append(
            declarationRenderer.typeRenderer.renderType(
                declarationRenderer.declarationTypeApproximator.approximateType(
                    superType,
                    CaTypeRendererPosition.OUT_VARIANCE,
                ),
            ),
        )
    }
}

object CaSuperTypeListRendererForSource {
    val AS_LIST: CaSuperTypeListRenderer = CaSuperTypeListRenderer { analysisSession, symbol, declarationRenderer, printer ->
        val superTypes = symbol.superTypes.filter { superType ->
            declarationRenderer.superTypesFilter.shouldRenderSuperType(analysisSession, symbol, superType)
        }
        if (superTypes.isEmpty()) return@CaSuperTypeListRenderer
        printer.append(" <: ")
        printer.append(
            superTypes.joinToString(" & ") { superType ->
                prettyPrint {
                    declarationRenderer.superTypeRenderer.renderSuperType(
                        analysisSession,
                        superType,
                        declarationRenderer,
                        this,
                    )
                }
            },
        )
    }
}

fun interface CaRendererBodyMemberScopeProvider {
    fun memberScope(
        analysisSession: CaSession,
        symbol: CaClassSymbol,
    ): CaScope

    companion object {
        val ALL_DECLARED: CaRendererBodyMemberScopeProvider = CaRendererBodyMemberScopeProvider { analysisSession, symbol ->
            with(analysisSession) { symbol.declaredMemberScope }
        }
    }
}

fun interface CaRendererBodyMemberScopeSorter {
    fun sort(symbols: List<CaDeclarationSymbol>): List<CaDeclarationSymbol>

    companion object {
        val BY_NAME: CaRendererBodyMemberScopeSorter = CaRendererBodyMemberScopeSorter { symbols ->
            symbols.sortedBy { symbol -> symbol.name?.asString().orEmpty() }
        }

        val ENUM_ENTRIES_AT_BEGINNING: CaRendererBodyMemberScopeSorter = CaRendererBodyMemberScopeSorter { symbols ->
            symbols.sortedWith(
                compareBy<CaDeclarationSymbol>(
                    { if (it is CaEnumEntrySymbol) 0 else 1 },
                    { it.name?.asString().orEmpty() },
                ),
            )
        }
    }
}

fun interface CaClassifierBodyRenderer {
    fun renderBody(
        analysisSession: CaSession,
        symbol: CaClassSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: CaPrettyPrinter,
    )
}

object CaClassifierBodyRendererForSource {
    val NO_BODY: CaClassifierBodyRenderer = CaClassifierBodyRenderer { _, _, _, _ -> }

    val BODY_WITH_MEMBERS: CaClassifierBodyRenderer = CaClassifierBodyRenderer { analysisSession, symbol, declarationRenderer, printer ->
        val members = declarationRenderer.bodyMemberScopeSorter.sort(
            declarationRenderer.bodyMemberScopeProvider.memberScope(analysisSession, symbol).symbols
                .filterIsInstance<CaDeclarationSymbol>(),
        )
        if (members.isEmpty()) {
            printer.append(" {}")
            return@CaClassifierBodyRenderer
        }

        printer.appendLine(" {")
        printer.withIndent {
            members.forEachIndexed { index, member ->
                if (index > 0) appendLine()
                append(declarationRenderer.renderDeclaration(analysisSession, member))
            }
        }
        printer.append("}")
    }

    val BODY_WITH_MEMBERS_OR_EMPTY_BRACES: CaClassifierBodyRenderer = CaClassifierBodyRenderer { analysisSession, symbol, declarationRenderer, printer ->
        val members = declarationRenderer.bodyMemberScopeSorter.sort(
            declarationRenderer.bodyMemberScopeProvider.memberScope(analysisSession, symbol).symbols
                .filterIsInstance<CaDeclarationSymbol>(),
        )
        if (members.isEmpty()) {
            printer.append(" {}")
            return@CaClassifierBodyRenderer
        }

        BODY_WITH_MEMBERS.renderBody(analysisSession, symbol, declarationRenderer, printer)
    }
}
