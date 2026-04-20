package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol

/**
 * 属性访问器体 renderer。
 *
 * 对齐 Kotlin analysis-api：
 * analysis-api 若未公开稳定 accessor body 语义，
 * renderer 就只保留能力位，不回读 PSI。
 */
fun interface CaPropertyAccessorBodyRenderer {
    fun renderAccessorBody(
        analysisSession: CaSession,
        symbol: CaPropertySymbol,
        printer: PrettyPrinter,
    )

    companion object {
        val NO_BODY: CaPropertyAccessorBodyRenderer = CaPropertyAccessorBodyRenderer { _, _, _ -> }
    }
}
