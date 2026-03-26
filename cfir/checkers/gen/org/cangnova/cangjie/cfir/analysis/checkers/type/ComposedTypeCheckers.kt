

package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

class ComposedTypeCheckers : TypeCheckers() {
    override val typeRefCheckers: Set<CfirTypeRefChecker>
        get() = _typeRefCheckers
    override val resolvedTypeRefCheckers: Set<CfirResolvedTypeRefChecker>
        get() = _resolvedTypeRefCheckers

    private val _typeRefCheckers: MutableSet<CfirTypeRefChecker> = mutableSetOf()
    private val _resolvedTypeRefCheckers: MutableSet<CfirResolvedTypeRefChecker> = mutableSetOf()

    @CheckersComponentInternal
    fun register(checkers: TypeCheckers) {
        _typeRefCheckers.addAll(checkers.typeRefCheckers)
        _resolvedTypeRefCheckers.addAll(checkers.resolvedTypeRefCheckers)
    }
}
