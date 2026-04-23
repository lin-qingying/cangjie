package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.Name

abstract class CfirContainingNamesAwareScope : CfirScope() {
    abstract fun getCallableNames(): Set<Name>

    abstract fun getClassifierNames(): Set<Name>

    open val hasDefinitelyNoStaticMembers: Boolean get() = false

    abstract override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirContainingNamesAwareScope?
}

/**
 * A utility [CfirTypeScope] which delegates to [delegate] by default for the purpose of reducing boilerplate code. Inheritors must override
 * at least some functions, or else [delegate] could be used directly.
 *
 * [toString] is not delegated by default because the [delegate] is usually not the same kind of scope as this delegating scope.
 */
abstract class CfirDelegatingTypeScope(private val delegate: CfirTypeScope) : CfirTypeScope() {
    override fun getCallableNames(): Set<Name> = delegate.getCallableNames()
    override fun getClassifierNames(): Set<Name> = delegate.getClassifierNames()
    override fun mayContainName(name: Name): Boolean = delegate.mayContainName(name)
    override val scopeOwnerLookupNames: List<String> get() = delegate.scopeOwnerLookupNames

    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (CfirClassifierSymbol<*>, ConeSubstitutor) -> Unit,
    ) {
        delegate.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processFunctionsByName(
        name: Name,
        processor: (CfirNamedFunctionSymbol) -> Unit,
    ) {
        delegate.processFunctionsByName(name, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        delegate.processPropertiesByName(name, processor)
    }

    override fun processDeclaredConstructors(processor: (CfirConstructorSymbol) -> Unit) {
        delegate.processDeclaredConstructors(processor)
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        return delegate.processDirectOverriddenFunctionsWithBaseScope(functionSymbol, processor)
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        return delegate.processDirectOverriddenPropertiesWithBaseScope(propertySymbol, processor)
    }

       abstract override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirDelegatingTypeScope?
}
