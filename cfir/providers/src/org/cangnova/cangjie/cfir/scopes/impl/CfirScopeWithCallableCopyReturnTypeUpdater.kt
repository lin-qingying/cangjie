package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.canHaveDeferredReturnTypeCalculation
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.scopes.CallableCopyTypeCalculator
import org.cangnova.cangjie.cfir.scopes.CfirDelegatingTypeScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.Name

/**
 * This scope is a wrapper which is intended to use with scopes that can create callable copies.
 *
 * The main purpose of this scope is to update dispatched callables return types
 * in case it is not yet calculated due to implicit body resolve logic.
 */
class CfirScopeWithCallableCopyReturnTypeUpdater(
    private val delegate: CfirTypeScope,
    private val callableCopyTypeCalculator: CallableCopyTypeCalculator
) : CfirDelegatingTypeScope(delegate) {
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        delegate.processFunctionsByName(name) {
            updateReturnType(it.cfir)
            processor(it)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        delegate.processPropertiesByName(name) {
            updateReturnType(it.cfir)
            processor(it)
        }
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return delegate.processDirectOverriddenFunctionsWithBaseScope(functionSymbol) { symbol, scope ->
            updateReturnType(symbol.cfir)
            processor(symbol, scope)
        }
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return delegate.processDirectOverriddenPropertiesWithBaseScope(propertySymbol) { symbol, scope ->
            updateReturnType(symbol.cfir)
            processor(symbol, scope)
        }
    }

    private fun updateReturnType(declaration: CfirCallableDeclaration) {
        if (declaration.canHaveDeferredReturnTypeCalculation) {
            callableCopyTypeCalculator.computeReturnType(declaration)
        }
    }

    override fun toString(): String {
        return delegate.toString()
    }

    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession
    ): CfirScopeWithCallableCopyReturnTypeUpdater? {
        return delegate.withReplacedSessionOrNull(newSession, newScopeSession)?.let {
            CfirScopeWithCallableCopyReturnTypeUpdater(delegate, callableCopyTypeCalculator)
        }
    }
}
