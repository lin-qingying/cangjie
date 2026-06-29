

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
 * 判断当前 [CfirBasedSymbol] 的 CFIR 来源 PSI 是否为 [element]。
 *
 * 该扩展为符号提供器内部的 PSI 比较提供统一入口，避免各实现重复书写来源比较逻辑。
 */
internal fun CfirBasedSymbol<*>.hasPsi(element: PsiElement): Boolean = cfir.psi == element

/**
 * 返回 [classId] 对应且来源 PSI 匹配 [declaration] 的 class-like 符号。
 *
 * 当接收者实现 [LLPsiAwareSymbolProvider] 时直接使用 PSI 精确查询；否则回退到 [CfirSymbolProvider.getClassLikeSymbolByClassId]，
 * 并在返回前确认符号来源 PSI 与 [declaration] 一致。
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

/**
 * 查询 [classId] 对应的 class-like 符号，但在模块级组合提供器上排除依赖。
 */
internal fun CfirSymbolProvider.getClassLikeSymbolByClassIdWithoutDependencies(classId: ClassId): CfirClassLikeSymbol<*>? =
    when (this) {
        is LLModuleWithDependenciesSymbolProvider -> getClassLikeSymbolByClassIdWithoutDependencies(classId)
        else -> getClassLikeSymbolByClassId(classId)
    }

/**
 * 按 [declaration] PSI 查询 [classId] 对应的 class-like 符号，但在模块级组合提供器上排除依赖。
 */
@LLModuleSpecificSymbolProviderAccess
internal fun CfirSymbolProvider.getClassLikeSymbolByPsiWithoutDependencies(
    classId: ClassId,
    declaration: PsiElement,
): CfirClassLikeSymbol<*>? =
    when (this) {
        is LLModuleWithDependenciesSymbolProvider -> getClassLikeSymbolByPsiWithoutDependencies(classId, declaration)
        else -> getClassLikeSymbolMatchingPsi(classId, declaration)
    }

/**
 * 查询 [classId] 对应的全部 class-like 符号；不支持多声明的提供器返回单个命中结果。
 */
internal fun CfirSymbolProvider.getAllClassLikeSymbolsByClassIdOrSingle(classId: ClassId): List<CfirClassLikeSymbol<*>> =
    when (this) {
        is LLMultiClassLikeSymbolProvider -> getAllClassLikeSymbolsByClassId(classId)
        else -> listOfNotNull(getClassLikeSymbolByClassId(classId))
    }

/**
 * 根据名称索引判断指定 [classId] 的顶层 classifier 是否可能存在。
 */
internal fun CfirSymbolNamesProvider.mayHaveTopLevelClassifier(classId: ClassId): Boolean {
    val names = getTopLevelClassifierNamesInPackage(classId.packageFqName) ?: return true
    return classId.shortClassName in names
}

/**
 * 递归物化符号提供器中的顶层扩展文件。
 *
 * 对组合提供器会展开内部提供器；对非仓颉源码提供器返回空列表。
 */
internal fun CfirSymbolProvider.materializeTopLevelExtendFiles(): List<CfirFile> =
    when (this) {
        is LLCangJieSymbolProvider -> materializeTopLevelExtendFiles()
        is LLModuleWithDependenciesSymbolProvider -> providers.flatMap { it.materializeTopLevelExtendFiles() }
        is LLCombinedCangJieSymbolProvider -> providers.flatMap { it.materializeTopLevelExtendFiles() }
        is LLCombinedPackageDelegationSymbolProvider -> providers.flatMap { it.materializeTopLevelExtendFiles() }
        is CfirCompositeSymbolProvider -> providers.flatMap { it.materializeTopLevelExtendFiles() }
        else -> emptyList()
    }
