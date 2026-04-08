package org.cangnova.cangjie.analysis.api.decompiled

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.CaLibraryModule
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjDecompiledFile

/**
 * `.cjo` 二进制索引入口。
 *
 * 该接口统一负责：
 * 1. library / builtins 模块到真实 binary file 的映射；
 * 2. package 到 binary file 的稳定定位；
 * 3. binary file 到 Analysis 模块的反查。
 */
interface CaDecompiledBinaryIndex {
    fun getBinaryFiles(module: CaLibraryModule): List<VirtualFile>

    fun getBinaryFiles(module: CaBuiltinsModule): List<VirtualFile>

    fun findBinaryFile(module: CaLibraryModule, packageFqName: FqName): VirtualFile?

    fun findBinaryFile(module: CaBuiltinsModule, packageFqName: FqName): VirtualFile?

    fun findOwningModule(binaryFile: VirtualFile): CaModule?

    companion object {
        fun getInstance(project: Project): CaDecompiledBinaryIndex = project.service()
    }
}

/**
 * Decompiled PSI 提供器。
 *
 * 该接口把 `.cjo` 恢复为稳定的只读 compiled PSI，
 * 供导航、stub、light declaration 与 symbol fallback 复用。
 */
interface CaDecompiledPsiProvider {
    fun getDecompiledFile(binaryFile: VirtualFile): CjDecompiledFile?

    fun findDecompiledFile(module: CaLibraryModule, packageFqName: FqName): CjDecompiledFile?

    fun findDecompiledFile(module: CaBuiltinsModule, packageFqName: FqName): CjDecompiledFile?

    companion object {
        fun getInstance(project: Project): CaDecompiledPsiProvider = project.service()
    }
}

/**
 * Decompiled 文本渲染入口。
 *
 * 这里只负责把 compiled metadata/PSI 渲染为展示文本，
 * 不承载模块发现与 PSI 生命周期管理。
 */
interface CaDecompiledTextRenderer {
    fun render(binaryFile: VirtualFile): String?

    fun render(file: CjDecompiledFile): String

    companion object {
        fun getInstance(project: Project): CaDecompiledTextRenderer = project.service()
    }
}

/**
 * Builtins 二进制虚拟文件提供器。
 *
 * project-structure、decompiled 与 low-level 静态依赖需要共用同一套
 * builtins 发现语义，因此这里收敛为应用级服务。
 */
abstract class CaBuiltinsVirtualFileProvider {
    abstract fun getBuiltinVirtualFiles(): Set<VirtualFile>

    abstract fun createBuiltinsScope(project: Project): GlobalSearchScope

    companion object {
        fun getInstance(): CaBuiltinsVirtualFileProvider {
            return requireNotNull(
                ApplicationManager.getApplication().getService(CaBuiltinsVirtualFileProvider::class.java),
            ) {
                "CaBuiltinsVirtualFileProvider is not registered in the current application container"
            }
        }
    }
}
