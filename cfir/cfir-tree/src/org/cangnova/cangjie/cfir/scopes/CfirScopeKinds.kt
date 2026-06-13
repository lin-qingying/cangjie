package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.name.Name

/** 包级 scope，解析包内的顶级声明 */
abstract class CfirPackageScope : CfirContainingNamesAwareScope() {
    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirPackageScope? = null
}

/** 类级 scope，解析类内部的成员声明 */
abstract class CfirClassScope : CfirContainingNamesAwareScope() {
    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession,
    ): CfirContainingNamesAwareScope? = null
}

/** import scope，解析通过 import 引入的声明 */
abstract class CfirImportScope : CfirScope() {
    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirScope? = null
}


/** extend scope，解析 extend 声明引入的成员 */
abstract class CfirExtendScope : CfirScope() {
    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirScope? = null
}

/** 类型参数 scope，解析泛型类/函数中的类型参数名称 */
abstract class CfirTypeParameterScope : CfirScope() {
    /** 按名称处理类型参数符号 */
    open fun processTypeParametersByName(name: Name, processor: (CfirTypeParameterSymbol) -> Unit) {}

    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirScope? = null
}

/**
 * 组合 scope，将多个 scope 合并为一个。
 */
class CfirCompositeScope(private val scopes: List<CfirScope>) : CfirScope() {

    constructor(vararg scopes: CfirScope) : this(scopes.toList())

    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        for (scope in scopes) scope.processClassifiersByName(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        for (scope in scopes) scope.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        for (scope in scopes) scope.processPropertiesByName(name, processor)
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        for (scope in scopes) scope.processCallablesByName(name, processor)
    }

    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirScope? {
        val newScopes = scopes.mapNotNull { it.withReplacedSessionOrNull(newSession, newScopeSession) }
        return if (newScopes.size == scopes.size) CfirCompositeScope(newScopes) else null
    }
}
