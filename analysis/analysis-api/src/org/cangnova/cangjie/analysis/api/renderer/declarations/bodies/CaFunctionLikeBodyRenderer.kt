package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol

/**
 * 函数体 renderer。
 *
 * 对齐 Kotlin analysis-api：
 * 函数符号层没有公开稳定的 body 语义时，
 * renderer 只保留能力位，不自行从 PSI 恢复源码函数体。
 */
fun interface CaFunctionLikeBodyRenderer {
    fun renderBody(
        analysisSession: CaSession,
        symbol: CaFunctionSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        val NO_BODY: CaFunctionLikeBodyRenderer = CaFunctionLikeBodyRenderer { _, _, _, _ -> }
    }
}
