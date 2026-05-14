package org.cangnova.cangjie.analysis.api.scopes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.Name

/**
 * Analysis API 的公开作用域视图。
 *
 * 作用域不是简单的“符号列表”，而是一套稳定的按名称查询协议。
 * IDE、引用解析、补全与测试框架都应通过这里观察“当前上下文有哪些声明可见”，
 * 而不是直接扫描底层 CFIR 结构。
 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class)
interface CaScope : CaScopeLike {



    /**
     * 当前作用域内的全部声明序列(`callables`、`classifiers`、`constructors` 的合并)。
     *
     * 调用方应按需选择更具体的 `callables` / `classifiers` 入口,
     * 直接使用此属性时会触发底层一次性遍历所有种类。
     */
    val declarations: Sequence<CaDeclarationSymbol>
        get() = withValidityAssertion {
            sequence {
                yieldAll(callables)
                yieldAll(classifiers)
                yieldAll(constructors)
            }
        }

    /**
     * 当前作用域内的全部 [CaCallableSymbol] 序列。
     *
     * 实现需先获取所有可能名字再处理 callable;
     * 已知候选名字集合时,应使用 `Collection<Name>` 版本以减少扫描成本。
     */
    val callables: Sequence<CaCallableSymbol>
        get() = callables { true }

    /**
     * 返回作用域中名字满足 [nameFilter] 的 callable 序列。
     *
     * 实现需先获取所有可能名字再处理 callable;
     * 已知候选名字集合时,应使用 `Collection<Name>` 版本以减少扫描成本。
     */
    fun callables(nameFilter: (Name) -> Boolean): Sequence<CaCallableSymbol>

    /**
     * 返回作用域中名字位于 [names] 中的 callable 序列。
     *
     * 已知候选名字集合时优于 nameFilter 版本,实现可直接定向查询。
     */
    fun callables(names: Collection<Name>): Sequence<CaCallableSymbol>

    /**
     * 返回作用域中名字位于 [names] 中的 callable 序列(vararg 便捷形式)。
     *
     * 已知候选名字集合时优于 nameFilter 版本,实现可直接定向查询。
     */
    fun callables(vararg names: Name): Sequence<CaCallableSymbol> =
        callables(names.toList())

    /**
     * 当前作用域内的全部 [CaClassifierSymbol] 序列。
     *
     * 包含:
     * - 嵌套类、内部类;
     * - 类作用域中的嵌套 type alias;
     * - 文件作用域中的顶级类与顶级 type alias。
     *
     * 实现需先获取所有可能名字再处理 classifier;已知候选名字集合时,应使用 `Collection<Name>` 版本。
     */
    val classifiers: Sequence<CaClassifierSymbol>
        get() = classifiers { true }

    /**
     * 返回作用域中名字满足 [nameFilter] 的 classifier 序列。
     *
     * 包含:
     * - 嵌套类、内部类;
     * - 类作用域中的嵌套 type alias;
     * - 文件作用域中的顶级类与顶级 type alias。
     *
     * 实现需先获取所有可能名字再处理 classifier;已知候选名字集合时,应使用 `Collection<Name>` 版本。
     */
    fun classifiers(nameFilter: (Name) -> Boolean): Sequence<CaClassifierSymbol>

    /**
     * 返回作用域中名字位于 [names] 中的 classifier 序列。
     *
     * 包含:
     * - 嵌套类、内部类;
     * - 类作用域中的嵌套 type alias;
     * - 文件作用域中的顶级类与顶级 type alias。
     *
     * 已知候选名字集合时优于 nameFilter 版本,实现可直接定向查询。
     */
    fun classifiers(names: Collection<Name>): Sequence<CaClassifierSymbol>

    /**
     * 返回作用域中名字位于 [names] 中的 classifier 序列(vararg 便捷形式)。
     *
     * 已知候选名字集合时优于 nameFilter 版本,实现可直接定向查询。
     */
    fun classifiers(vararg names: Name): Sequence<CaClassifierSymbol> =
        classifiers(names.toList())

    /**
     * 当前作用域内的全部 [CaConstructorSymbol] 序列。
     */
    val constructors: Sequence<CaConstructorSymbol>

    /**
     * 返回与 [nameFilter] 匹配、作为当前作用域包直接子包的 [CaPackageSymbol] 序列。
     */
    @CaExperimentalApi
    fun getPackageSymbols(nameFilter: (Name) -> Boolean = { true }): Sequence<CaPackageSymbol>

}
