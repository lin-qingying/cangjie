package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseCDocProvider

/**
 * CDoc 提供组件。
 *
 * 对齐 Kotlin 的 `KaFirKDocProvider` 落位，不再额外保留 `DocumentationProtocol`。
 * 结构化 CDoc 恢复与字符串文档适配统一由 `CaBaseCDocProvider` 承载。
 */
@OptIn(CaImplementationDetail::class)
internal class CaCfirCDocProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
): CaBaseCDocProvider<CaCfirSession>()
