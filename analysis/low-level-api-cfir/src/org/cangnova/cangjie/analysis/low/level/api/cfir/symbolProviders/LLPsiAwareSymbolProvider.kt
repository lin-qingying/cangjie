/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.symbols.impl.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * A [CfirSymbolProvider][org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider] that is able to provide class-like symbols for
 * specific PSI elements.
 *
 * This interface only needs to be implemented by symbol providers whose scope might contain multiple PSI elements with the same [ClassId]
 * (class ID ambiguities). Furthermore, such a symbol provider must also be eligible for [LLModuleSpecificSymbolProviderAccess]. For
 * example, combined symbol providers cannot be accessed in such a manner (they are always dependency symbol providers), and thus don't need
 * to implement [LLPsiAwareSymbolProvider].
 */
internal interface LLPsiAwareSymbolProvider {
    /**
     * Returns a [CfirClassLikeSymbol] for the specific [declaration], or `null` if there is no symbol that matches [declaration]. Symbols
     * without associated PSI are not considered as results.
     *
     * As per the contract of [LLModuleSpecificSymbolProviderAccess], the given [declaration] must be in the scope of the symbol provider's
     * module.
     *
     * In contrast to [getClassLikeSymbolByClassId][org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider.getClassLikeSymbolByClassId],
     * this function is aware of the requested PSI and is able to return a specific symbol for it.
     *
     * @see getClassLikeSymbolMatchingPsi
     */
    @LLModuleSpecificSymbolProviderAccess
    fun getClassLikeSymbolByPsi(classId: ClassId, declaration: PsiElement): CfirClassLikeSymbol<*>?
}
