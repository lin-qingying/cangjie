package org.cangnova.cangjie.llvm.jni

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 平台检测器测试。
 */
class PlatformDetectorTest {
    @Test
    fun `maps linux amd64 to linux-x86_64`() {
        assertEquals("linux-x86_64", PlatformDetector.detect("Linux", "amd64"))
    }

    @Test
    fun `maps linux aarch64 to linux-aarch64`() {
        assertEquals("linux-aarch64", PlatformDetector.detect("Linux", "aarch64"))
    }

    @Test
    fun `maps mac os x arm64 to macos-aarch64`() {
        assertEquals("macos-aarch64", PlatformDetector.detect("Mac OS X", "arm64"))
    }

    @Test
    fun `maps windows amd64 to windows-x86_64`() {
        assertEquals("windows-x86_64", PlatformDetector.detect("Windows 11", "amd64"))
    }

    @Test
    fun `throws on unsupported platform`() {
        assertThrows<IllegalStateException> {
            PlatformDetector.detect("FreeBSD", "amd64")
        }
    }
}
