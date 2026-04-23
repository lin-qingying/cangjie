package org.cangnova.cangjie.analysis.api.scopes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.name.Name

@CaExperimentalApi
@SubclassOptInRequired(CaImplementationDetail::class)
public interface CaScopeLike : CaLifetimeOwner {
    /**
     * Returns the set of top-level declaration names contained in the scope. The set may contain false positives, i.e. names which aren't
     * contained in the scope.
     */
    public fun getAllPossibleNames(): Set<Name> = withValidityAssertion {
        getPossibleCallableNames() + getPossibleClassifierNames()
    }

    /**
     * Returns the set of top-level callable names contained in the scope. The set may contain false positives, i.e. names which aren't
     * contained in the scope.
     */
    public fun getPossibleCallableNames(): Set<Name>

    /**
     * Returns the set of top-level classifier names contained in the scope. The set may contain false positives, i.e. names which aren't
     * contained in the scope.
     */
    public fun getPossibleClassifierNames(): Set<Name>

    /**
     * Checks whether the scope *might* contain the given [name].
     *
     * Since [getPossibleCallableNames] and [getPossibleClassifierNames] admit false positives, the result may be `true` even if the scope
     * doesn't contain a declaration with such a name. The reverse is not so: when [mayContainName] is `false`, the scope definitely doesn't
     * contain a declaration with that name.
     */
    public fun mayContainName(name: Name): Boolean = withValidityAssertion {
        name in getPossibleCallableNames() || name in getPossibleClassifierNames()
    }
}
