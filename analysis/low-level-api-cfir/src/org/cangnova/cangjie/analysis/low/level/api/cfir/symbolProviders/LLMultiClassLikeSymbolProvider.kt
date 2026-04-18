/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import org.cangnova.cangjie.cfir.symbols.impl.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * An [LLMultiClassLikeSymbolProvider] is able to provide all class-like symbols for a single [ClassId].
 *
 * Its behavior is in contrast to [getClassLikeSymbolByClassId][org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider.getClassLikeSymbolByClassId],
 * which only returns the first class-like symbol with the class ID, whereas [getAllClassLikeSymbolsByClassId] returns *all* such symbols.
 */
internal interface LLMultiClassLikeSymbolProvider : LLPsiAwareSymbolProvider {
    /**
     * Returns all [CfirClassLikeSymbol]s with the given [classId].
     */
    fun getAllClassLikeSymbolsByClassId(classId: ClassId): List<CfirClassLikeSymbol<*>>
}
