package org.cangnova.cangjie.analysis.test.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.BinaryLightVirtualFile
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsRootAware
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.decompiled.filestubs.CaBuiltinsVirtualFileProviderCliImpl
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import java.io.File

/**
 * `CaBuiltinsVirtualFileProviderTestImpl` 对位 Kotlin `BuiltinsVirtualFileProviderTestImpl`。
 *
 * 测试中的 [org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule]
 * 使用独立的内存 builtins 文件，避免生产态 builtins 路径与测试模块内容范围混在一起。
 * 同时，`.cjo` 反编译仍需要真实仓库根参与 `CjoManager` 搜索路径恢复，
 * 因而测试 provider 额外暴露 builtins roots 这一仓颉特有语义。
 */
internal class CaBuiltinsVirtualFileProviderTestImpl : CaBuiltinsVirtualFileProvider(), CaBuiltinsRootAware {
    private val coreVirtualFileProvider = CaBuiltinsVirtualFileProviderCliImpl()
    private val builtinRoots by lazy {
        coreVirtualFileProvider.getBuiltinRootVirtualFiles().ifEmpty { resolveFallbackBuiltinRoots() }
    }

    private val files by lazy {
        builtinRoots.flatMapTo(linkedSetOf()) { root -> collectBuiltinFiles(root) }.mapTo(mutableSetOf()) { file ->
            BinaryLightVirtualFile(file.name, file.contentsToByteArray())
        }
    }

    override fun getBuiltinVirtualFiles(): Set<VirtualFile> = files

    override fun getBuiltinRootVirtualFiles(): Set<VirtualFile> {
        return builtinRoots
    }

    override fun createBuiltinsScope(project: Project): GlobalSearchScope =
        GlobalSearchScope.filesScope(project, files)

    /**
     * Analysis API 测试默认不显式设置 `cangjie.stdlib.module`。
     *
     * 仓颉的 `String`、`ToString` 等不属于 primitive builtins，而是来自 stdlib `.cjo`。
     * 因而这里必须沿用 compiler test framework 已有的 stdlib fixture 回退规则，
     * 否则 source module 的 Analysis API 解析链会天然缺失 `std.core`。
     */
    private fun resolveFallbackBuiltinRoots(): Set<VirtualFile> {
        val fallbackCandidates = listOf(
            File("cfir/cfir-serialization/testResources/cjo-sdk/windows_x86_64_cjnative"),
            File("cfir/cfir-serialization/build/resources/test/cjo-sdk/windows_x86_64_cjnative"),
        )

        return fallbackCandidates
            .asSequence()
            .filter { it.exists() && it.isDirectory }
            .map(::normalizeStdlibRoot)
            .filter { it.exists() && it.isDirectory }
            .mapNotNull { root ->
                StandardFileSystems.local().findFileByPath(root.path.replace('\\', '/'))
            }
            .toCollection(linkedSetOf())
    }

    private fun normalizeStdlibRoot(path: File): File {
        val normalized = path.normalize()
        if (normalized.resolve("std/std.core.${CangJieBuiltInFileType.defaultExtension}").isFile) return normalized
        if (normalized.resolve("std.core.${CangJieBuiltInFileType.defaultExtension}").isFile) {
            return normalized.parentFile ?: normalized
        }
        return normalized
    }

    private fun collectBuiltinFiles(root: VirtualFile): List<VirtualFile> {
        if (!root.isDirectory) {
            return listOfNotNull(root.takeIf { it.extension.equals(CangJieBuiltInFileType.defaultExtension, ignoreCase = true) })
        }

        val files = linkedSetOf<VirtualFile>()
        VfsUtilCore.iterateChildrenRecursively(root, null) { child ->
            if (!child.isDirectory &&
                (child.fileType == CangJieBuiltInFileType ||
                    child.extension.equals(CangJieBuiltInFileType.defaultExtension, ignoreCase = true))
            ) {
                files += child
            }
            true
        }
        return files.toList()
    }
}
