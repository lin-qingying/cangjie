package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.components.CaSymbolInformationProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaDeprecationInfo
import org.cangnova.cangjie.analysis.api.symbols.CaSimpleDeprecationInfo
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.getDeprecation
import org.cangnova.cangjie.cfir.session.languageVersionSettings

/**
 * CFIR symbol 元信息组件。
 *
 * 当前仓颉公开 `CaSymbol` 已经直接提供 `createPointer()`，
 * 因此这里不再重复维护 component 级的指针入口。
 * 弃用状态读取复用 CFIR declaration 上的弃用 provider，不在 Analysis API 层重解析注解。
 */
internal class CaCfirSymbolInformationProvider(
    /**
     * 延迟取得当前 CFIR Analysis session，保留 symbol 元信息组件的标准生命周期。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSymbolInformationProvider {
    override val CaSymbol.deprecationStatus: CaDeprecationInfo?
        get() = withValidityAssertion {
            require(this is CaCfirSymbol<*>) { "${this::class}" }

            val languageVersionSettings = analysisSession.cfirSession.languageVersionSettings
            val deprecation = when (val symbol = cfirSymbol) {
                is CfirCallableSymbol<*> -> symbol.getDeprecation(languageVersionSettings)?.all
                is CfirClassLikeSymbol<*> -> symbol.getOwnDeprecation(languageVersionSettings)?.all
                else -> null
            } ?: return null

            CaSimpleDeprecationInfo(
                deprecationLevel = deprecation.deprecationLevel,
                propagatesToOverrides = deprecation.propagatesToOverrides,
                message = deprecation.getMessage(analysisSession.cfirSession),
            )
        }
}
