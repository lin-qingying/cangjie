package org.cangnova.cangjie.macro.process

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.WString
import com.sun.jna.platform.win32.BaseTSD
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

/**
 * 将本次启动拥有的五个子端点作为 HANDLE_LIST 传入 CreateProcessW。
 *
 * 仅打开 bInheritHandles 会把其它宏连接、IDE 和测试 worker 的可继承句柄一并传给子进程。
 * 显式白名单与 STARTUPINFOEX 同寿命，父端和 Job 句柄始终留在当前 JVM。
 */
internal class WindowsMacroProcessStartup(childHandles: List<HANDLE>) : AutoCloseable {
    private val api = Native.load("kernel32", StartupApi::class.java, W32APIOptions.DEFAULT_OPTIONS)
    private val handleList: Memory
    private val attributeList: Memory
    private var closed = false

    init {
        require(childHandles.size == 5 && childHandles.distinct().size == childHandles.size)
        val handles = Memory(childHandles.size.toLong() * Native.POINTER_SIZE)
        var attributes: Memory? = null
        var initialized = false
        try {
            childHandles.forEachIndexed { index, handle ->
                handles.setPointer(index.toLong() * Native.POINTER_SIZE, handle.pointer)
            }
            val bytes = Memory(Native.POINTER_SIZE.toLong()).use { size ->
                size.clear()
                val queried = api.InitializeProcThreadAttributeList(null, 1, 0, size)
                val error = Native.getLastError()
                check(!queried && error == 122) { "InitializeProcThreadAttributeList(size) failed: $error" }
                if (Native.POINTER_SIZE == 8) size.getLong(0) else size.getInt(0).toLong() and 0xFFFFFFFFL
            }
            check(bytes > 0)
            val list = Memory(bytes)
            attributes = list
            Memory(Native.POINTER_SIZE.toLong()).use { size ->
                if (Native.POINTER_SIZE == 8) size.setLong(0, bytes) else size.setInt(0, bytes.toInt())
                if (!api.InitializeProcThreadAttributeList(list, 1, 0, size)) {
                    throw com.sun.jna.LastErrorException(Native.getLastError())
                }
            }
            initialized = true
            if (!api.UpdateProcThreadAttribute(list, 0, BaseTSD.ULONG_PTR(0x00020002L),
                    handles, BaseTSD.SIZE_T(handles.size()), null, null)) {
                throw com.sun.jna.LastErrorException(Native.getLastError())
            }
            handleList = handles
            attributeList = list
        } catch (failure: Throwable) {
            fun release(action: () -> Unit) {
                try { action() } catch (error: Throwable) { failure.addSuppressed(error) }
            }
            attributes?.let { list ->
                if (initialized) release { api.DeleteProcThreadAttributeList(list) }
                release(list::close)
            }
            release(handles::close)
            throw failure
        }
    }

    /** 子进程先挂起，调用方将真实 PROCESS_INFORMATION 登记进 Job 后再恢复主线程。 */
    fun createProcess(
        commandLine: String,
        environment: Memory,
        directory: String,
        stdin: HANDLE,
        stdout: HANDLE,
        stderr: HANDLE,
        processInfo: WinBase.PROCESS_INFORMATION,
    ) {
        check(!closed)
        val startup = ExtendedStartupInfo()
        startup.StartupInfo.cb = DWORD(startup.size().toLong())
        startup.StartupInfo.dwFlags = 0x00000100 // STARTF_USESTDHANDLES
        startup.StartupInfo.hStdInput = stdin
        startup.StartupInfo.hStdOutput = stdout
        startup.StartupInfo.hStdError = stderr
        startup.lpAttributeList = attributeList
        startup.write()
        processInfo.write()
        Memory((commandLine.length + 1L) * Native.WCHAR_SIZE).use { command ->
            command.setWideString(0, commandLine)
            val flags = 0x00080000 or 0x00000400 or 0x08000000 or 0x00000004
            if (!api.CreateProcessW(null, command, null, null, true, flags,
                    environment, WString(directory), startup.pointer, processInfo.pointer)) {
                throw com.sun.jna.LastErrorException(Native.getLastError())
            }
            processInfo.read()
        }
    }

    override fun close() {
        if (closed) return
        api.DeleteProcThreadAttributeList(attributeList)
        attributeList.close()
        handleList.close()
        closed = true
    }

    @Structure.FieldOrder("StartupInfo", "lpAttributeList")
    class ExtendedStartupInfo : Structure() {
        @JvmField var StartupInfo = WinBase.STARTUPINFO()
        @JvmField var lpAttributeList: Pointer? = null
    }

    /** 直接声明扩展启动 ABI；不借用 ProcessBuilder 的内部句柄继承策略。 */
    private interface StartupApi : StdCallLibrary {
        fun InitializeProcThreadAttributeList(attributes: Pointer?, count: Int, flags: Int, bytes: Pointer?): Boolean
        fun UpdateProcThreadAttribute(attributes: Pointer, flags: Int, attribute: BaseTSD.ULONG_PTR,
            value: Pointer, size: BaseTSD.SIZE_T, previous: Pointer?, returnedSize: Pointer?): Boolean
        fun DeleteProcThreadAttributeList(attributes: Pointer)
        fun CreateProcessW(application: WString?, commandLine: Pointer, processAttributes: Pointer?,
            threadAttributes: Pointer?, inheritHandles: Boolean, flags: Int, environment: Pointer?,
            directory: WString?, startup: Pointer, processInfo: Pointer): Boolean
    }
}
