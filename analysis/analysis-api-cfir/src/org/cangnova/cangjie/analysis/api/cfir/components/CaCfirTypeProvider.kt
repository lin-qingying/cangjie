package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirClassLikeSymbolBase
import org.cangnova.cangjie.analysis.api.components.CaTypeProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 对齐 Kotlin `KaFirTypeProvider` 的公开类型入口。
 *
 * 当前仓颉公开 API 只在这里暴露 `defaultType` 这类稳定语义，
 * 具体 CFIR type 叶子实现已经独立落位到 `cfir/types`。
 */
internal class CaCfirTypeProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaTypeProvider, CaCfirSessionComponent {
    override val CaClassLikeSymbol.defaultType: CaType
        get() = withValidityAssertion {
            when (this@defaultType) {
                is CaCfirClassLikeSymbolBase<*> -> analysisSession.typeQueries.queryClassLikeDefaultType(backingSymbol)?.asPublicType()
                    ?: error("Cannot build default type for `${classId?.asString() ?: "<anonymous>"}`")
                else -> error("Only CFIR class-like symbols can expose defaultType: ${this@defaultType::class.simpleName}")
            }
        }
}
