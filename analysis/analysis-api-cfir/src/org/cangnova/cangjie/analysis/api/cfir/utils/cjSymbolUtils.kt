package org.cangnova.cangjie.analysis.api.cfir.utils

import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.utils.errors.requireIsInstance
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

/**
 * 从公开符号中取出底层 CFIR 符号。
 *
 * 该工具只允许用于 CFIR Analysis API 产生的符号，非 CFIR 符号会触发类型检查错误。
 */
internal val CaSymbol.cfirSymbol: CfirBasedSymbol<*>
    get() {
        requireIsInstance<CaCfirSymbol<*>>(this)
        return this.cfirSymbol
    }
