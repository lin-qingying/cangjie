package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaSubstitutorProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 对齐 Kotlin `KaFirSubstitutorProvider` 的组件边界，
 * 统一提供 Analysis API 可见的公开替换器构造入口。
 */
internal class CaCfirSubstitutorProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSubstitutorProvider {
    override fun createSubstitutor(mappings: Map<CaTypeParameterSymbol, CaType>): CaSubstitutor = withValidityAssertion {
        analysisSession.cfirSymbolBuilder.typeBuilder.buildSubstitutor(mappings)
    }
}
