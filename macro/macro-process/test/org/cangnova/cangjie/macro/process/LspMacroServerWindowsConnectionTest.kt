package org.cangnova.cangjie.macro.process

import com.sun.jna.Native
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.ptr.IntByReference
import org.cangnova.cangjie.macro.protocol.MacroMsgCodec
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

/** 使用真实 SDK 与 Win32 管道验证公共启动、继承边界和退出意图。 */
@EnabledOnOs(OS.WINDOWS)
@Timeout(30)
class LspMacroServerWindowsConnectionTest {
    @Test
    fun officialDefLibRoundTripAndRequestedExitRemainObservable() {
        val records = CopyOnWriteArrayList<LogRecord>()
        val connection = start(records)
        val executor = Executors.newSingleThreadExecutor { Thread(it, "native macro ack").apply { isDaemon = true } }
        try {
            assertTrue(connection.isAlive())
            assertTrue(connection.processId > 0)
            connection.transport.send(MacroMsgCodec.buildDefLib(emptyList()))
            val ack = executor.submit<ByteArray> { connection.transport.receive() }.get(10, TimeUnit.SECONDS)
            assertEquals("RespondFindDef ", ack.toString(Charsets.UTF_8))
            connection.beginClose()
            connection.transport.send(MacroMsgCodec.buildExitTask())
            connection.finishCloseRequest(true)
            connection.close()
            connection.close()
            assertFalse(connection.isAlive())
            assertTrue(records.any { it.message.contains("shutdownRequested=true") })
            assertFalse(records.any { it.level.intValue() >= Level.SEVERE.intValue() }, records.map { it.message }.toString())
        } finally {
            if (connection.isAlive()) connection.terminate() else connection.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun liveServerDoesNotInheritAnUnrelatedParentPipe() {
        val attributes = WinBase.SECURITY_ATTRIBUTES().apply {
            dwLength = DWORD(size().toLong())
            bInheritHandle = true
        }
        val read = WinNT.HANDLEByReference()
        val write = WinNT.HANDLEByReference()
        assertTrue(Kernel32.INSTANCE.CreatePipe(read, write, attributes, 0))
        val connection = start(CopyOnWriteArrayList())
        val executor = Executors.newSingleThreadExecutor { Thread(it, "unrelated pipe EOF").apply { isDaemon = true } }
        var writeClosed = false
        try {
            assertTrue(connection.isAlive())
            assertTrue(Kernel32.INSTANCE.CloseHandle(write.value))
            writeClosed = true
            val observed = executor.submit<Pair<Boolean, Int>> {
                val success = Kernel32.INSTANCE.ReadFile(read.value, ByteArray(1), 1, IntByReference(), null)
                success to Native.getLastError()
            }.get(3, TimeUnit.SECONDS)
            assertFalse(observed.first)
            assertEquals(109, observed.second, "The live child must not keep the unrelated pipe writer open")
            assertTrue(connection.isAlive())
        } finally {
            connection.terminate()
            if (!writeClosed) Kernel32.INSTANCE.CloseHandle(write.value)
            Kernel32.INSTANCE.CloseHandle(read.value)
            executor.shutdownNow()
        }
    }

    @Test
    fun anUnrequestedEarlyZeroExitIsStillSevere() {
        val records = CopyOnWriteArrayList<LogRecord>()
        val connection = start(records)
        try {
            // 未登记 owner 关闭而直接发退出帧：实际零退出仍必须报告为非预期退出。
            connection.transport.send(MacroMsgCodec.buildExitTask())
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (records.none { it.message.startsWith("LSPMacroServer exited ") } && System.nanoTime() < deadline) {
                Thread.sleep(25)
            }
            assertFalse(connection.isAlive())
            val exit = records.single { it.message.startsWith("LSPMacroServer exited ") }
            assertEquals(Level.SEVERE, exit.level)
            assertTrue(exit.message.contains("exitCode=0"), exit.message)
            assertTrue(exit.message.contains("shutdownRequested=false"), exit.message)
        } finally {
            connection.close()
        }
    }

    private fun start(
        records: CopyOnWriteArrayList<LogRecord>,
    ): LspMacroServerProcess {
        val sdk = File(checkNotNull(System.getenv("CANGJIE_HOME")) { "Real CANGJIE_HOME is required" })
        val server = File(sdk, "tools/bin/LSPMacroServer.exe")
        assertTrue(server.isFile)
        assertTrue(File(sdk, "bin/cjc.exe").isFile)
        val directories = listOf(File(sdk, "bin"), File(sdk, "tools/bin")) +
            listOf(File(sdk, "runtime/lib"), File(sdk, "lib")).flatMap { root ->
                root.listFiles()?.filter(File::isDirectory).orEmpty()
            }
        val environment = System.getenv().toMutableMap()
        val pathKey = environment.keys.single { it.equals("PATH", ignoreCase = true) }
        environment[pathKey] = (directories.map(File::getAbsolutePath) + environment.getValue(pathKey))
            .joinToString(File.pathSeparator)
        val logger = Logger.getAnonymousLogger().apply {
            useParentHandlers = false
            addHandler(object : Handler() {
                override fun publish(record: LogRecord) { records += record }
                override fun flush() {}
                override fun close() {}
            })
        }
        return LspMacroServerProcess.start(server, sdk.absolutePath, environment, true, logger)
    }
}
