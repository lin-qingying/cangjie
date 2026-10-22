@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLCangJieSymbolProvider
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.rawMacroSurfaceUsageStoreOrNull
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile

/**
 * 为源码 low-level session 收集完整的 module 级 pre-macro CFIR 输入。
 *
 * 该入口通过宿主 symbol name 索引枚举包，再由声明索引定位各包文件；每个输入文件都独立构建，
 * 不复用普通 LL `CfirProvider` 的文件缓存。宏构造会原地 stable-splice 输入树，因此它必须独占这些树，
 * 直到成功快照被发布或丢弃。
 * 只有包名集合可完整枚举时才返回输入，防止将部分文件误当作完整模块快照交给分类和构造阶段。
 *
 * 读访问按文件切分：整模块构建若独占一个读动作，EDT 上的写动作（例如 SDK 变更后的
 * `PsiManager.dropPsiCaches`）会一直等待直到全部文件构建完成，表现为 UI 冻结。
 * 逐文件读访问让写请求最多等待单个文件，并在文件之间响应取消。
 */
fun LLCfirSession.buildPreMacroRawBuildResultForModule(): PreMacroRawBuildResult? {
    if (caModule !is CaSourceModule) return null

    val resolvableSession = this as? LLCfirResolvableModuleSession ?: return null
    val surfaceStore = rawMacroSurfaceUsageStoreOrNull

    val sourceFiles = ReadAction.compute<List<CjFile>, RuntimeException> {
        val sourceSymbolProvider = cfirProvider.symbolProvider as? LLCangJieSymbolProvider
            ?: return@compute emptyList()
        val packageNames = sourceSymbolProvider.symbolNamesProvider.getPackageNames()
            ?: return@compute emptyList()
        packageNames
            .asSequence()
            .map(::FqName)
            .flatMap { packageFqName ->
                sourceSymbolProvider.declarationProvider.findFilesForFacadeByPackage(packageFqName).asSequence()
            }
            .filter { file ->
                file.sourceKind == CjSourceKind.SOURCE && CjSourceKind.isSourceFileName(file.name)
            }
            .distinct()
            .toList()
    }

    val fileBuilder = resolvableSession.moduleComponents.cfirFileBuilder
    val cfirFiles = ArrayList<CfirFile>(sourceFiles.size)
    val fileSurfaces = ArrayList<List<MacroSurface>>(sourceFiles.size)
    for (file in sourceFiles) {
        ProgressManager.checkCanceled()
        val built = ReadAction.compute<Pair<CfirFile, List<MacroSurface>>, RuntimeException> {
            val cfirFile = fileBuilder.buildRawCfirFileUncached(file)
            cfirFile to surfaceStore?.surfaces(cfirFile).orEmpty()
        }
        cfirFiles += built.first
        fileSurfaces += built.second
    }

    return buildPreMacroRawFiles(
        session = this,
        rawCfirFiles = cfirFiles,
        fileSurfaces = fileSurfaces,
        sourceFileIdentities = sourceFiles,
    )
}
