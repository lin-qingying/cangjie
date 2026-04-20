package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol

/**
 * 变量初始化器 renderer。
 *
 * 对齐 Kotlin analysis-api：
 * analysis-api 公共变量符号若未公开稳定 initializer 语义，
 * renderer 就不应自行从 PSI 恢复源码初始化器。
 */
fun interface CaVariableInitializerRenderer {
    fun renderInitializer(
        analysisSession: CaSession,
        symbol: CaVariableSymbol,
        printer: PrettyPrinter,
    )

    companion object {
        val NO_INITIALIZER: CaVariableInitializerRenderer = CaVariableInitializerRenderer { _, _, _ -> }
    }
}
