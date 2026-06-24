package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.Name

/**
 * 组合多个类型作用域，用于类型参数多上界和交叉类型的成员解析。
 */
class CfirCompositeTypeScope(
    /**
     * 被组合的类型 scope 列表。
     */
    private val scopes: List<CfirTypeScope>,
) : CfirTypeScope() {

    /**
     * 通过 vararg 创建组合 scope。
     */
    constructor(vararg scopes: CfirTypeScope) : this(scopes.toList())

    /**
     * 在所有子 scope 中处理直接覆盖函数。
     */
    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol:CfirNamedFunctionSymbol,
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
     * 返回 callable 名称并集。
     */
    override fun getCallableNames(): Set<Name> = buildSet {
        scopes.forEach { addAll(it.getCallableNames()) }
    }

    /**
     * 返回 classifier 名称并集。
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
     * 在所有子 scope 中处理函数，并按 override 签名去重。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        val mergedBySignature = linkedMapOf<String, CfirNamedFunctionSymbol>()
        scopes.forEach { scope ->
            scope.processFunctionsByName(name) { symbol ->
                val signature = symbol.overrideSignatureKey()
                val previous = mergedBySignature[signature]
                if (previous == null || previous.shouldBeReplacedBy(symbol)) {
                    mergedBySignature[signature] = symbol
                }
            }
        }
        mergedBySignature.values.forEach(processor)
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
     * 替换所有子 scope 的 session 后重建组合 scope。
     */
    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirTypeScope? {
        val replacedScopes = scopes.mapNotNull { it.withReplacedSessionOrNull(newSession, newScopeSession) }
        return if (replacedScopes.size == scopes.size) {
            CfirCompositeTypeScope(replacedScopes)
        } else {
            null
        }
    }

    /**
     * 判断当前函数是否应被候选函数替换。
     */
    private fun CfirNamedFunctionSymbol.shouldBeReplacedBy(candidate: CfirNamedFunctionSymbol): Boolean {
        if (!isBound || !candidate.isBound) return false
        val currentFunction = cfir
        val candidateFunction = candidate.cfir
        return currentFunction.status.isAbstract && !candidateFunction.status.isAbstract
    }
}
