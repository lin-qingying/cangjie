package org.cangnova.cangjie.macro.process

import org.cangnova.cangjie.macro.MacroLibraryLoadResult
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File

/**
 * Linux 端到端探针：用仓库的执行器启动 LSPMacroServer 并加载 SDK 自带宏库。
 *
 * Windows 上由 [EnabledOnOs] 跳过。
 */
@EnabledOnOs(OS.LINUX)
class LspMacroServerLinuxProbeTest {
    @Test
    fun loadsSdkMacroLibrary() {
        val sdk = System.getenv("CANGJIE_HOME") ?: error("CANGJIE_HOME 未设置")
        val exe = File(sdk, "tools/bin/LSPMacroServer").path
        val lib = File(sdk, "runtime/lib/linux_x86_64_cjnative/libcangjie-std-deriving.so").path
        check(File(exe).exists()) { "缺少 $exe" }
        check(File(lib).exists()) { "缺少 $lib" }
        val executor = LspMacroServerMacroExecutor(
            macroServerPath = exe,
            sdkHome = sdk,
            processEnvironment = emptyMap(),
        )
        try {
            val result = executor.loadLibraries(listOf(lib))
            println("LINUX_PROBE_RESULT: $result")
            assertTrue(result is MacroLibraryLoadResult.Success, "result=$result")
        } finally {
            executor.close()
        }
    }
}
