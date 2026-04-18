package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaAnnotationProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol

/**
 * CFIR 注解提供器。
 *
 * 注解查询统一走 session 内部元数据协议，不让组件层自己解析 PSI。
 */
internal class CaCfirAnnotationProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaAnnotationProvider {
    override val CaDeclarationSymbol.annotations: List<CaAnnotation>
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this@annotations)
        }
}
