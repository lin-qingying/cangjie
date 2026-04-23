package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.cfir.*
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirDelegatingNamesAwareScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirFileScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirPackageScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirDeclaredMemberScope
import org.cangnova.cangjie.analysis.api.cfir.symbols.getPublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirBackedSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirClassSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFileSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPackageSymbol
import org.cangnova.cangjie.analysis.api.components.CaScopeProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.toClassLikeSymbol
import org.cangnova.cangjie.cfir.scopes.unsubstitutedScope
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * 作用域组件。
 *
 * 所有 low-level scope 查询都统一通过 session 协议映射为公开 `CaScope`，
 * 不再保留额外的 snapshot 包装协议层。
 */
@OptIn(CaPlatformInterface::class, CaExperimentalApi::class)
internal class CaCfirScopeProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaScopeProvider, CaCfirSessionComponent {
    private fun getScopeSession(): ScopeSession {
        return analysisSession.getScopeSessionFor(analysisSession.cfirSession)
    }
    private fun CaDeclarationContainerSymbol.getCfirForScope(): CfirClassLikeDeclaration = when (this) {
        is CaCfirClassSymbol -> cfirSymbol.cfir
        else -> error(
            "`${this::class.qualifiedName}` needs to be specially handled by the scope provider or is an unknown" +
                    " ${CaDeclarationContainerSymbol::class.simpleName} implementation."
        )
    }

    override val CaDeclarationContainerSymbol.memberScope: CaScope
        get() = withValidityAssertion {
            val cfirScope = getCfirForScope().unsubstitutedScope(
                analysisSession.cfirSession,
                getScopeSession(),
                withForcedTypeCalculator = false,
                memberRequiredPhase = CfirResolvePhase.STATUS,
            )
            CaCfirDelegatingNamesAwareScope(cfirScope, analysisSession.cfirSymbolBuilder)
        }

    override fun CjFile.getFileScope(): CaScope = withValidityAssertion {
        CaCfirFileScope(CaCfirFileSymbol(this@getFileScope, analysisSession), analysisSession.cfirSymbolBuilder)
    }

    override fun getPackageScope(packageFqName: FqName): CaScope? = withValidityAssertion {
        if (!analysisSession.useSitePackageProvider.doesPackageExist(packageFqName)) return@withValidityAssertion null
        CaCfirPackageScope(packageFqName, analysisSession)
    }

    override val CaPackageSymbol.packageScope: CaScope
        get() = withValidityAssertion {
            val packageSymbol = this@packageScope as? CaCfirPackageSymbol
                ?: error("仅 CFIR 包符号支持包级作用域查询：${this@packageScope::class.simpleName}")
            CaCfirPackageScope(packageSymbol.fqName, analysisSession)
        }

    override val CaDeclarationContainerSymbol.combinedDeclaredMemberScope: CaScope
        get() = withValidityAssertion {
            when (this@combinedDeclaredMemberScope) {
                is CaClassLikeSymbol -> (this@combinedDeclaredMemberScope as CaClassLikeSymbol).memberScope
                else -> error("当前仅 class-like 声明容器支持 combinedDeclaredMemberScope：${this@combinedDeclaredMemberScope::class.simpleName}")
            }
        }

    override val CaClassLikeSymbol.declaredMemberScope: CaScope
        get() = withValidityAssertion {
            val classSymbol = requireClassLikeSymbol(this@declaredMemberScope)
            val classDeclaration = classSymbol.backingSymbol.cfir
            val cfirScope = when (classDeclaration) {
                is CfirClass -> analysisSession.cfirSession.cangjieScopeProvider.getDeclarationSiteMemberScope(
                    classDeclaration,
                    analysisSession.cfirSession,
                    getScopeSession(),
                )

                else -> classDeclaration.unsubstitutedScope(
                    analysisSession.cfirSession,
                    getScopeSession(),
                    withForcedTypeCalculator = false,
                    memberRequiredPhase = null,
                )
            }
            CaCfirDeclaredMemberScope(cfirScope, analysisSession.cfirSymbolBuilder)
        }

    override val org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol.declaredMemberScope: CaScope
        get() = withValidityAssertion {
            val extendSymbol = this@declaredMemberScope as? CaCfirExtendSymbol
                ?: error("Only CFIR extend symbols can expose declared-member scope: ${this@declaredMemberScope::class.simpleName}")
            extendSymbol.backingSymbol.cfir.declarations.asDeclarationListScope()
        }

    override val CaClassLikeSymbol.memberScope: CaScope
        get() = withValidityAssertion {
            val classSymbol = requireClassLikeSymbol(this@memberScope)
            val cfirScope = classSymbol.backingSymbol.cfir.unsubstitutedScope(
                analysisSession.cfirSession,
                getScopeSession(),
                withForcedTypeCalculator = false,
                memberRequiredPhase = CfirResolvePhase.STATUS,
            )
            CaCfirDelegatingNamesAwareScope(cfirScope, analysisSession.cfirSymbolBuilder)
        }

    override val org.cangnova.cangjie.analysis.api.types.CaType.scope: CaScope?
        get() = withValidityAssertion {
            val classLikeSymbol = this@scope.requireCfirConeType("成员作用域查询")
                .toClassLikeSymbol(analysisSession.cfirSession)
                ?: return@withValidityAssertion null
            val cfirScope = classLikeSymbol.cfir.unsubstitutedScope(
                analysisSession.cfirSession,
                getScopeSession(),
                withForcedTypeCalculator = false,
                memberRequiredPhase = CfirResolvePhase.STATUS,
            )
            CaCfirDelegatingNamesAwareScope(cfirScope, analysisSession.cfirSymbolBuilder)
        }

    private fun requireClassLikeSymbol(symbol: CaClassLikeSymbol): CaCfirBackedSymbol<CfirClassLikeSymbol<*>> {
        return symbol as? CaCfirBackedSymbol<CfirClassLikeSymbol<*>>
            ?: error("仅 CFIR class-like 符号支持成员作用域查询：${symbol::class.simpleName}")
    }

    private fun List<org.cangnova.cangjie.cfir.declarations.CfirDeclaration>.asDeclarationListScope(): CaScope {
        val publicSymbols = mapNotNull { declaration ->
            when (declaration) {
                is org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration ->
                    analysisSession.getPublicSymbol(declaration.symbol) as? CaDeclarationSymbol
                is org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration ->
                    analysisSession.getPublicSymbol(declaration.symbol) as? CaDeclarationSymbol
                is org.cangnova.cangjie.cfir.declarations.CfirTypeParameter ->
                    analysisSession.getPublicSymbol(declaration.symbol) as? CaDeclarationSymbol
                is org.cangnova.cangjie.cfir.declarations.CfirExtend ->
                    analysisSession.getPublicSymbol(declaration.symbol) as? CaDeclarationSymbol
                else -> null
            }
        }
        val symbolsByName = publicSymbols.groupBy { symbol -> symbol.name ?: Name.special("<anonymous>") }
        return object : CaScope {
            override val token = this@CaCfirScopeProvider.token

            override fun getPossibleCallableNames(): Set<Name> =
                symbolsByName.filterValues { symbols -> symbols.any { it is CaCallableSymbol } }.keys

            override fun getPossibleClassifierNames(): Set<Name> =
                symbolsByName.filterValues { symbols -> symbols.any { it is CaClassifierSymbol } }.keys

            override fun callables(nameFilter: (Name) -> Boolean): Sequence<CaCallableSymbol> =
                symbolsByName.asSequence()
                    .filter { (name, _) -> nameFilter(name) }
                    .flatMap { (_, symbols) -> symbols.asSequence().filterIsInstance<CaCallableSymbol>() }

            override fun callables(names: Collection<Name>): Sequence<CaCallableSymbol> {
                if (names.isEmpty()) return emptySequence()
                val nameSet = names.toSet()
                return callables { it in nameSet }
            }

            override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<CaClassifierSymbol> =
                symbolsByName.asSequence()
                    .filter { (name, _) -> nameFilter(name) }
                    .flatMap { (_, symbols) -> symbols.asSequence().filterIsInstance<CaClassifierSymbol>() }

            override fun classifiers(names: Collection<Name>): Sequence<CaClassifierSymbol> {
                if (names.isEmpty()) return emptySequence()
                val nameSet = names.toSet()
                return classifiers { it in nameSet }
            }

            override val constructors = emptySequence<org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol>()

            @CaExperimentalApi
            override fun getPackageSymbols(nameFilter: (Name) -> Boolean) = emptySequence<org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol>()
        }
    }
}
