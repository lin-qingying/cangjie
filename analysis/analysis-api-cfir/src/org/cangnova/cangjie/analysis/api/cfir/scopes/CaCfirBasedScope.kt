package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
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
    internal val cfirScope: S,
    protected val builder: CaSymbolByCfirBuilder,
) : CaScope {
    final override val token: CaLifetimeToken get() = builder.token




    override fun callables(nameFilter: (Name) -> Boolean): Sequence<CaCallableSymbol> = withValidityAssertion {
        cfirScope.getCallableSymbols(getPossibleCallableNames().filter(nameFilter), builder)
    }

    override fun callables(names: Collection<Name>): Sequence<CaCallableSymbol> = withValidityAssertion {
        cfirScope.getCallableSymbols(names, builder)
    }

    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<CaClassifierSymbol> = withValidityAssertion {
        cfirScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), builder)
    }

    override fun classifiers(names: Collection<Name>): Sequence<CaClassifierSymbol> = withValidityAssertion {
        cfirScope.getClassifierSymbols(names, builder)
    }

    override val constructors: Sequence<CaConstructorSymbol>
        get() = withValidityAssertion {
            cfirScope.getConstructors(builder)
        }

    @CaExperimentalApi
    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<CaPackageSymbol> = withValidityAssertion {
        emptySequence()
    }
}

private fun CfirBasedSymbol<*>.scopeIdentity(): String = when (this) {
    is CfirClassLikeSymbol<*> -> "class:${classId.asString()}"
    is CfirCallableSymbol<*> -> "callable:${callableId?.toString() ?: name.asString()}"
    is CfirFileSymbol -> "file:${cfir.name}"
    else -> "${this::class.qualifiedName}:$debugName"
}
