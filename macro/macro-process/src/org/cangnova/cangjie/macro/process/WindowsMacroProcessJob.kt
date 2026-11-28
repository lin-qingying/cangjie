package org.cangnova.cangjie.macro.process

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.platform.win32.BaseTSD.SIZE_T
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT.HANDLE
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

/** 对位官方 CreateJobObjectForMacroSrv：Job 只归父进程持有，关闭父进程时回收其宏服务器。 */
internal class WindowsMacroProcessJob : AutoCloseable {
    private val api = Native.load("kernel32", JobApi::class.java, W32APIOptions.DEFAULT_OPTIONS)
    private var handle: HANDLE? = api.CreateJobObjectW(null, null)
        ?: throw com.sun.jna.LastErrorException(Native.getLastError())

    init {
        val limits = ExtendedLimitInformation()
        limits.BasicLimitInformation.LimitFlags = 0x00002000 // JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
        limits.write()
        if (!api.SetInformationJobObject(checkNotNull(handle), 9, limits.pointer, limits.size())) {
            val failure = com.sun.jna.LastErrorException(Native.getLastError())
            Kernel32.INSTANCE.CloseHandle(checkNotNull(handle))
            handle = null
            throw failure
        }
    }

    /** 必须在宏进程开始执行之前登记真实进程句柄，不能依赖进程名或 PID 重查。 */
    fun attach(process: HANDLE) {
        if (!api.AssignProcessToJobObject(checkNotNull(handle), process)) {
            throw com.sun.jna.LastErrorException(Native.getLastError())
        }
    }

    /** 已归属 Job 后恢复服务器主线程；失败仍属于当前启动事务。 */
    fun resume(thread: HANDLE) {
        if (api.ResumeThread(thread) == -1) {
            throw com.sun.jna.LastErrorException(Native.getLastError())
        }
    }

    override fun close() {
        val current = handle ?: return
        if (!Kernel32.INSTANCE.CloseHandle(current)) {
            throw com.sun.jna.LastErrorException(Native.getLastError())
        }
        handle = null
    }

    /** Win32 Job API 的直接 ABI 声明，不引入替代启动或兼容分支。 */
    private interface JobApi : StdCallLibrary {
        fun CreateJobObjectW(attributes: Pointer?, name: String?): HANDLE?
        fun SetInformationJobObject(job: HANDLE, informationClass: Int, information: Pointer, size: Int): Boolean
        fun AssignProcessToJobObject(job: HANDLE, process: HANDLE): Boolean
        fun ResumeThread(thread: HANDLE): Int
    }

    @Structure.FieldOrder("PerProcessUserTimeLimit", "PerJobUserTimeLimit", "LimitFlags", "MinimumWorkingSetSize", "MaximumWorkingSetSize", "ActiveProcessLimit", "Affinity", "PriorityClass", "SchedulingClass")
    class BasicLimitInformation : Structure() {
        @JvmField var PerProcessUserTimeLimit: Long = 0
        @JvmField var PerJobUserTimeLimit: Long = 0
        @JvmField var LimitFlags: Int = 0
        @JvmField var MinimumWorkingSetSize = SIZE_T()
        @JvmField var MaximumWorkingSetSize = SIZE_T()
        @JvmField var ActiveProcessLimit: Int = 0
        @JvmField var Affinity = ULONG_PTR()
        @JvmField var PriorityClass: Int = 0
        @JvmField var SchedulingClass: Int = 0
    }

    @Structure.FieldOrder("ReadOperationCount", "WriteOperationCount", "OtherOperationCount", "ReadTransferCount", "WriteTransferCount", "OtherTransferCount")
    class IoCounters : Structure() {
        @JvmField var ReadOperationCount: Long = 0
        @JvmField var WriteOperationCount: Long = 0
        @JvmField var OtherOperationCount: Long = 0
        @JvmField var ReadTransferCount: Long = 0
        @JvmField var WriteTransferCount: Long = 0
        @JvmField var OtherTransferCount: Long = 0
    }

    @Structure.FieldOrder("BasicLimitInformation", "IoInfo", "ProcessMemoryLimit", "JobMemoryLimit", "PeakProcessMemoryUsed", "PeakJobMemoryUsed")
    class ExtendedLimitInformation : Structure() {
        @JvmField var BasicLimitInformation = WindowsMacroProcessJob.BasicLimitInformation()
        @JvmField var IoInfo = IoCounters()
        @JvmField var ProcessMemoryLimit = SIZE_T()
        @JvmField var JobMemoryLimit = SIZE_T()
        @JvmField var PeakProcessMemoryUsed = SIZE_T()
        @JvmField var PeakJobMemoryUsed = SIZE_T()
    }
}
