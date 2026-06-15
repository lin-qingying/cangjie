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
    private val scopes: List<CfirTypeScope>,
) : CfirTypeScope() {

    constructor(vararg scopes: CfirTypeScope) : this(scopes.toList())

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

    override fun getCallableNames(): Set<Name> = buildSet {
        scopes.forEach { addAll(it.getCallableNames()) }
    }

    override fun getClassifierNames(): Set<Name> = buildSet {
        scopes.forEach { addAll(it.getClassifierNames()) }
    }

    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        scopes.forEach { it.processClassifiersByName(name, processor) }
    }

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

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        scopes.forEach { it.processPropertiesByName(name, processor) }
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        scopes.forEach { it.processCallablesByName(name, processor) }
    }

    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirTypeScope? {
        val replacedScopes = scopes.mapNotNull { it.withReplacedSessionOrNull(newSession, newScopeSession) }
        return if (replacedScopes.size == scopes.size) {
            CfirCompositeTypeScope(replacedScopes)
        } else {
            null
        }
    }

    private fun CfirNamedFunctionSymbol.shouldBeReplacedBy(candidate: CfirNamedFunctionSymbol): Boolean {
        if (!isBound || !candidate.isBound) return false
        val currentFunction = cfir
        val candidateFunction = candidate.cfir
        return currentFunction.status.isAbstract && !candidateFunction.status.isAbstract
    }
}
