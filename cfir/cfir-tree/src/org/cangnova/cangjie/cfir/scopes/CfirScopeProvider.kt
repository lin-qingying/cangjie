package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 类成员 scope 的提供者。
 *
 * scope provider 把 class-like 声明、use-site session 和 [ScopeSession] 组合成可查询成员 scope。
 * use-site scope、declaration-site scope 与 typealias 构造器 scope 的边界必须在这里保持清晰，
 * 避免扩展成员、继承成员和类型别名构造语义互相污染。
 */
abstract class CfirScopeProvider {
    /**
     * 返回 use-site 成员 scope。
     *
     * use-site scope 用于调用解析和普通成员查询，允许包含从继承、扩展或替换规则引入的可见成员。
     */
    abstract fun getUseSiteMemberScope(
        klass: CfirClass,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirTypeScope

    /**
     * typealias 构造器 scope。
     *
     * 对齐 Kotlin FIR `FirScopeProvider.getTypealiasConstructorScope`：构造调用命中
     * typealias 名称时，不直接绕到展开类，而是通过一个合成构造器 scope 保留
     * typealias 自身类型参数、source 与诊断上下文。
     */
    abstract fun getTypealiasConstructorScope(
        typeAlias: CfirTypeAlias,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirScope

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


}
