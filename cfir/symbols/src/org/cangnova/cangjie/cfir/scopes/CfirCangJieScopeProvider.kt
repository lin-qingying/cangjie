package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.name.FqName

/**
 * Scope provider aligned with Kotlin's FirKotlinScopeProvider responsibilities.
 */
open class CfirCangJieScopeProvider : CfirScopeProvider(), CfirSessionComponent {
    override fun getUseSiteMemberScope(
        klass: CfirClass,
        useSiteSession: CfirSession,
        scopeSession: CfirScopeSession,
    ): CfirClassScope {
        val classSymbol = klass.symbol as? org.cangnova.cangjie.cfir.symbols.CfirClassSymbol ?: return EMPTY_CLASS_SCOPE
        val key = UseSiteMemberScopeKey(classSymbol)
        return scopeSession.getOrBuild(key) {
            CfirClassUseSiteMemberScope(classSymbol, useSiteSession.symbolProvider)
        } as CfirClassScope
    }

    override fun getPackageMemberScope(
        packageFqName: FqName,
        symbolProvider: CfirSymbolProvider,
        useSiteSession: CfirSession,
        scopeSession: CfirScopeSession,
    ): CfirPackageScope {
        val key = PackageMemberScopeKey(packageFqName, symbolProvider)
        return scopeSession.getOrBuild(key) {
            CfirPackageMemberScope(packageFqName, symbolProvider)
        } as CfirPackageScope
    }

    private data class UseSiteMemberScopeKey(
        val classSymbol: org.cangnova.cangjie.cfir.symbols.CfirClassSymbol,
    )

    private data class PackageMemberScopeKey(
        val packageFqName: FqName,
        val symbolProvider: CfirSymbolProvider,
    )

    private companion object {
        val EMPTY_CLASS_SCOPE = object : CfirClassScope {}
    }
}

