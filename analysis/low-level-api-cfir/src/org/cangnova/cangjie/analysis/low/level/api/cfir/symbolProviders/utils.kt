/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

private const val KOTLIN_PACKAGE_PREFIX = "kotlin."

internal fun ClassId.isKotlinPackage(): Boolean = startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)

internal fun FqName.isKotlinPackage(): Boolean = startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)

internal fun String.isKotlinPackage(): Boolean = startsWith(KOTLIN_PACKAGE_PREFIX)

/**
 * Checks if this [CfirBasedSymbol] has the given PSI element as a source.
 *
 * [hasPsi] exists to ensure a consistent approach to compare PSI in symbol providers, e.g. by [LLPsiAwareSymbolProvider].
 */
internal fun CfirBasedSymbol<*>.hasPsi(element: PsiElement): Boolean = cfir.psi == element

/**
 * Returns a [CfirClassLikeSymbol] with the given [classId] that matches [declaration].
 *
 * If the symbol provider is not an [LLPsiAwareSymbolProvider], the function falls back to [CfirSymbolProvider.getClassLikeSymbolByClassId],
 * but still ensures that the resulting symbol matches [declaration].
 */
@LLModuleSpecificSymbolProviderAccess
internal fun CfirSymbolProvider.getClassLikeSymbolMatchingPsi(classId: ClassId, declaration: PsiElement): CfirClassLikeSymbol<*>? {
    if (this is LLPsiAwareSymbolProvider) {
        return getClassLikeSymbolByPsi(classId, declaration)
    }

    return getClassLikeSymbolByClassId(classId)?.takeIf { symbol ->
        // If the symbol's PSI is `null`, it cannot be a symbol for `element`, since the PSI exists and any symbol created for it should
        // have a PSI source.
        symbol.hasPsi(declaration)
    }
}

internal fun CfirSymbolProvider.getClassLikeSymbolByClassIdWithoutDependencies(classId: ClassId): CfirClassLikeSymbol<*>? =
    when (this) {
        is LLModuleWithDependenciesSymbolProvider -> getClassLikeSymbolByClassIdWithoutDependencies(classId)
        else -> getClassLikeSymbolByClassId(classId)
    }

@LLModuleSpecificSymbolProviderAccess
internal fun CfirSymbolProvider.getClassLikeSymbolByPsiWithoutDependencies(
    classId: ClassId,
    declaration: PsiElement,
): CfirClassLikeSymbol<*>? =
    when (this) {
        is LLModuleWithDependenciesSymbolProvider -> getClassLikeSymbolByPsiWithoutDependencies(classId, declaration)
        else -> getClassLikeSymbolMatchingPsi(classId, declaration)
    }

internal fun CfirSymbolProvider.getAllClassLikeSymbolsByClassIdOrSingle(classId: ClassId): List<CfirClassLikeSymbol<*>> =
    when (this) {
        is LLMultiClassLikeSymbolProvider -> getAllClassLikeSymbolsByClassId(classId)
        else -> listOfNotNull(getClassLikeSymbolByClassId(classId))
    }

