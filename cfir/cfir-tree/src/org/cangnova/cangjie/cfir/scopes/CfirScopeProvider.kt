package org.cangnova.cangjie.cfir.scopes

abstract class CfirScopeProvider {
    abstract fun getUseSiteMemberScope(
        klass: org.cangnova.cangjie.cfir.declarations.CfirClass,
        useSiteSession: org.cangnova.cangjie.cfir.session.CfirSession,
        scopeSession: org.cangnova.cangjie.cfir.ScopeSession,
    ): CfirTypeScope

    /**
     * 声明检查使用的成员 scope。
     *
     * 这里只反映类本体源码显式声明的继承关系，不注入 extend 引入的父接口/成员，
     * 防止 declaration checker 把“外部扩展语义”误判成“类本体义务”。
     */
    abstract fun getDeclarationSiteMemberScope(
        klass: org.cangnova.cangjie.cfir.declarations.CfirClass,
        useSiteSession: org.cangnova.cangjie.cfir.session.CfirSession,
        scopeSession: org.cangnova.cangjie.cfir.ScopeSession,
    ): CfirTypeScope


}