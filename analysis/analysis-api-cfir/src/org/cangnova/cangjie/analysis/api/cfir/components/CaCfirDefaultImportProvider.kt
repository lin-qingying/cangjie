package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaDefaultImportProvider
import org.cangnova.cangjie.analysis.api.imports.CaDefaultImports
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion

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
