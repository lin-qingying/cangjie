package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrintWithSettingsFrom
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import kotlin.text.append

fun interface CaClassifierBodyRenderer {
    public fun renderBody(
        analysisSession: CaSession,
        symbol: CaDeclarationContainerSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val BODY_WITH_MEMBERS_OR_EMPTY_BRACES = CaClassifierBodyWithMembersRenderer {
            true

        }
        val NO_BODY = CaClassifierBodyRenderer { analysisSession,
                                                 symbol,
                                                 declarationRenderer,
                                                 printer ->
        }
        val BODY_WITH_MEMBERS = CaClassifierBodyWithMembersRenderer {
            false
        }
    }


}

fun interface CaClassifierBodyWithMembersRenderer : CaClassifierBodyRenderer {
    public abstract fun renderEmptyBodyForEmptyMemberScope(symbol: CaDeclarationContainerSymbol): Boolean

    public override fun renderBody(
        analysisSession: CaSession,
        symbol: CaDeclarationContainerSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    ) {
        val members = declarationRenderer.bodyMemberScopeProvider.getMemberScope(analysisSession, symbol)
            .filter { it !is CaConstructorSymbol || !it.isPrimary }
            .let { declarationRenderer.bodyMemberScopeSorter.sortMembers(analysisSession, it, symbol) }

        val membersToPrint = members.mapNotNull { member ->
            val rendered = prettyPrintWithSettingsFrom(printer) {
                declarationRenderer.renderDeclaration(analysisSession, member, this)
            }
            if (rendered.isNotEmpty()) member to rendered else null
        }

        if (membersToPrint.isEmpty() && !renderEmptyBodyForEmptyMemberScope(symbol)) return

        printer.withIndentInBraces {
            var previous: CaDeclarationSymbol? = null
            for ((member, rendered) in membersToPrint) {
                if (previous != null) {
                    printer.append(
                        declarationRenderer.codeStyle.getSeparatorBetweenMembers(
                            analysisSession,
                            previous,
                            member
                        )
                    )
                }
                previous = member
                printer.append(rendered)
            }
        }
    }
}
