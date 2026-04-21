package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 数据流分析中的抽象类型表示。
 *
 * 它对位 Kotlin FIR tree 层的 `DfaType`，因此必须属于 CFIR 树模型层，
 * 不能落在 `cfir:resolve` 这类后续阶段模块中。
 */
sealed class DfaType {
    data class Cone(val type: ConeCangJieType) : DfaType() {
        override fun toString(): String = "$type"
    }

    data class Symbol(val symbol: CfirBasedSymbol<*>) : DfaType() {
        override fun toString(): String = "$symbol"
    }

    data class BooleanLiteral(val value: Boolean) : DfaType() {
        override fun toString(): String = "$value"
    }
}
