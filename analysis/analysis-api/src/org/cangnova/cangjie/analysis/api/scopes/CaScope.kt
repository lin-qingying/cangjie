package org.cangnova.cangjie.analysis.api.scopes

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.Name

/**
 * Analysis API 的公开作用域视图。
 *
 * 作用域不是简单的“符号列表”，而是一套稳定的按名称查询协议。
 * IDE、引用解析、补全与测试框架都应通过这里观察“当前上下文有哪些声明可见”，
 * 而不是直接扫描底层 CFIR 结构。
 */
interface CaScope : CaLifetimeOwner {
    /**
     * 当前作用域已经物化出来的全部符号。
     *
     * 该集合适合调试、测试断言与一次性枚举；真正的按名称查询
     * 应优先走 [getSymbols]、[getCallableSymbols] 与 [getClassifierSymbols]。
     */
    val symbols: List<CaSymbol>

    /**
     * 当前作用域已经建立稳定索引的名字集合。
     *
     * 某些底层 provider 只能保证“按名字查询”，不能保证“完整枚举全部名字”。
     * 因此这里表达的是“当前可直接索引的名字”，而不是理论上的全部候选。
     */
    val availableNames: Set<Name>

    /**
     * 查询指定名字在当前作用域内可见的所有符号。
     */
    fun getSymbols(name: Name): List<CaSymbol>

    /**
     * 查询指定名字在当前作用域内可见的所有可调用符号。
     */
    fun getCallableSymbols(name: Name): List<CaCallableSymbol>

    /**
     * 查询指定名字在当前作用域内可见的所有 class-like 符号。
     */
    fun getClassifierSymbols(name: Name): List<CaClassifierSymbol>
}
