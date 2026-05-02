package org.cangnova.cangjie.analysis.test.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.BinaryLightVirtualFile
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.decompiled.filestubs.CaBuiltinsVirtualFileProviderCliImpl

/**
 * `CaBuiltinsVirtualFileProviderTestImpl` 对位 Kotlin `BuiltinsVirtualFileProviderTestImpl`。
 *
 * 测试中的 [org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule]
 * 使用独立的内存 builtins 文件，避免生产态 builtins 路径与测试模块内容范围混在一起。
 */
internal class CaBuiltinsVirtualFileProviderTestImpl : CaBuiltinsVirtualFileProvider() {
    private val coreVirtualFileProvider = CaBuiltinsVirtualFileProviderCliImpl()

    private val files by lazy {
        coreVirtualFileProvider.getBuiltinVirtualFiles().mapTo(mutableSetOf()) { file ->
            BinaryLightVirtualFile(file.name, file.contentsToByteArray())
        }
    }

    override fun getBuiltinVirtualFiles(): Set<VirtualFile> = files

    override fun createBuiltinsScope(project: Project): GlobalSearchScope =
        GlobalSearchScope.filesScope(project, files)
}
