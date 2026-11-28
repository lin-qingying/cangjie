package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.name.Name

/**
 * 空作用域。
 *
 * 类型形态借用 Kotlin `KaFirEmptyScope`（`analysis-api-fir/src/.../scopes/KaFirEmptyScope.kt`），
 * 但**用法是我方偏离**：Kotlin 只在「委托字段为空 / 插件没有提供者」时返回它，
 * 从不用它表达「符号缺失」——因为 FIR 每个声明必有符号。这里用于「CFIR 符号不可恢复」，
 * 与官方 `InvalidTy` 语义下的空继承/空成员集合一致
 * （`external/cangjie_compiler/src/Sema/TypeManager.cpp:1795-1800` 的错误类型直接返回空表），
 * 并且错误类型节点上的诊断会被抑制（`src/Sema/TypeCheckUtil.cpp:276-279`），不会级联出假错。
 *
 * 与之配套的判定在 [CaCfirScopeProvider]：只有「符号不可恢复」才返回本作用域；
 * 符号存在但作用域构建失败仍然是缺陷，必须暴露。
 */
internal class CaCfirEmptyScope(
    /**
     * 约束本作用域生命周期的会话令牌。
     */
    override val token: CaLifetimeToken,
) : CaScope {
    /**
     * 空作用域没有任何可能的名字。
     */
    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion { emptySet() }

    /**
     * 空作用域没有可能的 callable 名字。
     */
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion { emptySet() }

    /**
     * 空作用域没有可能的 classifier 名字。
     */
    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion { emptySet() }

    /**
     * 空作用域不包含任何 callable。
     */
    override fun callables(nameFilter: (Name) -> Boolean): Sequence<CaCallableSymbol> = withValidityAssertion { emptySequence() }

    /**
     * 空作用域不包含任何 callable。
     */
    override fun callables(names: Collection<Name>): Sequence<CaCallableSymbol> = withValidityAssertion { emptySequence() }

    /**
     * 空作用域不包含任何 classifier。
     */
    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<CaClassifierSymbol> = withValidityAssertion { emptySequence() }

    /**
     * 空作用域不包含任何 classifier。
     */
    override fun classifiers(names: Collection<Name>): Sequence<CaClassifierSymbol> = withValidityAssertion { emptySequence() }

    /**
     * 空作用域不包含任何类型参数。
     */
    override fun typeParameters(nameFilter: (Name) -> Boolean): Sequence<CaTypeParameterSymbol> = withValidityAssertion { emptySequence() }

    /**
     * 空作用域不包含任何构造器。
     */
    override val constructors: Sequence<CaConstructorSymbol>
        get() = withValidityAssertion { emptySequence() }

    /**
     * 空作用域不包含任何直接子包。
     */
    @CaExperimentalApi
    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<CaPackageSymbol> = withValidityAssertion { emptySequence() }

    /**
     * 空作用域不包含任何名字。
     */
    override fun mayContainName(name: Name): Boolean = withValidityAssertion { false }
}
