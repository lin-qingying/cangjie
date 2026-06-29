@file:OptIn(org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals::class)

package org.cangnova.cangjie.analysis.decompiled

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.decompiler.stub.file.CjoModuleDataProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirBuiltinsSessionFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.cfir.common.CfirModuleData

/**
 * `.cjo` 反编译 stub 构建的真实 moduleData owner。
 *
 * builtins 统一来自 [LLCfirBuiltinsSessionFactory]，
 * library 统一来自 [LLCfirSessionCache]，decompiler/psi/text 层不创建宿主 session。
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
     * 无法归属到已知模块时返回 `null`。
     */
    override fun getModuleData(binaryFile: VirtualFile): CfirModuleData? {
        return when (val ownerModule = CaDecompiledBinaryIndex.getInstance(project).findOwningModule(binaryFile)) {
            is CaBuiltinsModule -> LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(ownerModule.targetPlatform).moduleData
            is CaLibraryModule -> LLCfirSessionCache.getInstance(project).getSession(ownerModule, preferBinary = true).moduleData
            else -> null
        }
    }
}
