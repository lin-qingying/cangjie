package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.Name

abstract class CfirTypeScope : CfirContainingNamesAwareScope() {
    // If the scope instance is the same as the one from which the symbol was originated, this function supplies
    // all direct overridden members (each of them comes a base scope where grand-parents [overridden of this overridden] may be obtained from)
    //
    // For example:
    //
    // interface A { fun foo() }
    // interface B : A { override fun foo() }
    // interface C : B { override fun foo() }
    //
    // Here, for override C::foo from a scope of C, processor will receiver B::foo and scope for B
    // Then, for B::foo from scope for B one may receive override A::foo and scope for A
    //
    // Currently, this function and its property brother both have very weak guarantees
    // - It may return the same overridden symbols more than once in case of substitution or intersection
    //     (but with different base scope)
    abstract fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirFunctionSymbol<*>,
        processor: (CfirFunctionSymbol<*>, CfirTypeScope) -> ProcessorAction
    ): ProcessorAction

    // ------------------------------------------------------------------------------------

    abstract fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction
    ): ProcessorAction

    abstract override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession
    ): CfirTypeScope?

    // ------------------------------------------------------------------------------------

    object Empty : CfirTypeScope() {
        override fun processDirectOverriddenFunctionsWithBaseScope(
            functionSymbol: CfirFunctionSymbol<*>,
            processor: (CfirFunctionSymbol<*>, CfirTypeScope) -> ProcessorAction
        ): ProcessorAction = ProcessorAction.NEXT

        override fun processDirectOverriddenPropertiesWithBaseScope(
            propertySymbol: CfirPropertySymbol,
            processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction
        ): ProcessorAction = ProcessorAction.NEXT

        override fun getCallableNames(): Set<Name> = emptySet()

        override fun getClassifierNames(): Set<Name> = emptySet()

        override fun toString(): String {
            return "Empty scope"
        }


        override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirTypeScope? {
            return null
        }
    }
}
