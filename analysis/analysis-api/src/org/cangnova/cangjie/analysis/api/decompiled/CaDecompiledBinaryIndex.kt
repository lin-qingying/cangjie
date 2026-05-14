package org.cangnova.cangjie.analysis.api.decompiled

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.name.FqName

/**
 * `.cjo` 反编译产物的项目级索引。
 *
 * 把"module ↔ binary file ↔ package"三角的查询能力集中暴露:
 * - 给 IDE 的 Go-to-Class、Find Symbol、引用解析提供快速反查;
 * - 隔离上层不直接接触虚拟文件系统中的 `.cjo` 物理布局。
 *
 * 通过 [getInstance] 取得当前 [Project] 上的实例。
 */
interface CaDecompiledBinaryIndex {
    /** 枚举指定库模块中的所有反编译 binary 文件。 */
    fun getBinaryFiles(module: CaLibraryModule): List<VirtualFile>

    /** 枚举 builtins 模块中的所有反编译 binary 文件。 */
    fun getBinaryFiles(module: CaBuiltinsModule): List<VirtualFile>

    /**
     * 直接从 `.cjo` binary 头读取真实包名。
     *
     * package provider 这类只需要 package facts 的上层，必须走这条轻量路径，
     * 不能先物化 decompiled PSI 再回头读取 `packageFqName`。
     */
    fun readPackageFqName(binaryFile: VirtualFile): FqName?

    /** 按模块 + 包名定位库中的 binary 文件,找不到时返回 `null`。 */
    fun findBinaryFile(module: CaLibraryModule, packageFqName: FqName): VirtualFile?

    /** 按模块 + 包名定位 builtins 中的 binary 文件,找不到时返回 `null`。 */
    fun findBinaryFile(module: CaBuiltinsModule, packageFqName: FqName): VirtualFile?

    /**
     * 直接按 builtins package 查找 binary file。
     *
     * builtins module 在部分平台（例如 Analysis API 测试宿主）不会暴露进 `allModules`，
     * 因而 builtins `.cjo` 的定位不能强依赖外层先枚举到 `CaBuiltinsModule`。
     */
    fun findBuiltinsBinaryFile(packageFqName: FqName): VirtualFile?

    /** 反向定位:给定 binary 文件,找出它所属的 [CaModule]。 */
    fun findOwningModule(binaryFile: VirtualFile): CaModule?

    companion object {
        /** 获取当前 [Project] 上的索引实例(application service)。 */
        fun getInstance(project: Project): CaDecompiledBinaryIndex = project.service()
    }
}
