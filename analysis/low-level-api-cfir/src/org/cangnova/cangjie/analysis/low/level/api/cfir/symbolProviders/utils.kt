

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.combined.LLCombinedCangJieSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.combined.LLCombinedPackageDelegationSymbolProvider
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId

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

internal fun CfirSymbolNamesProvider.mayHaveTopLevelClassifier(classId: ClassId): Boolean {
    val names = getTopLevelClassifierNamesInPackage(classId.packageFqName) ?: return true
    return classId.shortClassName in names
}

internal fun CfirSymbolProvider.materializeTopLevelExtendFiles(): List<CfirFile> =
    when (this) {
        is LLCangJieSymbolProvider -> materializeTopLevelExtendFiles()
        is LLModuleWithDependenciesSymbolProvider -> providers.flatMap { it.materializeTopLevelExtendFiles() }
        is LLCombinedCangJieSymbolProvider -> providers.flatMap { it.materializeTopLevelExtendFiles() }
        is LLCombinedPackageDelegationSymbolProvider -> providers.flatMap { it.materializeTopLevelExtendFiles() }
        is CfirCompositeSymbolProvider -> providers.flatMap { it.materializeTopLevelExtendFiles() }
        else -> emptyList()
    }
