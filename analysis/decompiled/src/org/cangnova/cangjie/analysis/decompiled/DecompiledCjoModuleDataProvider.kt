@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
    org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals::class,
)

package org.cangnova.cangjie.analysis.decompiled

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.decompiler.stub.file.CjoBinaryFileReader
import org.cangnova.cangjie.analysis.decompiler.stub.file.CjoModuleDataProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirBuiltinsSessionFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.DetachedCjoModuleData
import org.cangnova.cangjie.cfir.session.DetachedCjoSession
import org.cangnova.cangjie.name.FqName

/**
 * `.cjo` 反编译 stub 构建的真实 moduleData owner。
 *
 * builtins 统一来自 [LLCfirBuiltinsSessionFactory]，
 * library 统一来自 [LLCfirSessionCache]，decompiler/psi/text 层不创建宿主 session。
 *
 * 结构外 `.cjo`（不属于项目结构中的任何 library/builtins 模块）拿到 per-file 的
 * [DetachedCjoModuleData]：它只承载反序列化所需的最小 session，不拉起真实 builtins/library session，
 * 也不触碰 stub 索引，因此 `readFile` 在结构未就绪（项目刚打开、SDK 还没注册）时同样能产出 stub。
 */
class DecompiledCjoModuleDataProvider(
    /**
     * 提供 binary index 与 low-level CFIR session 服务的 IntelliJ project。
     */
    private val project: Project,
) : CjoModuleDataProvider {
    /**
     * 根据 `.cjo` 二进制所属模块返回对应 CFIR module data。
     *
     * Builtins 使用目标平台 builtins session，library 使用偏向 binary 的 low-level session；
     * 结构外文件降级为 detached module data，不返回 `null`。
     */
    override fun getModuleData(binaryFile: VirtualFile): CfirModuleData =
        when (val ownerModule = CaDecompiledBinaryIndex.getInstance(project).findOwningModule(binaryFile)) {
            is CaBuiltinsModule -> LLCfirBuiltinsSessionFactory.getInstance(project)
                .getBuiltinsSession(ownerModule.targetPlatform).moduleData
            is CaLibraryModule -> LLCfirSessionCache.getInstance(project).getSession(ownerModule, preferBinary = true).moduleData
            else -> detachedModuleData(binaryFile)
        }

    /**
     * 为结构外 `.cjo` 创建 detached owner。
     *
     * 包名只用于诊断标识，读不到时退回文件路径；语言设置沿用项目结构的库侧设置，
     * 保证与同一 SDK 下的 library session 看到一致的语言语义。
     */
    private fun detachedModuleData(binaryFile: VirtualFile): DetachedCjoModuleData {
        val packageFqName = CjoBinaryFileReader.readPackageFqName(binaryFile)
            ?: FqName("<unknown:${binaryFile.path}>")
        val languageVersionSettings = CangJieProjectStructureProvider.getInstance(project).libraryLanguageVersionSettings
        return DetachedCjoModuleData(packageFqName, DetachedCjoSession(packageFqName, languageVersionSettings))
    }
}
