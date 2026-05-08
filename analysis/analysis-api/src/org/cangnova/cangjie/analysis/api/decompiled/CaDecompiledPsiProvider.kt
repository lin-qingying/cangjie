package org.cangnova.cangjie.analysis.api.decompiled

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjDecompiledFile

interface CaDecompiledPsiProvider {
    fun getDecompiledFile(binaryFile: VirtualFile): CjDecompiledFile?

    fun findDecompiledFile(module: CaLibraryModule, packageFqName: FqName): CjDecompiledFile?

    fun findDecompiledFile(module: CaBuiltinsModule, packageFqName: FqName): CjDecompiledFile?

    /**
     * 直接恢复 builtins package 的 decompiled PSI。
     *
     * 这样 builtins 消费方不需要再从 project structure 暴露层回捞 `CaBuiltinsModule`，
     * 与 Kotlin 测试平台“builtins 不进入 allModules”的边界保持一致。
     */
    fun findBuiltinsDecompiledFile(packageFqName: FqName): CjDecompiledFile?

    companion object {
        fun getInstance(project: Project): CaDecompiledPsiProvider = project.service()
    }
}
