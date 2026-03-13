/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.cfir.analysis.checkers.type

import org.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangjie.cfir.analysis.checkers.CfirCheckerWithMppKind
import org.cangjie.cfir.analysis.checkers.MppCheckerKind

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
