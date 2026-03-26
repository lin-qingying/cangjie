

package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

@Suppress("UNCHECKED_CAST")
abstract class TypeCheckers {
    companion object {
        val EMPTY: TypeCheckers = object : TypeCheckers() {}
    }

    open val typeRefCheckers: Set<CfirTypeRefChecker> = emptySet()
    open val resolvedTypeRefCheckers: Set<CfirResolvedTypeRefChecker> = emptySet()

    @CheckersComponentInternal internal val allTypeRefCheckers: Array<CfirTypeRefChecker> by lazy { typeRefCheckers.toTypedArray() }
    @CheckersComponentInternal internal val allResolvedTypeRefCheckers: Array<CfirResolvedTypeRefChecker> by lazy { (resolvedTypeRefCheckers + typeRefCheckers).toTypedArray() as Array<CfirResolvedTypeRefChecker> }
}
