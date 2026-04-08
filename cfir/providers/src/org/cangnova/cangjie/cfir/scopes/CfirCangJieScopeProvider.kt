package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.ScopeSessionKey
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.name.FqName

/**
 * 仓颉语言的 scope 提供者，对标 K2 FirKotlinScopeProvider。
 */
open class CfirCangJieScopeProvider : CfirScopeProvider(), CfirSessionComponent {
    override fun getUseSiteMemberScope(
        klass: CfirClass,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirTypeScope {
        val classSymbol = klass.symbol as? CfirClassSymbol ?: return CfirTypeScope.Empty
        return scopeSession.getOrBuild(useSiteSession to classSymbol, USE_SITE) {
            val rawScope = CfirClassUseSiteMemberScope(
                session = useSiteSession,
                classSymbol = classSymbol,
                symbolProvider = useSiteSession.symbolProvider,
                extendProvider = useSiteSession.extendProvider,
                directSupertypeProvider = useSiteSession.directSupertypeProviderOrNull,
                scopeKind = CfirClassMemberScopeKind.USE_SITE,
            )
            CfirClassSubstitutionScope(useSiteSession, rawScope, classSymbol.constructType())
        }
    }

    override fun getDeclarationSiteMemberScope(
        klass: CfirClass,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirTypeScope {
        val classSymbol = klass.symbol as? CfirClassSymbol ?: return CfirTypeScope.Empty
        return CfirClassUseSiteMemberScope(
            session = useSiteSession,
            classSymbol = classSymbol,
            symbolProvider = useSiteSession.symbolProvider,
            extendProvider = useSiteSession.extendProvider,
            directSupertypeProvider = useSiteSession.directSupertypeProviderOrNull,
            scopeKind = CfirClassMemberScopeKind.DECLARATION_SITE,
        )
    }

    override fun getPackageMemberScope(
        packageFqName: FqName,
        symbolProvider: CfirSymbolProvider,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirPackageScope {
        val key: ScopeSessionKey<PackageMemberScopeKey, CfirPackageMemberScope> = scopeSessionKey()
        return scopeSession.getOrBuild(PackageMemberScopeKey(packageFqName, symbolProvider), key) {
            CfirPackageMemberScope(packageFqName, symbolProvider)
        }
    }

    private data class PackageMemberScopeKey(
        val packageFqName: FqName,
        val symbolProvider: CfirSymbolProvider,
    )
}
