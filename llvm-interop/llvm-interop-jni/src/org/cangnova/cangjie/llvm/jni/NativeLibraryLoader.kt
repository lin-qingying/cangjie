package org.cangnova.cangjie.llvm.jni

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.outputStream

internal data class NativeLoadResult(
    val loaded: Boolean,
    val diagnostics: String,
    val cause: Throwable? = null,
)

internal class NativeLibraryLoader(
    private val loadAbsolute: (String) -> Unit,
    private val loadByName: (String) -> Unit,
    private val resourceOpener: (String) -> InputStream?,
    private val tempDirProvider: () -> Path,
) {
    fun load(): NativeLoadResult {
        val diagnostics = mutableListOf<String>()

        val propertyPath = System.getProperty("cangjie.llvm.native.library.path")
        if (!propertyPath.isNullOrBlank()) {
            try {
                loadAbsolute(propertyPath)
                return NativeLoadResult(true, "loaded from system property: $propertyPath")
            } catch (e: Throwable) {
                diagnostics += "property path failed: $propertyPath (${e.message})"
            }
        }

        val platformId = runCatching { PlatformDetector.detect() }.getOrElse { error ->
            return NativeLoadResult(false, "platform detection failed: ${error.message}", error)
        }
        val fileName = libraryFileName(platformId)
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
    }
}
