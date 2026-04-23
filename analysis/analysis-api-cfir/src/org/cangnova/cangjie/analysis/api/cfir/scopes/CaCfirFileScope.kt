package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFileSymbol
import org.cangnova.cangjie.analysis.api.cfir.utils.cached
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.name.Name

/**
 * 文件级公开作用域。
 *
 * 当前仓颉文件作用域需要同时体现：
 * 1. 当前文件自身的顶层声明
 * 2. 同 package 中可直接可见的顶层声明
 * 因此这里组合“文件声明 scope + package scope”，但两者都是真实 CFIR scope。
 */
internal class CaCfirFileScope(
    private val owner: CaCfirFileSymbol,
    private val builder: CaSymbolByCfirBuilder
) : CaScope {
    override val token: CaLifetimeToken get() = builder.token
    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion { allNamesCached }
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion { backingCallableNames }
    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion { _classifierNames }


    private val backingCallableNames: Set<Name> by cached {
        val result = mutableSetOf<Name>()
        owner.cfirSymbol.cfir.declarations
            .mapNotNullTo(result) { firDeclaration ->
                when (firDeclaration) {
                    is CfirNamedFunction -> firDeclaration.name
                    is CfirProperty -> firDeclaration.name
                    else -> null
                }
            }

        result
    }
    private val _classifierNames: Set<Name> by cached {
        val result = mutableSetOf<Name>()
        owner.cfirSymbol.cfir.declarations
            .mapNotNullTo(result) { firDeclaration ->
                (firDeclaration as? CfirClassLikeDeclaration)?.name
            }

        result
    }
    override fun callables(nameFilter: (Name) -> Boolean): Sequence<CaCallableSymbol> = withValidityAssertion {
        sequence {
            owner.cfirSymbol.cfir.declarations.forEach { firDeclaration ->
                val callableDeclaration = when (firDeclaration) {
                    is CfirNamedFunction -> firDeclaration.takeIf { nameFilter(firDeclaration.name) }
                    is CfirProperty -> firDeclaration.takeIf { nameFilter(firDeclaration.name) }
                    else -> null
                }

                if (callableDeclaration != null) {
                    yield(builder.callableBuilder.buildCallableSymbol(callableDeclaration.symbol))
                }
            }
        }
    }
    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<CaClassifierSymbol> = withValidityAssertion {
        sequence {
            owner.cfirSymbol.cfir.declarations.forEach { firDeclaration ->
                val classLikeDeclaration = when (firDeclaration) {
                    is CfirTypeAlias -> firDeclaration.takeIf { nameFilter(it.name) }
                    is CfirClassLikeDeclaration -> firDeclaration.takeIf { nameFilter(it.name) }
                    else -> null
                }
                if (classLikeDeclaration != null) {
                    yield(builder.classifierBuilder.buildClassLikeSymbol(classLikeDeclaration.symbol))
                }
            }
        }
    }

    override fun classifiers(names: Collection<Name>): Sequence<CaClassifierSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return classifiers { it in namesSet }
    }

    override val constructors: Sequence<CaConstructorSymbol>
        get() = withValidityAssertion { emptySequence() }

    @CaExperimentalApi
    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<CaPackageSymbol> = withValidityAssertion {
        emptySequence()
    }
    override fun callables(names: Collection<Name>): Sequence<CaCallableSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return callables { it in namesSet }
    }

    private val allNamesCached by cached {
        backingCallableNames + _classifierNames
    }

}
