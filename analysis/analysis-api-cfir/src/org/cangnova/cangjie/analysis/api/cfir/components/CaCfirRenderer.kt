package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaRenderer
import org.cangnova.cangjie.analysis.api.impl.base.CaBaseSession
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrint
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 统一文本渲染入口。
 *
 * 组件只做会话绑定与 pretty-printer 编排，具体渲染语义仍由 renderer 自身决定。
 */
internal class CaCfirRenderer(
    override val analysisSessionProvider: () -> CaSession,
) : CaBaseSessionComponent<CaSession>(), CaRenderer {
    override fun CaDeclarationSymbol.render(renderer: CaDeclarationRenderer): String = withValidityAssertion {
        val session = analysisSession as CaBaseSession
        with(session) {
            prettyPrint { renderer.renderDeclaration(useSiteSession, this@render, this) }
        }
    }

    override fun CaType.render(renderer: CaTypeRenderer): String = withValidityAssertion {
        val session = analysisSession as CaBaseSession
        with(session) {
            val approximatedType = renderer.typeApproximator.approximateType(useSiteSession, this@render)
            prettyPrint { renderer.renderType(useSiteSession, approximatedType, this) }
        }
    }
}
