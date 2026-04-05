package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaAnnotationProvider
import org.cangnova.cangjie.analysis.api.components.CaDefaultImportProvider
import org.cangnova.cangjie.analysis.api.components.CaSignatureProvider
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.psi.CjCallableDeclaration

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

/**
 * CFIR 签名提供器。
 */
internal class CaCfirSignatureProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSignatureProvider {
    override val CjCallableDeclaration.signature: CaSignature
        get() = withValidityAssertion {
            analysisSession.renderSignature(this@signature)
        }

    override val CaCallableSymbol.signature: CaSignature?
        get() = withValidityAssertion {
            analysisSession.renderSignature(this@signature)
        }
}

/**
 * CFIR 默认导入提供器。
 */
internal class CaCfirDefaultImportProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaDefaultImportProvider {
    override val defaultImports: CaDefaultImports
        get() = withValidityAssertion {
            analysisSession.renderDefaultImports()
        }
}
