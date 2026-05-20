package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.cfir.utils.cfirSymbol
import org.cangnova.cangjie.analysis.api.components.CaTypeProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseTypeProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.expressions.withCfirSymbolEntry
import org.cangnova.cangjie.cfir.resolve.defaultType
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

/**
 * 对齐 Kotlin `KaFirTypeProvider` 的公开类型入口。
 *
 * 当前仓颉公开 API 只在这里暴露 `defaultType` 这类稳定语义，
 * 具体 CFIR type 叶子实现已经独立落位到 `cfir/types`。
 */
internal class CaCfirTypeProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseTypeProvider<CaCfirSession>(), CaTypeProvider, CaCfirSessionComponent {

    override val CaClassLikeSymbol.defaultType: CaType
        get() = withValidityAssertion {
            with(analysisSession) {
                val firSymbol = cfirSymbol
                val defaultConeType = when (firSymbol) {
                    is CfirTypeParameterSymbol -> firSymbol.defaultType
                    is CfirClassLikeSymbol<*> -> firSymbol.defaultType()
                    else -> errorWithAttachment("Unexpected ${firSymbol::class.simpleName}") {
                        withCfirSymbolEntry("symbol", firSymbol)
                    }
                }

                defaultConeType.asCaType()
            }
        }
}
