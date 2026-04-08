package org.cangnova.cangjie.analysis.decompiled.filestubs

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsVirtualFileProvider
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import java.io.File

/**
 * 暴露 builtins 根目录，供二进制仓库恢复 `CjoManager` 搜索路径。
 */
interface CaBuiltinsRootAware {
    fun getBuiltinRootVirtualFiles(): Set<VirtualFile>
}

/**
 * 按宿主环境约定解析 builtins `.cjo` 根路径。
 *
 * 与 low-level CFIR 共用：
 * - `cangjie.stdlib.module`
 * - `CANGJIE_STDLIB_MODULE`
 */
class CaBuiltinsVirtualFileProviderCliImpl : CaBuiltinsVirtualFileProvider(), CaBuiltinsRootAware {
    override fun getBuiltinVirtualFiles(): Set<VirtualFile> {
        return getBuiltinRootVirtualFiles()
            .flatMapTo(linkedSetOf()) { root -> collectBuiltinFiles(root) }
    }

    override fun getBuiltinRootVirtualFiles(): Set<VirtualFile> {
        return readPaths("cangjie.stdlib.module", "CANGJIE_STDLIB_MODULE")
            .mapNotNull { path ->
                StandardFileSystems.local().findFileByPath(path.replace('\\', '/'))
                    ?: run {
                        logger<CaBuiltinsVirtualFileProviderCliImpl>().warn("Cannot resolve builtins path: $path")
                        null
                    }
            }
            .toCollection(linkedSetOf())
    }

    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        return GlobalSearchScope.filesScope(project, getBuiltinVirtualFiles())
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

    private fun readPaths(propertyKey: String, envKey: String): List<String> {
        val raw = System.getProperty(propertyKey)
            ?.takeIf(String::isNotBlank)
            ?: System.getenv(envKey)?.takeIf(String::isNotBlank)
            ?: return emptyList()
        return raw.split(File.pathSeparator).filter(String::isNotBlank)
    }
}
