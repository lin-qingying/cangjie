

package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangnova.cangjie.cfir.analysis.checkers.CfirCheckerWithDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind

/*
 * 鏈枃浠剁敱鐢熸垚鍣ㄨ嚜鍔ㄧ敓鎴? * 璇峰嬁鎵嬪姩淇敼
 */

class ComposedCfirTypeCheckers(val predicate: (CfirCheckerWithDispatchKind) -> Boolean) : CfirTypeCheckers() {
    constructor(dispatchKind: CheckerDispatchKind) : this({ it.dispatchKind == dispatchKind })

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

