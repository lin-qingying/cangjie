package org.cangnova.cangjie.analysis.api.symbols

/**
 * 符号在源码结构中的声明位置。
 *
 * 相比 Kotlin 的位置模型，仓颉额外引入 [EXTEND] 来显式区分 extend 成员和普通类成员。
 */
enum class CaSymbolLocation {
    TOP_LEVEL,
    CLASS,
    EXTEND,
    PROPERTY,
    LOCAL,
}

val CaSymbol.isTopLevel: Boolean
    get() = location == CaSymbolLocation.TOP_LEVEL

val CaSymbol.isLocal: Boolean
    get() = location == CaSymbolLocation.LOCAL
