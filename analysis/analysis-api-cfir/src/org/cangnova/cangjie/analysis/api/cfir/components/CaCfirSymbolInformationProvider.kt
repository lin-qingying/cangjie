package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaSymbolInformationProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent

/**
 * CFIR symbol 元信息组件。
 *
 * 当前仓颉公开 `CaSymbol` 已经直接提供 `createPointer()`，
 * 因此这里不再重复维护 component 级的指针入口。
 * 该组件保留为后续 symbol 元信息能力的稳定落点。
 */
internal class CaCfirSymbolInformationProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSymbolInformationProvider
