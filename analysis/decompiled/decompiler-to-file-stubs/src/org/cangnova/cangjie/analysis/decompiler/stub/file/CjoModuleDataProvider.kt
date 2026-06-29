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
    /**
     * 返回指定 `.cjo` 二进制文件所属的 CFIR module data。
     *
     * 当文件不属于当前项目结构中的任何 library/builtins 模块，或模块索引尚无法解析 owner 时返回 `null`；
     * stub 构建方据此决定是否继续创建带模块上下文的反编译声明。
     */
    fun getModuleData(binaryFile: VirtualFile): CfirModuleData?

    companion object {
        /**
         * 从 IntelliJ project service 容器中取得当前项目的 `.cjo` module data provider。
         */
        fun getInstance(project: Project): CjoModuleDataProvider = project.service()
    }
}
