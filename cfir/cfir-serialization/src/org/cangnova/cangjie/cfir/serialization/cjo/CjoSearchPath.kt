package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.Package
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * `.cjo` 搜索路径配置。
 *
 * 搜索顺序：
 * 1. 优先按包头里的 `fullPkgName` 建立索引
 * 2. 回退到遗留路径约定
 */
class CjoSearchPath(
    /** 环境变量读取函数，测试可注入以隔离宿主环境。 */
    private val envProvider: (String) -> String? = System::getenv,
) {
    /** 标准库 `.cjo` 搜索根目录，来自 `CANGJIE_STDLIB_MODULE`。 */
    private val stdlibSearchPaths: List<File> by lazy { readPaths("CANGJIE_STDLIB_MODULE") }
    /** 普通库 `.cjo` 搜索根目录，来自 `CANGJIE_LIBRARY`。 */
    private val librarySearchPaths: List<File> by lazy { readPaths("CANGJIE_LIBRARY") }

    /** 每个根目录按包头 fullPkgName 建出的索引缓存。 */
    private val directoryIndexCache = ConcurrentHashMap<File, Map<String, File>>()
    /** 已成功解析的完整包名到 `.cjo` 文件缓存。 */
    private val resolvedByPackage = ConcurrentHashMap<String, File>()
    /** 已确认缺失的完整包名集合。 */
    private val missingPackages = ConcurrentHashMap.newKeySet<String>()

    /** 从指定环境变量读取并过滤存在的目录路径。 */
    private fun readPaths(envName: String): List<File> {
        return envProvider(envName)
            ?.split(File.pathSeparator)
            ?.map(::File)
            ?.filter { it.isDirectory }
            .orEmpty()
    }

    /** 根据包名选择搜索根目录；标准库包优先查标准库路径。 */
    private fun searchPathsFor(fullPkgName: String): List<File> {
        return if (isStdlibPackage(fullPkgName)) {
            stdlibSearchPaths + librarySearchPaths
        } else {
            librarySearchPaths
        }
    }

    /** 判断完整包名是否属于 `std` 标准库命名空间。 */
    private fun isStdlibPackage(fullPkgName: String): Boolean {
        return fullPkgName == "std" || fullPkgName.startsWith("std.")
    }

    /**
     * 查找指定完整包名对应的 `.cjo` 文件。
     *
     * 优先使用包头索引，找不到时回退到旧的包名转路径约定。
     */
    fun findCjoFile(fullPkgName: String): File? {
        resolvedByPackage[fullPkgName]?.let { return it }
        if (fullPkgName in missingPackages) return null

        val searchPaths = searchPathsFor(fullPkgName)

        for (dir in searchPaths) {
            val indexed = indexDirectoryByHeader(dir)[fullPkgName] ?: continue
            resolvedByPackage.putIfAbsent(fullPkgName, indexed)
            missingPackages.remove(fullPkgName)
            return resolvedByPackage[fullPkgName] ?: indexed
        }

        val legacyRelativePath = CjoConstants.packageNameToPath(fullPkgName)
        for (dir in searchPaths) {
            val candidate = File(dir, legacyRelativePath)
            if (!candidate.isFile) continue
            resolvedByPackage.putIfAbsent(fullPkgName, candidate)
            missingPackages.remove(fullPkgName)
            return resolvedByPackage[fullPkgName] ?: candidate
        }

        missingPackages += fullPkgName
        return null
    }

    /** 枚举当前搜索根目录与解析缓存中已知的包名。 */
    fun getAvailablePackageNames(): Set<String> {
        return buildSet {
            for (root in stdlibSearchPaths + librarySearchPaths) {
                addAll(indexDirectoryByHeader(root).keys)
            }
            addAll(resolvedByPackage.keys)
        }
    }

    /**
     * 扫描根目录下所有 `.cjo` 文件，并按包头 fullPkgName 建索引。
     *
     * 文件按相对路径排序，确保同名包冲突时索引结果稳定。
     */
    private fun indexDirectoryByHeader(root: File): Map<String, File> {
        return directoryIndexCache.getOrPut(root) {
            if (!root.isDirectory) return@getOrPut emptyMap()

            val files = root.walkTopDown()
                .filter { it.isFile && it.extension.equals("cjo", ignoreCase = true) }
                .toList()
                .sortedBy { it.relativeTo(root).invariantSeparatorsPath }

            val index = LinkedHashMap<String, File>()
            for (file in files) {
                val packageName = readPackageNameFromHeader(file) ?: continue
                index.putIfAbsent(packageName, file)
            }
            index
        }
    }

    /** 从 `.cjo` 文件包头读取 fullPkgName；读取失败或空包名时返回 null。 */
    private fun readPackageNameFromHeader(file: File): String? {
        return runCatching {
            val pkg = Package.getRootAsPackage(ByteBuffer.wrap(file.readBytes()))
            pkg.fullPkgName?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
