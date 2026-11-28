package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.name.Name

/**
 * 对齐 Kotlin `CaFirBasedScope` 的公开作用域底座。
 *
 * 这里直接包装真实的 `CfirContainingNamesAwareScope`，Analysis API 侧只负责：
 * 1. 把底层 CFIR symbol 映射为公开 `CaSymbol`
 * 2. 在 file/package 等场景补充宿主 eager symbol
 * 3. 统一做按名称查询与去重
 */
internal abstract class CaCfirBasedScope<S : CfirScope>(
    /**
     * 被包装的底层 CFIR 作用域。
     */
    internal val cfirScope: S,
    /**
     * 用于将底层符号构造为公开 Analysis API 符号的 builder。
     */
    protected val builder: CaSymbolByCfirBuilder,
) : CaScope {
    /**
     * 当前作用域公开对象的生命周期令牌。
     */
    final override val token: CaLifetimeToken get() = builder.token




    /**
     * 按名称过滤器查询 callable 符号序列。
     */
    override fun callables(nameFilter: (Name) -> Boolean): Sequence<CaCallableSymbol> = withValidityAssertion {
        cfirScope.getCallableSymbols(getPossibleCallableNames().filter(nameFilter), builder)
    }

    /**
     * 按名称集合查询 callable 符号序列。
     */
    override fun callables(names: Collection<Name>): Sequence<CaCallableSymbol> = withValidityAssertion {
        cfirScope.getCallableSymbols(names, builder)
    }

    /**
     * 按名称过滤器查询 classifier 符号序列。
     */
    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<CaClassifierSymbol> = withValidityAssertion {
        cfirScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), builder)
    }

    /**
     * 按名称集合查询 classifier 符号序列。
     */
    override fun classifiers(names: Collection<Name>): Sequence<CaClassifierSymbol> = withValidityAssertion {
        cfirScope.getClassifierSymbols(names, builder)
    }

    /**
     * 按名称过滤器查询类型参数序列。
     *
     * 名字来源用 [getPossibleClassifierNames]：类型参数作用域的 `getCallableNames()` 是空集，
     * 而 `getClassifierNames()` 报告的正是类型参数名（尽管 `processClassifiersByName`
     * 是空实现）。**名字来源与符号来源走两条不同通道**是这个 scope 的既有形状。
     */
    override fun typeParameters(nameFilter: (Name) -> Boolean): Sequence<CaTypeParameterSymbol> =
        withValidityAssertion {
            cfirScope.getTypeParameterSymbols(getPossibleClassifierNames().filter(nameFilter), builder)
        }

    /**
     * 当前作用域声明的构造器符号序列。
     */
    override val constructors: Sequence<CaConstructorSymbol>
        get() = withValidityAssertion {
            cfirScope.getConstructors(builder)
        }

    /**
     * 基础 CFIR scope 不直接暴露包符号。
     */
    @CaExperimentalApi
    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<CaPackageSymbol> = withValidityAssertion {
        emptySequence()
    }
}

/**
 * 计算底层 CFIR 符号在作用域去重场景中的稳定身份文本。
 */
private fun CfirBasedSymbol<*>.scopeIdentity(): String = when (this) {
    is CfirClassLikeSymbol<*> -> "class:${classId.asString()}"
    is CfirCallableSymbol<*> -> "callable:${callableId?.toString() ?: name.asString()}"
    is CfirFileSymbol -> "file:${cfir.name}"
    else -> "${this::class.qualifiedName}:$debugName"
}
