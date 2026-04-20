package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendSymbolImpl
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledPsiProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile

/**
 * decompiled symbol 的 package 归属推导。
 *
 * 这里只暴露能够稳定回推 package 的公共 symbol 形态：
 * file / class-like / callable / extend / package。
 * 其余场景若没有显式 PSI，则保持 `null`，避免引入猜测式回退。
 */
internal fun CaSymbol.decompiledContainingPackageFqName(): FqName? = when (this) {
    is CaFileSymbol -> packageFqName
    is CaClassLikeSymbol -> classId?.packageFqName
    is CaExtendSymbol -> {
        val declarationPsi = when (this) {
            is CaCfirExtendSymbolImpl -> extendPsi?.containingFile as? CjFile
            else -> (this as? CaDeclarationSymbol)?.psi?.containingFile as? CjFile
        }
        declarationPsi?.packageFqName
            ?: (this as? CaCfirExtendSymbolImpl)?.extendPackageFqName
            ?: targetClassId?.packageFqName
    }

    is CaCallableSymbol -> callableId?.packageName
    is CaPackageSymbol -> fqName
    else -> null
}

/**
 * decompiled fallback 查找入口。
 *
 * 查找顺序固定为：
 * 1. 当前 session 的 use-site module；
 * 2. builtins；
 * 3. 普通 libraries。
 *
 * 这样 `CaSourceProvider` 与 `CaOriginalPsiProvider` 会复用同一套 package binary 选择语义。
 */
internal fun CaCfirSession.findDecompiledContainingFile(
    packageFqName: FqName,
    preferredModule: CaModule? = useSiteModule,
): CjFile? {
    val psiProvider = project.getService(CaDecompiledPsiProvider::class.java) ?: return null
    val projectStructure = CangJieProjectStructureProvider.getInstance(project)

    fun findInModule(module: CaModule?): CjFile? = when (module) {
        is CaBuiltinsModule -> psiProvider.findDecompiledFile(module, packageFqName)
        is CaLibraryModule -> psiProvider.findDecompiledFile(module, packageFqName)
        else -> null
    }

    findInModule(preferredModule)?.let { return it }

    projectStructure.allModules.filterIsInstance<CaBuiltinsModule>().forEach { module ->
        if (module === preferredModule) return@forEach
        psiProvider.findDecompiledFile(module, packageFqName)?.let { return it }
    }

    projectStructure.allModules.filterIsInstance<CaLibraryModule>().forEach { module ->
        if (module === preferredModule) return@forEach
        psiProvider.findDecompiledFile(module, packageFqName)?.let { return it }
    }

    return null
}
