package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 包级公开作用域。
 */
@OptIn(CaPlatformInterface::class, CaExperimentalApi::class)
internal class CaCfirPackageScope(
    /**
     * 当前作用域表示的包名。
     */
    private val fqName: FqName,
    /**
     * 当前 CFIR Analysis API 会话。
     */
    private val analysisSession: CaCfirSession,
) : CaScope {
    /**
     * 当前作用域公开对象的生命周期令牌。
     */
    override val token: CaLifetimeToken get() = analysisSession.token

    /**
     * 底层 CFIR 包成员作用域。
     */
    private val firScope: CfirPackageMemberScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CfirPackageMemberScope(fqName, analysisSession.cfirSession)
    }

    /**
     * 返回当前包中的顶层 callable 名称。
     */
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        analysisSession.useSiteScopeDeclarationProvider.getTopLevelCallableNamesInPackage(fqName)
    }

    /**
     * 返回当前包中的顶层 class-like 名称。
     */
    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        analysisSession.useSiteScopeDeclarationProvider.getTopLevelCangJieClassLikeDeclarationNamesInPackage(fqName)
    }

    /**
     * 按名称过滤器查询包内 callable 符号。
     */
    override fun callables(nameFilter: (Name) -> Boolean): Sequence<CaCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(getPossibleCallableNames().filter(nameFilter), analysisSession.cfirSymbolBuilder)
    }

    /**
     * 按名称集合查询包内 callable 符号。
     */
    override fun callables(names: Collection<Name>): Sequence<CaCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(names, analysisSession.cfirSymbolBuilder)
    }

    /**
     * 按名称过滤器查询包内 classifier 符号。
     */
    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<CaClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), analysisSession.cfirSymbolBuilder)
    }

    /**
     * 按名称集合查询包内 classifier 符号。
     */
    override fun classifiers(names: Collection<Name>): Sequence<CaClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(names, analysisSession.cfirSymbolBuilder)
    }

    /**
     * 包作用域不直接提供构造器集合。
     */
    override val constructors: Sequence<CaConstructorSymbol>
        get() = withValidityAssertion { emptySequence() }

    /**
     * 查询当前包的直接子包符号。
     */
    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<CaPackageSymbol> = withValidityAssertion {
        sequence {
            analysisSession.useSitePackageProvider.getSubpackageNames(fqName).forEach { name ->
                if (nameFilter(name)) {
                    yield(analysisSession.cfirSymbolBuilder.createPackageSymbol(fqName.child(name)))
                }
            }
        }
    }
}
