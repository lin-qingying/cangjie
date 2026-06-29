package org.cangnova.cangjie.llvm.jni

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.outputStream
import kotlin.streams.toList

/**
 * 原生库加载结果。
 */
internal data class NativeLoadResult(
    /**
     * 原生库是否成功加载。
     */
    val loaded: Boolean,
    /**
     * 加载成功来源或失败诊断信息。
     */
    val diagnostics: String,
    /**
     * 导致加载失败的底层异常。
     */
    val cause: Throwable? = null,
)

/**
 * LLVM JNI 动态库加载器。
 *
 * 按“显式路径 -> native home -> classpath 资源 -> 系统库名”顺序尝试加载。
 */
internal class NativeLibraryLoader(
    /**
     * 按绝对路径加载动态库的函数。
     */
    private val loadAbsolute: (String) -> Unit,
    /**
     * 按系统库名称加载动态库的函数。
     */
    private val loadByName: (String) -> Unit,
    /**
     * 打开 classpath 原生库资源的函数。
     */
    private val resourceOpener: (String) -> InputStream?,
    /**
     * 创建临时解压目录的函数。
     */
    private val tempDirProvider: () -> Path,
) {
    /**
     * 按优先级尝试加载 LLVM JNI 原生库。
     */
    fun load(): NativeLoadResult {
        val diagnostics = mutableListOf<String>()

        val platformId = runCatching { PlatformDetector.detect() }.getOrElse { error ->
            return NativeLoadResult(false, "platform detection failed: ${error.message}", error)
        }
        val fileName = libraryFileName(platformId)

        val propertyPath = System.getProperty(PROPERTY_NATIVE_LIBRARY_PATH)
        if (!propertyPath.isNullOrBlank()) {
            try {
                val mainLibrary = Paths.get(propertyPath).toAbsolutePath()
                preloadDependencies(mainLibrary.parent, mainLibrary.fileName.toString())
                loadAbsolute(mainLibrary.toString())
                return NativeLoadResult(true, "loaded from system property: $propertyPath")
            } catch (e: Throwable) {
                diagnostics += "property path failed: $propertyPath (${e.message})"
            }
        }

        resolveNativeHome(platformId)?.let { platformDir ->
            val mainLibrary = platformDir.resolve(fileName)
            try {
                if (mainLibrary.isRegularFile()) {
                    preloadDependencies(platformDir, fileName)
                    loadAbsolute(mainLibrary.toAbsolutePath().toString())
                    return NativeLoadResult(
                        true,
                        "loaded from native home directory: ${mainLibrary.toAbsolutePath()}",
                    )
                }
                diagnostics += "native home missing library: ${mainLibrary.toAbsolutePath()}"
            } catch (e: Throwable) {
                diagnostics += "native home load failed: ${mainLibrary.toAbsolutePath()} (${e.message})"
            }
        }

        val resourcePath = "/native/$platformId/$fileName"
        try {
            val stream = resourceOpener(resourcePath)
            if (stream != null) {
                val tempDir = tempDirProvider()
                val extracted = tempDir.resolve(fileName)
                stream.use { input ->
                    extracted.outputStream().use { output -> input.copyTo(output) }
                }
                loadAbsolute(extracted.toAbsolutePath().toString())
                return NativeLoadResult(true, "loaded from classpath resource: $resourcePath")
            }
            diagnostics += "classpath resource not found: $resourcePath"
        } catch (e: Throwable) {
            diagnostics += "classpath resource load failed: $resourcePath (${e.message})"
        }

        try {
            loadByName("cangjie_llvm_jni")
            return NativeLoadResult(true, "loaded from system library path by name: cangjie_llvm_jni")
        } catch (e: Throwable) {
            diagnostics += "system library load failed: cangjie_llvm_jni (${e.message})"
        }

        val message = buildString {
            append("Failed to load native LLVM JNI library. Attempts:\n")
            diagnostics.forEach { append("- ").append(it).append('\n') }
        }.trim()
        return NativeLoadResult(false, message)
    }

    /**
     * 根据平台标识解析 Cangjie native home 中的原生库目录。
     */
    private fun resolveNativeHome(platformId: String): Path? {
        val configuredHome = System.getProperty(PROPERTY_NATIVE_HOME)
            ?: System.getenv(ENV_CANGJIE_HOME)
            ?: return null

        val root = Paths.get(configuredHome).toAbsolutePath()
        if (root.name == platformId && root.isDirectory()) return root

        val platformPath = root.resolve("native").resolve(platformId)
        return if (platformPath.isDirectory()) platformPath else root.takeIf { it.isDirectory() }
    }

    /**
     * 预加载主库所在目录中的依赖动态库。
     */
    private fun preloadDependencies(directory: Path?, mainFileName: String) {
        if (directory == null || !directory.isDirectory()) return
        val explicitOrder = directory.resolve(DEPS_ORDER_FILE)
        val dependencies = if (explicitOrder.isRegularFile()) {
            explicitOrder.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && it != mainFileName }
                .map { directory.resolve(it) }
        } else {
            Files.list(directory).use { entries ->
                entries
                    .filter { it.isRegularFile() }
                    .filter { it.fileName.toString() != mainFileName }
                    .filter { isDynamicLibraryFile(it.fileName.toString()) }
                    .sorted { a, b -> a.fileName.toString().compareTo(b.fileName.toString()) }
                    .toList()
            }
        }

        dependencies
            .filter { it.isRegularFile() }
            .let { ordered ->
                val pending = ordered.toMutableList()
                var lastError: Throwable? = null
                var progressed = true
                while (pending.isNotEmpty() && progressed) {
                    progressed = false
                    val iterator = pending.iterator()
                    while (iterator.hasNext()) {
                        val candidate = iterator.next()
                        runCatching { loadAbsolute(candidate.toAbsolutePath().toString()) }
                            .onSuccess {
                                iterator.remove()
                                progressed = true
                            }
                            .onFailure { error -> lastError = error }
                    }
                }
                if (pending.isNotEmpty()) {
                    throw UnsatisfiedLinkError(
                        "failed to preload dependencies: ${
                            pending.joinToString { it.fileName.toString() }
                        } (${lastError?.message})",
                    )
                }
            }
    }

    /**
     * 判断文件名是否为当前加载器支持的动态库文件。
     */
    private fun isDynamicLibraryFile(fileName: String): Boolean {
        return fileName.endsWith(".dll", ignoreCase = true)
            || fileName.endsWith(".so", ignoreCase = true)
            || fileName.endsWith(".dylib", ignoreCase = true)
    }

    /**
     * 根据平台标识返回主 JNI 动态库文件名。
     */
    private fun libraryFileName(platformId: String): String {
        return when {
            platformId.startsWith("windows-") -> "cangjie_llvm_jni.dll"
            platformId.startsWith("macos-") -> "libcangjie_llvm_jni.dylib"
            else -> "libcangjie_llvm_jni.so"
        }
    }

    /**
     * 默认加载器工厂和配置常量。
     */
    companion object {
        /**
         * 创建使用 JVM 系统加载函数和 classpath 资源的默认加载器。
         */
        fun default(): NativeLibraryLoader {
            return NativeLibraryLoader(
                loadAbsolute = System::load,
                loadByName = System::loadLibrary,
                resourceOpener = { NativeLibraryLoader::class.java.getResourceAsStream(it) },
                tempDirProvider = { Files.createTempDirectory("cangjie-llvm-jni") },
            )
        }

        /**
         * 显式原生库文件路径的系统属性。
         */
        private const val PROPERTY_NATIVE_LIBRARY_PATH = "cangjie.llvm.native.library.path"
        /**
         * Cangjie native home 的系统属性。
         */
        private const val PROPERTY_NATIVE_HOME = "cangjie.native.home"
        /**
         * Cangjie native home 的环境变量。
         */
        private const val ENV_CANGJIE_HOME = "CANGJIE_HOME"
        /**
         * 依赖动态库显式加载顺序文件。
         */
        private const val DEPS_ORDER_FILE = "deps.order"
    }
}
