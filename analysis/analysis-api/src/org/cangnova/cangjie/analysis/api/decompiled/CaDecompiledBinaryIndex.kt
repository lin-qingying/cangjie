package org.cangnova.cangjie.analysis.api.decompiled

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.name.FqName

interface CaDecompiledBinaryIndex {
    fun getBinaryFiles(module: CaLibraryModule): List<VirtualFile>

    fun getBinaryFiles(module: CaBuiltinsModule): List<VirtualFile>

    /**
     * 直接从 `.cjo` binary 头读取真实包名。
     *
     * package provider 这类只需要 package facts 的上层，必须走这条轻量路径，
     * 不能先物化 decompiled PSI 再回头读取 `packageFqName`。
     */
    fun readPackageFqName(binaryFile: VirtualFile): FqName?

    fun findBinaryFile(module: CaLibraryModule, packageFqName: FqName): VirtualFile?

    fun findBinaryFile(module: CaBuiltinsModule, packageFqName: FqName): VirtualFile?

    /**
     * 直接按 builtins package 查找 binary file。
     *
     * builtins module 在部分平台（例如 Analysis API 测试宿主）不会暴露进 `allModules`，
     * 因而 builtins `.cjo` 的定位不能强依赖外层先枚举到 `CaBuiltinsModule`。
     */
    fun findBuiltinsBinaryFile(packageFqName: FqName): VirtualFile?

    fun findOwningModule(binaryFile: VirtualFile): CaModule?

    companion object {
        fun getInstance(project: Project): CaDecompiledBinaryIndex = project.service()
    }
}
