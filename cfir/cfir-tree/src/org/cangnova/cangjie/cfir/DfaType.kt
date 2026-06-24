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
    /**
     * 数据流类型为 Cone 类型。
     *
     * @property type 数据流分析追踪的 Cone 类型。
     */
    data class Cone(val type: ConeCangJieType) : DfaType() {
        /**
         * 返回 Cone 类型文本。
         */
        override fun toString(): String = "$type"
    }

    /**
     * 数据流类型为 CFIR symbol。
     *
     * @property symbol 数据流分析追踪的 symbol。
     */
    data class Symbol(val symbol: CfirBasedSymbol<*>) : DfaType() {
        /**
         * 返回 symbol 文本。
         */
        override fun toString(): String = "$symbol"
    }

    /**
     * 数据流类型为布尔常量。
     *
     * @property value 布尔常量值。
     */
    data class BooleanLiteral(val value: Boolean) : DfaType() {
        /**
         * 返回布尔常量文本。
         */
        override fun toString(): String = "$value"
    }
}
