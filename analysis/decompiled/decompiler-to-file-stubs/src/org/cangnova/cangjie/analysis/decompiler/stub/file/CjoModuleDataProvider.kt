package org.cangnova.cangjie.analysis.decompiler.stub.file

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.cfir.common.CfirModuleData

/**
 * `.cjo` binary 所属 CFIR module data 的解析入口。
 *
 * 该入口放在 file-stub 层，是因为 `.cjo` stub 构建需要真实 module owner；
 * 具体 owner 仍由上层 Analysis/low-level 装配提供，decompiler-to-psi 不直接依赖 low-level。
 */
interface CjoModuleDataProvider {
    fun getModuleData(binaryFile: VirtualFile): CfirModuleData?

    companion object {
        fun getInstance(project: Project): CjoModuleDataProvider = project.service()
    }
}
