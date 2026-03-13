package org.cangjie.cfir.scopes

import org.cangjie.cfir.symbols.CfirClassSymbol
import org.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.Name

/**
 * 名称解析 scope 接口。
 *
 * scope 用于按名称查找符号，是名称解析的核心抽象。
 * 参考 K2 FirScope。
 */
interface CfirScope {

    /** 按名称处理类/接口/结构体/枚举符号 */
    fun processClassifiersByName(name: Name, processor: (CfirClassSymbol) -> Unit) {}

    /** 按名称处理函数符号 */
    fun processFunctionsByName(name: Name, processor: (CfirFunctionSymbol) -> Unit) {}

    /** 按名称处理属性符号 */
    fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {}
}
