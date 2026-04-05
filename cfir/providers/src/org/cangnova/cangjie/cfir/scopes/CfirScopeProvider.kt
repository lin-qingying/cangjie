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

    /**
     * 声明检查使用的成员 scope。
     *
     * 这里只反映类本体源码显式声明的继承关系，不注入 extend 引入的父接口/成员，
     * 防止 declaration checker 把“外部扩展语义”误判成“类本体义务”。
     */
    abstract fun getDeclarationSiteMemberScope(
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
