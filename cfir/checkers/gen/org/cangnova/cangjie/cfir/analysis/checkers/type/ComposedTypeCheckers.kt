

package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangnova.cangjie.cfir.analysis.checkers.CfirCheckerWithMppKind
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

class ComposedTypeCheckers(val predicate: (CfirCheckerWithMppKind) -> Boolean) : TypeCheckers() {
    constructor(mppKind: MppCheckerKind) : this({ it.mppKind == mppKind })

    override val typeRefCheckers: Set<CfirTypeRefChecker>
        get() = _typeRefCheckers
    override val resolvedTypeRefCheckers: Set<CfirResolvedTypeRefChecker>
        get() = _resolvedTypeRefCheckers

    private val _typeRefCheckers: MutableSet<CfirTypeRefChecker> = mutableSetOf()
    private val _resolvedTypeRefCheckers: MutableSet<CfirResolvedTypeRefChecker> = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: TypeCheckers) {
        checkers.typeRefCheckers.filterTo(_typeRefCheckers, predicate)
        checkers.resolvedTypeRefCheckers.filterTo(_resolvedTypeRefCheckers, predicate)
    }
}
