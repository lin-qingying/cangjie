package org.cangnova.cangjie.macro.process

import com.sun.jna.Native
import com.sun.jna.platform.win32.Kernel32
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.util.concurrent.TimeUnit

/** 使用真实 Windows Job 与子进程验证回收，不能由 PID 表或模拟 close 替代系统行为。 */
@EnabledOnOs(OS.WINDOWS)
class WindowsMacroProcessJobTest {
    @Test
    fun closingTheOwnedJobTerminatesItsAttachedProcess() {
        val child = ProcessBuilder(checkNotNull(System.getenv("ComSpec")), "/c", "pause").start()
        val processHandle = Kernel32.INSTANCE.OpenProcess(0x0101, false, child.pid().toInt())
            ?: throw com.sun.jna.LastErrorException(Native.getLastError())
        val job = WindowsMacroProcessJob()
        try {
            assertTrue(child.isAlive)
            job.attach(processHandle)
            job.close()
            assertTrue(child.waitFor(10, TimeUnit.SECONDS), "Job 关闭后真实子进程必须退出")
            assertFalse(child.isAlive)
            job.close()
        } finally {
            job.close()
            Kernel32.INSTANCE.CloseHandle(processHandle)
            if (child.isAlive) child.destroyForcibly()
            child.inputStream.close()
            child.errorStream.close()
            child.outputStream.close()
        }
    }
}
