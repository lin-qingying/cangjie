package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.utils.cached
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.name.Name

internal open class CaCfirDelegatingNamesAwareScope(
    cfirScope: CfirContainingNamesAwareScope,
    builder: CaSymbolByCfirBuilder,
) : CaCfirBasedScope<CfirContainingNamesAwareScope>(cfirScope, builder) {
    private val allNamesCached by cached {
        getPossibleCallableNames() + getPossibleClassifierNames()
    }

    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion { allNamesCached }

    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        cfirScope.getCallableNames()
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        cfirScope.getClassifierNames()
    }

    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        name in getAllPossibleNames()
    }
}
