/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.cfir.analysis.checkers.type

import org.cangjie.cfir.analysis.CheckersComponentInternal

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
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
