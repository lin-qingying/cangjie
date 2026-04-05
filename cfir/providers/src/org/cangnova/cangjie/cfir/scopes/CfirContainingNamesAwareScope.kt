package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.name.Name

abstract class CfirContainingNamesAwareScope : CfirScope() {
    abstract fun getCallableNames(): Set<Name>

    abstract fun getClassifierNames(): Set<Name>

    open val hasDefinitelyNoStaticMembers: Boolean get() = false

    abstract override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirContainingNamesAwareScope?
}
