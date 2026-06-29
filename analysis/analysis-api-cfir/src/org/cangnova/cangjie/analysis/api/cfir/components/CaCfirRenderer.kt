package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaRenderer
import org.cangnova.cangjie.analysis.api.impl.base.CaBaseSession
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
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
    /**
     * 延迟取得当前 Analysis session，渲染时用于绑定 use-site session。
     */
    override val analysisSessionProvider: () -> CaSession,
) : CaBaseSessionComponent<CaSession>(), CaRenderer {
    /**
     * 使用指定声明 renderer 渲染公开声明符号。
     */
    override fun CaDeclarationSymbol.render(renderer: CaDeclarationRenderer): String = withValidityAssertion {
        val session = analysisSession as CaBaseSession
        with(session) {
            prettyPrint { renderer.renderDeclaration(useSiteSession, this@render, this) }
        }
    }

    /**
     * 使用指定类型 renderer 渲染公开类型。
     */
    override fun CaType.render(renderer: CaTypeRenderer): String = withValidityAssertion {
        val session = analysisSession as CaBaseSession
        with(session) {
            val approximatedType = renderer.typeApproximator.approximateType(useSiteSession, this@render)
            prettyPrint { renderer.renderType(useSiteSession, approximatedType, this) }
        }
    }
}
