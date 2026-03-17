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
    val loaded: Boolean,
    val diagnostics: String,
    val cause: Throwable? = null,
)

/**
 * LLVM JNI 动态库加载器。
 *
 * 按“显式路径 -> native home -> classpath 资源 -> 系统库名”顺序尝试加载。
 */
internal class NativeLibraryLoader(
    private val loadAbsolute: (String) -> Unit,
    private val loadByName: (String) -> Unit,
    private val resourceOpener: (String) -> InputStream?,
    private val tempDirProvider: () -> Path,
) {
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

    private fun resolveNativeHome(platformId: String): Path? {
        val configuredHome = System.getProperty(PROPERTY_NATIVE_HOME)
            ?: System.getenv(ENV_CANGJIE_HOME)
            ?: return null

        val root = Paths.get(configuredHome).toAbsolutePath()
        if (root.name == platformId && root.isDirectory()) return root

        val platformPath = root.resolve("native").resolve(platformId)
        return if (platformPath.isDirectory()) platformPath else root.takeIf { it.isDirectory() }
    }

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

    private fun isDynamicLibraryFile(fileName: String): Boolean {
        return fileName.endsWith(".dll", ignoreCase = true)
            || fileName.endsWith(".so", ignoreCase = true)
            || fileName.endsWith(".dylib", ignoreCase = true)
    }

    private fun libraryFileName(platformId: String): String {
        return when {
            platformId.startsWith("windows-") -> "cangjie_llvm_jni.dll"
            platformId.startsWith("macos-") -> "libcangjie_llvm_jni.dylib"
            else -> "libcangjie_llvm_jni.so"
        }
    }

    companion object {
        fun default(): NativeLibraryLoader {
            return NativeLibraryLoader(
                loadAbsolute = System::load,
                loadByName = System::loadLibrary,
                resourceOpener = { NativeLibraryLoader::class.java.getResourceAsStream(it) },
                tempDirProvider = { Files.createTempDirectory("cangjie-llvm-jni") },
            )
        }

        private const val PROPERTY_NATIVE_LIBRARY_PATH = "cangjie.llvm.native.library.path"
        private const val PROPERTY_NATIVE_HOME = "cangjie.native.home"
        private const val ENV_CANGJIE_HOME = "CANGJIE_HOME"
        private const val DEPS_ORDER_FILE = "deps.order"
    }
}
