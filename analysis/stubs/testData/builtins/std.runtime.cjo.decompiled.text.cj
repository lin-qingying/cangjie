// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.runtime

import std.core.*
import std.fs.*

func blackBox(input: T): T

func invokeGC(heavy: Bool): Unit

func getCjGCCount: Int64

func getCjGCTime: Int64

func getCjGCFreedSize: Int64

func setCjGCThreshold(value: UInt64): Unit

func GC(heavy: Bool): Unit

func SetGCThreshold(value: UInt64): Unit

func gc(heavy: Bool): Unit

func setGCThreshold(value: UInt64): Unit

func getGCCount: Int64

func getGCTime: Int64

func getGCFreedSize: Int64

func getMaxCjHeapSize: Int64

func getAllocatedCjHeapSize: Int64

func getRealHeapSize: Int64

func dumpCjHeapData(fd: Int32): Bool

class MemoryInfoException <: std.core.Exception {
    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

struct MemoryInfo {
    prop maxHeapSize: Int64

    prop allocatedHeapSize: Int64

    prop heapPhysicalMemory: Int64

    init() { /* compiled code */ }
}

func getMaxHeapSize: Int64

func getAllocatedHeapSize: Int64

func getUsedHeapSize: Int64

func dumpHeapData(path: std.fs.Path): Unit

func startCjCPUProfiling: Bool

func stopCjCPUProfiling(fd: Int32): Bool

func CJ_OS_ProcessorCount: Int64

class ProfilingInfoException <: std.core.Exception {
    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

struct ProcessorInfo {
    prop processorCount: Int64

    init() { /* compiled code */ }
}

func getProcessorCount: Int64

func startCPUProfiling: Unit

func stopCPUProfiling(path: std.fs.Path): Unit

func getThreadNumber: Int64

func getBlockingThreadNumber: Int64

func getNativeThreadNumber: Int64

struct ThreadInfo {
    prop threadCount: Int64

    prop blockingThreadCount: Int64

    prop nativeThreadCount: Int64

    init() { /* compiled code */ }
}

func getThreadCount: Int64

func getBlockingThreadCount: Int64

func getNativeThreadCount: Int64
