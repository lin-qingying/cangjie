package org.cangnova.cangjie.analysis.test.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProviderBaseImpl
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProviderCliImpl
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import java.io.File

/**
 * `BuiltinsVirtualFileProviderTestImpl` 对位 Kotlin `BuiltinsVirtualFileProviderTestImpl`。
 *
 * 测试中的 [org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule]
 * 直接使用 fixture root 下的真实 `.cjo` VirtualFile。
 * 这样 binary index、decompiled PSI 和 stub builder 共享同一条 VFS/fileType 路径。
 */
internal class BuiltinsVirtualFileProviderTestImpl : BuiltinsVirtualFileProviderBaseImpl() {
    /**
     * 复用 CLI builtins provider 的标准 builtins 解析逻辑。
     */
    private val coreVirtualFileProvider = BuiltinsVirtualFileProviderCliImpl()

    /**
     * 当前测试可用的 builtins 根目录集合。
     */
    private val builtinRoots by lazy {
        coreVirtualFileProvider.getBuiltinVirtualFiles()
            .map(::toBuiltinRoot)
            .toCollection(linkedSetOf())
            .ifEmpty { resolveFallbackBuiltinRoots() }
    }

    /**
     * 当前测试可见的 builtins `.cjo` 文件集合。
     */
    private val files by lazy {
        builtinRoots.flatMapTo(linkedSetOf()) { root -> collectBuiltinFiles(root) }
    }

    /**
     * 返回全部 builtins binary 文件。
     */
    override fun getBuiltinVirtualFiles(): Set<VirtualFile> = files

    /**
     * 返回全部 builtins 根目录。
     */
    override fun getBuiltinRootVirtualFiles(): Set<VirtualFile> {
        return builtinRoots
    }

    /**
     * 返回指定 project 可见的 builtins 根目录。
     */
    override fun getBuiltinRootVirtualFiles(project: Project): Set<VirtualFile> {
        return builtinRoots
    }

    /**
     * 创建覆盖当前测试 builtins 文件集合的搜索作用域。
     */
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
        val repositoryRoot = locateRepositoryRoot(File("").absoluteFile.normalize())
        val fallbackCandidates = listOf(
            repositoryRoot.resolve("cfir/cfir-serialization/testResources/cjo-sdk/windows_x86_64_cjnative"),
            repositoryRoot.resolve("cfir/cfir-serialization/build/resources/test/cjo-sdk/windows_x86_64_cjnative"),
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

    /**
     * 将 stdlib 根目录规范化到包含 builtins 文件的父级目录。
     */
    private fun normalizeStdlibRoot(path: File): File {
        val normalized = path.normalize()
        if (normalized.resolve("std/std.core.${CangJieBuiltInFileType.defaultExtension}").isFile) return normalized
        if (normalized.resolve("std.core.${CangJieBuiltInFileType.defaultExtension}").isFile) {
            return normalized.parentFile ?: normalized
        }
        return normalized
    }

    /**
     * 从当前目录向上定位仓库根目录。
     */
    private fun locateRepositoryRoot(start: File): File {
        return generateSequence(start) { file -> file.parentFile }
            .firstOrNull { file -> file.resolve("settings.gradle.kts").isFile }
            ?: start
    }

    /**
     * 从单个 builtins 文件反推出 builtins 根目录。
     */
    private fun toBuiltinRoot(file: VirtualFile): VirtualFile {
        return if (file.parent?.name == "std") file.parent.parent ?: file.parent else file.parent ?: file
    }
}
