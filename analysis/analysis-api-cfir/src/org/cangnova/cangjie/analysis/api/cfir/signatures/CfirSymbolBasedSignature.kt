package org.cangnova.cangjie.analysis.api.cfir.signatures

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol

/**
 * 对齐 Kotlin `FirSymbolBasedSignature`。
 *
 * CFIR use-site signature 只保存底层 callable symbol 与当前 builder，
 * 其他公开语义按需从这两个入口恢复。
 */
internal interface CfirSymbolBasedSignature {
    /**
     * 签名对应的底层 CFIR callable 符号。
     */
    val cfirSymbol: CfirCallableSymbol<*>

    /**
     * 用于从 CFIR 符号和类型恢复公开 Analysis API 模型的 builder。
     */
    val cfirSymbolBuilder: CaSymbolByCfirBuilder
}
