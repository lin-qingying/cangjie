package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.FqName

abstract class CfirScopeProvider {
    abstract fun getUseSiteMemberScope(
        klass: CfirClass,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirTypeScope

    abstract fun getPackageMemberScope(
        packageFqName: FqName,
        symbolProvider: CfirSymbolProvider,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirPackageScope
}
