package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import java.io.File
import java.nio.file.Path

/**
 * 提供仓颉 builtins `.cjo` 虚拟文件集合的应用级服务。
 *
 * 该抽象把“内建库位置如何发现”与“反编译、索引、搜索作用域如何消费这些文件”分离；
 * CLI、LSP、IDE 宿主可以各自实现 roots 发现策略，而上层统一通过该 provider 获取文件集合。
 */
abstract class BuiltinsVirtualFileProvider {
    /**
     * 返回当前应用环境可见的 builtins `.cjo` 文件集合。
     */
    abstract fun getBuiltinVirtualFiles(): Set<VirtualFile>

    /**
     * 返回指定项目上下文中可见的 builtins `.cjo` 文件集合。
     *
     * IDE 宿主可以依据 project SDK 或项目模型解析 builtins；CLI 实现通常会退化为应用级路径。
     */
    abstract fun getBuiltinVirtualFiles(project: Project): Set<VirtualFile>

    /**
     * 为指定项目创建只覆盖 builtins `.cjo` 文件的搜索作用域。
     */
    abstract fun createBuiltinsScope(project: Project): GlobalSearchScope

    companion object {
        /**
         * 从 IntelliJ application service 容器中取得已注册的 builtins 文件 provider。
         */
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
    /**
     * 返回当前宿主环境声明的 builtins 根虚拟文件。
     */
    protected abstract fun getBuiltinRootVirtualFiles(): Set<VirtualFile>

    /**
     * 返回指定项目上下文声明的 builtins 根虚拟文件。
     */
    protected abstract fun getBuiltinRootVirtualFiles(project: Project): Set<VirtualFile>

    /**
     * 收集应用级 builtins 根下的所有 `.cjo` 二进制文件。
     */
    override fun getBuiltinVirtualFiles(): Set<VirtualFile> {
        return getBuiltinRootVirtualFiles()
            .flatMapTo(linkedSetOf(), ::collectBuiltinFiles)
    }

    /**
     * 收集项目级 builtins 根下的所有 `.cjo` 二进制文件。
     */
    override fun getBuiltinVirtualFiles(project: Project): Set<VirtualFile> {
        return getBuiltinRootVirtualFiles(project)
            .flatMapTo(linkedSetOf(), ::collectBuiltinFiles)
    }

    /**
     * 基于项目级 builtins 文件集合创建 IntelliJ 搜索作用域。
     */
    override fun createBuiltinsScope(project: Project): GlobalSearchScope {
        return GlobalSearchScope.filesScope(project, getBuiltinVirtualFiles(project))
    }

    /**
     * 从单个 builtins 根收集可反编译的 `.cjo` 文件。
     *
     * 根可以是目录，也可以是一个具体 `.cjo` 文件；目录场景会递归遍历所有子文件。
     */
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

    /**
     * 判断虚拟文件是否是 builtins provider 应当暴露的仓颉二进制。
     */
    protected fun isBuiltinBinary(file: VirtualFile): Boolean {
        return file.fileType == CangJieBuiltInFileType ||
            file.extension.equals(CangJieBuiltInFileType.defaultExtension, ignoreCase = true)
    }

    /**
     * 统一解析宿主传入的本地 stdlib 根路径。
     *
     * CLI/LSP/IDE 都可能在运行期拿到新复制出来的目录，
     * 这里只允许走同一条 refresh-aware VFS 恢复链，避免不同宿主各自出现
     * “属性已设置但 VirtualFile 尚未进入 VFS” 的分叉。
     */
    protected fun resolveLocalRootVirtualFile(path: String): VirtualFile? {
        val normalizedPath = path.replace('\\', '/')
        val localFileSystem = StandardFileSystems.local()
        val nioPath = runCatching { Path.of(path) }.getOrNull()

        return localFileSystem.findFileByPath(normalizedPath)
            ?: localFileSystem.refreshAndFindFileByPath(normalizedPath)
            ?: nioPath?.let(VirtualFileManager.getInstance()::findFileByNioPath)
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
    /**
     * 从系统属性或环境变量中读取 CLI/LSP 宿主暴露的 builtins 根路径并转换为虚拟文件。
     */
    override fun getBuiltinRootVirtualFiles(): Set<VirtualFile> {
        return readPaths("cangjie.stdlib.module", "CANGJIE_STDLIB_MODULE")
            .mapNotNull { path ->
                resolveLocalRootVirtualFile(path)
                    ?: run {
                        logger<BuiltinsVirtualFileProviderCliImpl>().warn("Cannot resolve builtins path: $path")
                        null
                    }
            }
            .toCollection(linkedSetOf())
    }

    /**
     * CLI 实现没有额外 project 维度，直接复用应用级 builtins 根集合。
     */
    override fun getBuiltinRootVirtualFiles(project: Project): Set<VirtualFile> = getBuiltinRootVirtualFiles()

    /**
     * 读取并拆分指定系统属性或环境变量中的本地路径列表。
     *
     * 系统属性优先于环境变量，多个路径使用当前平台的 [File.pathSeparator] 分隔。
     */
    private fun readPaths(propertyKey: String, envKey: String): List<String> {
        val raw = System.getProperty(propertyKey)
            ?.takeIf(String::isNotBlank)
            ?: System.getenv(envKey)?.takeIf(String::isNotBlank)
            ?: return emptyList()
        return raw.split(File.pathSeparator).filter(String::isNotBlank)
    }
}
