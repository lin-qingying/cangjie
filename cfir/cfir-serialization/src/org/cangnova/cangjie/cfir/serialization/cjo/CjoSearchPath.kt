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
    private val envProvider: (String) -> String? = System::getenv,
) {
    private val stdlibSearchPaths: List<File> by lazy { readPaths("CANGJIE_STDLIB_MODULE") }
    private val librarySearchPaths: List<File> by lazy { readPaths("CANGJIE_LIBRARY") }

    private val directoryIndexCache = ConcurrentHashMap<File, Map<String, File>>()
    private val resolvedByPackage = ConcurrentHashMap<String, File>()
    private val missingPackages = ConcurrentHashMap.newKeySet<String>()

    private fun readPaths(envName: String): List<File> {
        return envProvider(envName)
            ?.split(File.pathSeparator)
            ?.map(::File)
            ?.filter { it.isDirectory }
            .orEmpty()
    }

    private fun searchPathsFor(fullPkgName: String): List<File> {
        return if (isStdlibPackage(fullPkgName)) {
            stdlibSearchPaths + librarySearchPaths
        } else {
            librarySearchPaths
        }
    }

    private fun isStdlibPackage(fullPkgName: String): Boolean {
        return fullPkgName == "std" || fullPkgName.startsWith("std.")
    }

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

    fun getAvailablePackageNames(): Set<String> {
        return buildSet {
            for (root in stdlibSearchPaths + librarySearchPaths) {
                addAll(indexDirectoryByHeader(root).keys)
            }
            addAll(resolvedByPackage.keys)
        }
    }

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

    private fun readPackageNameFromHeader(file: File): String? {
        return runCatching {
            val pkg = Package.getRootAsPackage(ByteBuffer.wrap(file.readBytes()))
            pkg.fullPkgName?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
