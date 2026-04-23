package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.Name

/**
 * 名称解析 scope 接口。
 *
 * scope 用于按名称查找符号，是名称解析的核心抽象。
 * 参考 K2 FirScope。
 */
abstract class CfirScope {
    open fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (CfirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
    }
    open val scopeOwnerLookupNames: List<String> get() = emptyList()

    /** 按名称处理类/接口/结构体/枚举符号 */
    open fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {}
    open fun processDeclaredConstructors(
        processor: (CfirConstructorSymbol) -> Unit
    ) {
    }
    /** 按名称处理函数符号 */
    open fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {}

    open fun processVariablesByName(
        name: Name,
        processor: (CfirVariableSymbol<*>) -> Unit
    ) {
    }
    open fun mayContainName(name: Name): Boolean = true

    /** 按名称处理属性符号 */
    open fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {}
    abstract fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirScope?

    open fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {}
}
