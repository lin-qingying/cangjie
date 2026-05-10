package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import java.io.File

abstract class BuiltinsVirtualFileProvider {
    abstract fun getBuiltinVirtualFiles(): Set<VirtualFile>

    abstract fun createBuiltinsScope(project: Project): GlobalSearchScope

    companion object {
        fun getInstance(): BuiltinsVirtualFileProvider {
            return requireNotNull(
                ApplicationManager.getApplication().getService(BuiltinsVirtualFileProvider::class.java),
            ) {
                "BuiltinsVirtualFileProvider is not registered in the current application container"
            }
        }
    }
}

/**
 * 对位 Kotlin `BuiltinsVirtualFileProviderBaseImpl`。
 *
 * 仓颉 builtins 的真实来源与 Kotlin 不同，
 * 但“由 provider 基类统一收集 binary files，再由具体宿主实现 root 定位”这一 owner 形状保持一致。
 */
abstract class BuiltinsVirtualFileProviderBaseImpl : BuiltinsVirtualFileProvider() {
    protected abstract fun getBuiltinRootVirtualFiles(): Set<VirtualFile>

    override fun getBuiltinVirtualFiles(): Set<VirtualFile> {
        return getBuiltinRootVirtualFiles()
            .flatMapTo(linkedSetOf(), ::collectBuiltinFiles)
    }

    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        return GlobalSearchScope.filesScope(project, getBuiltinVirtualFiles())
    }

    protected fun collectBuiltinFiles(root: VirtualFile): List<VirtualFile> {
        if (!root.isDirectory) {
            return listOfNotNull(root.takeIf(::isBuiltinBinary))
        }

        val files = linkedSetOf<VirtualFile>()
        VfsUtilCore.iterateChildrenRecursively(root, null) { child ->
            if (!child.isDirectory && isBuiltinBinary(child)) {
                files += child
            }
            true
        }
        return files.toList()
    }

    protected fun isBuiltinBinary(file: VirtualFile): Boolean {
        return file.fileType == CangJieBuiltInFileType ||
            file.extension.equals(CangJieBuiltInFileType.defaultExtension, ignoreCase = true)
    }
}

/**
 * 按宿主环境约定解析 builtins `.cjo` 根路径。
 *
 * 与 low-level CFIR 共用：
 * - `cangjie.stdlib.module`
 * - `CANGJIE_STDLIB_MODULE`
 */
class BuiltinsVirtualFileProviderCliImpl : BuiltinsVirtualFileProviderBaseImpl() {
    override fun getBuiltinRootVirtualFiles(): Set<VirtualFile> {
        return readPaths("cangjie.stdlib.module", "CANGJIE_STDLIB_MODULE")
            .mapNotNull { path ->
                StandardFileSystems.local().findFileByPath(path.replace('\\', '/'))
                    ?: run {
                        logger<BuiltinsVirtualFileProviderCliImpl>().warn("Cannot resolve builtins path: $path")
                        null
                    }
            }
            .toCollection(linkedSetOf())
    }

    private fun readPaths(propertyKey: String, envKey: String): List<String> {
        val raw = System.getProperty(propertyKey)
            ?.takeIf(String::isNotBlank)
            ?: System.getenv(envKey)?.takeIf(String::isNotBlank)
            ?: return emptyList()
        return raw.split(File.pathSeparator).filter(String::isNotBlank)
    }
}
