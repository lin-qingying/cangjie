package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol

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
