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
     * A sequence of all [CaDeclarationSymbol]s contained in the scope.
     */
    public val declarations: Sequence<CaDeclarationSymbol>
        get() = withValidityAssertion {
            sequence {
                yieldAll(callables)
                yieldAll(classifiers)
                yieldAll(constructors)
            }
        }

    /**
     * A sequence of [CaCallableSymbol]s contained in the scope.
     *
     * The implementation of this property needs to retrieve a set of all possible names before processing callables. The overload with
     * `Collection<Name>` should be used when the candidate name set is known.
     */
    public val callables: Sequence<CaCallableSymbol>
        get() = callables { true }

    /**
     * Returns a sequence of [CaCallableSymbol]s contained in the scope which match the [nameFilter].
     *
     * The implementation of this function needs to retrieve a set of all possible names before processing callables. The overload with
     * `Collection<Name>` should be used when the candidate name set is known.
     */
    public fun callables(nameFilter: (Name) -> Boolean): Sequence<CaCallableSymbol>

    /**
     * Returns a sequence of [CaCallableSymbol]s contained in the scope which match the given [names].
     *
     * The implementation of this function is optimized compared to using a name filter and should be used when the candidate name set is
     * known.
     */
    public fun callables(names: Collection<Name>): Sequence<CaCallableSymbol>

    /**
     * Returns a sequence of [CaCallableSymbol]s contained in the scope which match the given [names].
     *
     * The implementation of this function is optimized compared to using a name filter and should be used when the candidate name set is
     * known.
     */
    public fun callables(vararg names: Name): Sequence<CaCallableSymbol> =
        callables(names.toList())

    /**
     * A sequence of [CaClassifierSymbol]s contained in the scope.
     *
     * The result includes:
     *
     * - Nested classes
     * - Inner classes
     * - Nested type aliases for a class scope
     * - Top-level classes and top-level type aliases for a file scope
     *
     * The implementation of this property needs to retrieve a set of all possible names before processing classifiers. The overload with
     * `Collection<Name>` should be used when the candidate name set is known.
     */
    public val classifiers: Sequence<CaClassifierSymbol>
        get() = classifiers { true }

    /**
     * Returns a sequence of [CaClassifierSymbol]s contained in the scope which match the [nameFilter].
     *
     * The result includes:
     *
     * - Nested classes
     * - Inner classes
     * - Nested type aliases for a class scope
     * - Top-level classes and top-level type aliases for a file scope
     *
     * The implementation of this function needs to retrieve a set of all possible names before processing classifiers. The overload with
     * `Collection<Name>` should be used when the candidate name set is known.
     */
    public fun classifiers(nameFilter: (Name) -> Boolean): Sequence<CaClassifierSymbol>

    /**
     * Returns a sequence of [CaClassifierSymbol]s contained in the scope which match the given [names].
     *
     * The result includes:
     *
     * - Nested classes
     * - Inner classes
     * - Nested type aliases for a class scope
     * - Top-level classes and top-level type aliases for a file scope
     *
     * The implementation of this function is optimized compared to using a name filter and should be used when the candidate name set is
     * known.
     */
    public fun classifiers(names: Collection<Name>): Sequence<CaClassifierSymbol>

    /**
     * Returns a sequence of [CaClassifierSymbol]s contained in the scope which match the given [names].
     *
     * The result includes:
     *
     * - Nested classes
     * - Inner classes
     * - Nested type aliases for a class scope
     * - Top-level classes and top-level type aliases for a file scope
     *
     * The implementation of this function is optimized compared to using a name filter and should be used when the candidate name set is
     * known.
     */
    public fun classifiers(vararg names: Name): Sequence<CaClassifierSymbol> =
        classifiers(names.toList())

    /**
     * A sequence of [CaConstructorSymbol] contained in the scope.
     */
    public val constructors: Sequence<CaConstructorSymbol>

    /**
     * Returns a sequence of [CaPackageSymbol]s matching [nameFilter] which are a direct subpackage of the scope's package.
     */
    @CaExperimentalApi
    public fun getPackageSymbols(nameFilter: (Name) -> Boolean = { true }): Sequence<CaPackageSymbol>

}
