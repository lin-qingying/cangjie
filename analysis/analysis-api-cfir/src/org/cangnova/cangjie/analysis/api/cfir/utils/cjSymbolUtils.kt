package org.cangnova.cangjie.analysis.api.cfir.utils

import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.utils.errors.requireIsInstance
import org.cangnova.cangjie.cfir.dispatchReceiverClassLookupTagOrNull
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol

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

/**
 * 对位 Kotlin getContainingKtModule：普通声明取真实模块，fake override 取其接收者类型的模块。
 * 仓颉 extend 的声明模块独立于被扩展类型，必须先读取真实 extend owner，不能归属到目标类型模块。
 */
internal fun CfirBasedSymbol<*>.getContainingCaModule(resolutionFacade: LLResolutionFacade): CaModule {
    val target = when (this) {
        is CfirCallableSymbol<*> -> {
            val extend = getContainingExtend()
            if (extend != null) return extend.llCfirModuleData.caModule
            dispatchReceiverClassLookupTagOrNull()?.toSymbol(resolutionFacade.useSiteCfirSession) ?: this
        }
        else -> this
    }
    return target.llCfirModuleData.caModule
}
