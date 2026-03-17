

package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal

/*
 * 鏈枃浠剁敱鐢熸垚鍣ㄨ嚜鍔ㄧ敓鎴? * 璇峰嬁鎵嬪姩淇敼
 */

@Suppress("UNCHECKED_CAST")
abstract class CfirTypeCheckers {
    companion object {
        val EMPTY: CfirTypeCheckers = object : CfirTypeCheckers() {}
    }

    open val typeRefCheckers: Set<CfirTypeRefChecker> = emptySet()
    open val resolvedTypeRefCheckers: Set<CfirResolvedTypeRefChecker> = emptySet()

    @CheckersComponentInternal internal val allTypeRefCheckers: Array<CfirTypeRefChecker> by lazy { typeRefCheckers.toTypedArray() }
    @CheckersComponentInternal internal val allResolvedTypeRefCheckers: Array<CfirResolvedTypeRefChecker> by lazy { (resolvedTypeRefCheckers + typeRefCheckers).toTypedArray() as Array<CfirResolvedTypeRefChecker> }
}

