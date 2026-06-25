package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.Name

/**
 * 多个候选类型作用域的并集。
 *
 * 该作用域用于仓颉理想字面量的 primitive extend 查找。官方编译器会按理想类型可落地的
 * primitive 类型逐一查找 extend 成员，并在多个目标命中时报告歧义；这里不能复用交叉类型
 * 的 override 合并规则，否则相同签名的不同 primitive extend 会被提前折叠。
 *
 * @property scopes 参与并集查询的候选类型 scope。
 */
class CfirUnionTypeScope(
    /**
     * 参与并集查询的候选类型 scope 列表。
     */
    private val scopes: List<CfirTypeScope>,
) : CfirTypeScope() {

    /**
     * 在所有子 scope 中处理直接覆盖函数。
     */
    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        for (scope in scopes) {
            if (scope.processDirectOverriddenFunctionsWithBaseScope(functionSymbol, processor) == ProcessorAction.STOP) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    }

    /**
     * 在所有子 scope 中处理直接覆盖属性。
     */
    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        for (scope in scopes) {
            if (scope.processDirectOverriddenPropertiesWithBaseScope(propertySymbol, processor) == ProcessorAction.STOP) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NEXT
    }

    /**
     * 返回所有子 scope callable 名称的并集。
     */
    override fun getCallableNames(): Set<Name> = buildSet {
        scopes.forEach { addAll(it.getCallableNames()) }
    }

    /**
     * 返回所有子 scope classifier 名称的并集。
     */
    override fun getClassifierNames(): Set<Name> = buildSet {
        scopes.forEach { addAll(it.getClassifierNames()) }
    }

    /**
     * 在所有子 scope 中处理 classifier。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        scopes.forEach { it.processClassifiersByName(name, processor) }
    }

    /**
     * 在所有子 scope 中处理函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        scopes.forEach { it.processFunctionsByName(name, processor) }
    }

    /**
     * 在所有子 scope 中处理属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        scopes.forEach { it.processPropertiesByName(name, processor) }
    }

    /**
     * 在所有子 scope 中处理 callable。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        scopes.forEach { it.processCallablesByName(name, processor) }
    }

    /**
     * 替换所有子 scope 的 session。
     */
    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirTypeScope? {
        val replacedScopes = scopes.mapNotNull { it.withReplacedSessionOrNull(newSession, newScopeSession) }
        return if (replacedScopes.size == scopes.size) {
            CfirUnionTypeScope(replacedScopes)
        } else {
            null
        }
    }
}
