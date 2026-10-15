@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLCangJieSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.rawMacroSurfaceUsageStoreOrNull
import org.cangnova.cangjie.name.FqName

/**
 * 为源码 low-level session 收集完整的 module 级 pre-macro CFIR 输入。
 *
 * 该入口通过宿主 symbol name 索引枚举包，再由声明索引定位各包文件；每个输入文件都独立构建，
 * 不复用普通 LL `CfirProvider` 的文件缓存。宏构造会原地 stable-splice 输入树，因此它必须独占这些树，
 * 直到成功快照被发布或丢弃。
 * 只有包名集合可完整枚举时才返回输入，防止将部分文件误当作完整模块快照交给分类和构造阶段。
 */
fun LLCfirSession.buildPreMacroRawBuildResultForModule(): PreMacroRawBuildResult? {
    if (caModule !is CaSourceModule) return null

    val resolvableSession = this as? LLCfirResolvableModuleSession ?: return null
    val sourceSymbolProvider = cfirProvider.symbolProvider as? LLCangJieSymbolProvider ?: return null
    val packageNames = sourceSymbolProvider.symbolNamesProvider.getPackageNames() ?: return null
    val sourceFiles = packageNames
        .asSequence()
        .map(::FqName)
        .flatMap { packageFqName -> sourceSymbolProvider.declarationProvider.findFilesForFacadeByPackage(packageFqName).asSequence() }
        .filter { file ->
            file.sourceKind == CjSourceKind.SOURCE && CjSourceKind.isSourceFileName(file.name)
        }
        .distinct()
        .toList()
    val cfirFiles = sourceFiles.map(resolvableSession.moduleComponents.cfirFileBuilder::buildRawCfirFileUncached)
    val fileSurfaces = cfirFiles.map { file -> rawMacroSurfaceUsageStoreOrNull?.surfaces(file).orEmpty() }

    return buildPreMacroRawFiles(
        session = this,
        rawCfirFiles = cfirFiles,
        fileSurfaces = fileSurfaces,
        sourceFileIdentities = sourceFiles,
    )
}
