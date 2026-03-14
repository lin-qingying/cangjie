package org.cangnova.cangjie.llvm.jni

object PlatformDetector {
    fun detect(
        osName: String = System.getProperty("os.name"),
        osArch: String = System.getProperty("os.arch"),
    ): String {
        val os = when {
            osName.startsWith("Linux", ignoreCase = true) -> "linux"
            osName.startsWith("Mac", ignoreCase = true) -> "macos"
            osName.startsWith("Windows", ignoreCase = true) -> "windows"
            else -> throw IllegalStateException("Unsupported platform: $osName-$osArch")
        }
        val arch = when (osArch.lowercase()) {
            "amd64", "x86_64" -> "x86_64"
            "aarch64", "arm64" -> "aarch64"
            else -> throw IllegalStateException("Unsupported platform: $osName-$osArch")
        }
        return "$os-$arch"
    }
}
