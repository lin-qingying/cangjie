package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaSourceProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent

/**
 * 对位 Kotlin `KaFirSourceProvider` 的 CFIR source provider。
 *
 * 当前仓颉没有 Kotlin klib `source file name` 的同构语义，因此这里只保留 Kotlin 对位类型，
 * 不额外发明仓颉专属导航入口或 fallback 协议。
 */
internal class CaCfirSourceProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSourceProvider {
}
