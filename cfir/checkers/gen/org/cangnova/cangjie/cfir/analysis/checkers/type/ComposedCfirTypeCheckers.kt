

package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangnova.cangjie.cfir.analysis.checkers.CfirCheckerWithMppKind
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class ComposedCfirTypeCheckers(val predicate: (CfirCheckerWithMppKind) -> Boolean) : CfirTypeCheckers() {
    constructor(mppKind: MppCheckerKind) : this({ it.mppKind == mppKind })

    override val typeRefCheckers: Set<CfirTypeRefChecker>
        get() = _typeRefCheckers
    override val resolvedTypeRefCheckers: Set<CfirResolvedTypeRefChecker>
        get() = _resolvedTypeRefCheckers

    private val _typeRefCheckers: MutableSet<CfirTypeRefChecker> = mutableSetOf()
    private val _resolvedTypeRefCheckers: MutableSet<CfirResolvedTypeRefChecker> = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: CfirTypeCheckers) {
        checkers.typeRefCheckers.filterTo(_typeRefCheckers, predicate)
        checkers.resolvedTypeRefCheckers.filterTo(_resolvedTypeRefCheckers, predicate)
    }
}
