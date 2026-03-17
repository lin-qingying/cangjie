package org.cangnova.cangjie.cfir.serialization.cjo

import java.io.File

/**
 * .cjo 文件搜索路径配置。
 *
 * 仅保留两种加载方式：
 * 1. `CANGJIE_STDLIB_MODULE`：仅用于标准库包（`std` / `std.*`）
 * 2. `CANGJIE_LIBRARY`：可用于任意包
 */
class CjoSearchPath(
    private val envProvider: (String) -> String? = System::getenv,
) {
    private val stdlibSearchPaths: List<File> by lazy { readPaths("CANGJIE_STDLIB_MODULE") }
    private val librarySearchPaths: List<File> by lazy { readPaths("CANGJIE_LIBRARY") }

    private fun readPaths(envName: String): List<File> {
        return envProvider(envName)?.split(File.pathSeparator)
            ?.map { File(it) }
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

    /**
     * 在搜索路径中查找指定包名对应的 .cjo 文件。
     * @return 找到的 .cjo 文件，未找到返回 null
     */
    fun findCjoFile(fullPkgName: String): File? {
        val relativePath = org.cangnova.cangjie.cfir.serialization.CjoConstants.packageNameToPath(fullPkgName)
        for (dir in searchPathsFor(fullPkgName)) {
            val candidate = File(dir, relativePath)
            if (candidate.isFile) return candidate
        }
        return null
    }
}
