package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.name.FqName
import java.io.File

/**
 * 生产级宏包 artifact 发现器。
 *
 * 这里只负责按官方文件命名和搜索路径找到 `.cjo + 动态库`；artifact 内容、
 * 包名、ABI 和导出宏定义仍统一交给 [MacroArtifactResolver] 校验。
 */
class MacroArtifactLocator(
    private val sdkHome: String = DEFAULT_MACRO_SDK_HOME,
    private val host: String = defaultCangjieLibHost(),
) {
    fun locate(
        packageDemands: Set<FqName>,
        searchRoots: List<String>,
        explicitArtifacts: List<MacroArtifactPackage> = emptyList(),
    ): List<MacroArtifactPackage> {
        if (packageDemands.isEmpty()) return emptyList()

        val result = linkedMapOf<FqName, MacroArtifactPackage>()
        explicitArtifacts
            .filter { it.packageFqName in packageDemands }
            .forEach { result.putIfAbsent(it.packageFqName, it) }

        val roots = searchRoots
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(::File)
            .filter(File::exists)
            .distinctBy { it.absoluteFile.normalize().path }

        for (packageFqName in packageDemands) {
            if (packageFqName in result) continue
            val sdkArtifact = locateFromSdk(packageFqName)
            if (sdkArtifact != null) {
                result[packageFqName] = sdkArtifact
            } else {
                locateFromRoots(packageFqName, roots)?.let { result[packageFqName] = it }
            }
        }

        return result.values.toList()
    }

    fun locateFromRoots(packageFqName: FqName, roots: List<File>): MacroArtifactPackage? {
        val cjoPath = findCjo(packageFqName, roots) ?: return null
        val dynamicLibPath = findMacroDynamicLibrary(packageFqName, roots) ?: return null
        return MacroArtifactPackage(
            packageFqName = packageFqName,
            kind = MacroArtifactPackage.Kind.MACRO,
            cjoPath = cjoPath.path,
            dynamicLibPath = dynamicLibPath.path,
            origin = MacroArtifactPackage.Origin.EXTERNAL_PATH,
        )
    }

    private fun locateFromSdk(packageFqName: FqName): MacroArtifactPackage? {
        if (toLibCangjieBaseName(packageFqName) == null) return null
        val modulesRoot = File(sdkHome, "modules/$host")
        val runtimeRoot = File(sdkHome, "runtime/lib/$host")
        val cjoPath = findCjo(packageFqName, listOf(modulesRoot)) ?: return null
        val dynamicLibPath = findStdDynamicLibrary(packageFqName, runtimeRoot) ?: return null
        return MacroArtifactPackage(
            packageFqName = packageFqName,
            kind = MacroArtifactPackage.Kind.MACRO,
            cjoPath = cjoPath.path,
            dynamicLibPath = dynamicLibPath.path,
            origin = MacroArtifactPackage.Origin.SDK_STDLIB,
        )
    }

    private fun findCjo(packageFqName: FqName, roots: List<File>): File? {
        val cjoName = "${toCjoFileName(packageFqName)}.cjo"
        val firstSegment = packageFqName.firstSegment()?.asString()
        for (root in roots) {
            if (firstSegment != null) {
                root.resolve(firstSegment).resolve(cjoName).takeIf(File::isFile)?.let { return it }
            }
            root.resolve(cjoName).takeIf(File::isFile)?.let { return it }
        }
        return null
    }

    private fun findMacroDynamicLibrary(packageFqName: FqName, roots: List<File>): File? {
        val libraryName = "lib-macro_${toCjoFileName(packageFqName)}.${dynamicLibraryExtension()}"
        val firstSegment = packageFqName.firstSegment()?.asString()
        for (root in roots) {
            if (firstSegment != null) {
                root.resolve(firstSegment).resolve(libraryName).takeIf(File::isFile)?.let { return it }
            }
            root.resolve(libraryName).takeIf(File::isFile)?.let { return it }
        }
        return null
    }

    private fun findStdDynamicLibrary(packageFqName: FqName, runtimeRoot: File): File? {
        val baseName = toLibCangjieBaseName(packageFqName) ?: return null
        val libraryName = "lib$baseName.${dynamicLibraryExtension()}"
        return runtimeRoot.resolve(libraryName).takeIf(File::isFile)
    }
}

fun toCjoFileName(packageFqName: FqName): String {
    val text = packageFqName.asString()
    val index = text.indexOf("::")
    return if (index >= 0) {
        text.substring(index + 2) + "@" + text.substring(0, index)
    } else {
        text
    }
}

private fun toLibCangjieBaseName(packageFqName: FqName): String? {
    val text = packageFqName.asString()
    val stdlibName = standardLibraryPackages[text] ?: return null
    val firstDot = text.indexOf('.')
    return if (firstDot < 0) {
        "cangjie-$stdlibName"
    } else {
        "cangjie-${text.substring(0, firstDot)}-$stdlibName"
    }
}

/**
 * 官方 `STANDARD_LIBS` 映射的 Kotlin 投影。
 *
 * std 动态库必须走 `libcangjie-*`，但只有官方 stdlib map 中的包允许进入
 * SDK runtime/lib 查找；普通宏包即使以相似名字出现也不能被当作 std 宏库。
 */
private val standardLibraryPackages: Map<String, String> = linkedMapOf(
    "std.core" to "core",
    "std.binary" to "binary",
    "std.io" to "io",
    "std.math" to "math",
    "std.overflow" to "overflow",
    "std.runtime" to "runtime",
    "std.convert" to "convert",
    "std.random" to "random",
    "std.collection" to "collection",
    "std.unicode" to "unicode",
    "std.sort" to "sort",
    "std.argopt" to "argopt",
    "std.ast" to "ast",
    "std.interop" to "interop",
    "std.time" to "time",
    "std.sync" to "sync",
    "std.collection.concurrent" to "collection.concurrent",
    "std.net" to "net",
    "std.regex" to "regex",
    "std.unittest.common" to "unittest.common",
    "std.unittest.prop_test" to "unittest.prop_test",
    "std.unittest.diff" to "unittest.diff",
    "std.math.numeric" to "math.numeric",
    "std.fs" to "fs",
    "std.unittest.mock.internal" to "unittest.mock.internal",
    "std.unittest.mock" to "unittest.mock",
    "std.reflect" to "reflect",
    "std.ref" to "ref",
    "std.crypto" to "crypto",
    "std.crypto.digest" to "crypto.digest",
    "std.crypto.cipher" to "crypto.cipher",
    "std.console" to "console",
    "std.database" to "database",
    "std.database.sql" to "database.sql",
    "std.posix" to "posix",
    "std.process" to "process",
    "std.env" to "env",
    "std.objectpool" to "objectpool",
    "std.unittest" to "unittest",
    "std.deriving.api" to "deriving.api",
    "std.deriving.resolve" to "deriving.resolve",
    "std.deriving.impl" to "deriving.impl",
    "std.deriving.builtins" to "deriving.builtins",
    "std.deriving" to "deriving",
    "std.unittest.testmacro" to "unittest.testmacro",
    "std.unittest.mock.mockmacro" to "unittest.mock.mockmacro",
    "std.ffi.python" to "ffi.python",
    "fuzz.fuzz" to "fuzz",
    "std" to "std",
)

internal fun dynamicLibraryExtension(): String =
    when {
        System.getProperty("os.name").contains("Windows", ignoreCase = true) -> "dll"
        System.getProperty("os.name").contains("Mac", ignoreCase = true) -> "dylib"
        else -> "so"
    }

private fun defaultCangjieLibHost(): String =
    when {
        System.getProperty("os.name").contains("Windows", ignoreCase = true) -> "windows_x86_64_cjnative"
        System.getProperty("os.name").contains("Mac", ignoreCase = true) -> "darwin_x86_64_cjnative"
        else -> "linux_x86_64_cjnative"
    }
