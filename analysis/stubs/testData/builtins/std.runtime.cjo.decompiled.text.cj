// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.runtime

import std.core.*
import std.fs.*

func blackBox(input: T): T { /* compiled code */ }

func invokeGC(heavy: Bool): Unit { /* compiled code */ }

func getCjGCCount(): Int64 { /* compiled code */ }

func getCjGCTime(): Int64 { /* compiled code */ }

func getCjGCFreedSize(): Int64 { /* compiled code */ }

func setCjGCThreshold(value: UInt64): Unit { /* compiled code */ }

func GC(heavy: Bool): Unit { /* compiled code */ }

func SetGCThreshold(value: UInt64): Unit { /* compiled code */ }

func gc(heavy: Bool): Unit { /* compiled code */ }

func setGCThreshold(value: UInt64): Unit { /* compiled code */ }

func getGCCount(): Int64 { /* compiled code */ }

func getGCTime(): Int64 { /* compiled code */ }

func getGCFreedSize(): Int64 { /* compiled code */ }

func getMaxCjHeapSize(): Int64 { /* compiled code */ }

func getAllocatedCjHeapSize(): Int64 { /* compiled code */ }

func getRealHeapSize(): Int64 { /* compiled code */ }

func dumpCjHeapData(fd: Int32): Bool { /* compiled code */ }

class MemoryInfoException <: std.core.Exception {
    init(message: std.core.String) { /* compiled code */ }

    func getClassName(): std.core.String { /* compiled code */ }
}

struct MemoryInfo {
    static prop maxHeapSize: Int64 {
        get() { /* compiled code */ }
    }

    static prop allocatedHeapSize: Int64 {
        get() { /* compiled code */ }
    }

    static prop heapPhysicalMemory: Int64 {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

func getMaxHeapSize(): Int64 { /* compiled code */ }

func getAllocatedHeapSize(): Int64 { /* compiled code */ }

func getUsedHeapSize(): Int64 { /* compiled code */ }

func dumpHeapData(path: std.fs.Path): Unit { /* compiled code */ }

func startCjCPUProfiling(): Bool { /* compiled code */ }

func stopCjCPUProfiling(fd: Int32): Bool { /* compiled code */ }

foreign func CJ_OS_ProcessorCount(): Int64

class ProfilingInfoException <: std.core.Exception {
    init(message: std.core.String) { /* compiled code */ }

    func getClassName(): std.core.String { /* compiled code */ }
}

struct ProcessorInfo {
    static prop processorCount: Int64 {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

func getProcessorCount(): Int64 { /* compiled code */ }

func startCPUProfiling(): Unit { /* compiled code */ }

func stopCPUProfiling(path: std.fs.Path): Unit { /* compiled code */ }

func getThreadNumber(): Int64 { /* compiled code */ }

func getBlockingThreadNumber(): Int64 { /* compiled code */ }

func getNativeThreadNumber(): Int64 { /* compiled code */ }

struct ThreadInfo {
    static prop threadCount: Int64 {
        get() { /* compiled code */ }
    }

    static prop blockingThreadCount: Int64 {
        get() { /* compiled code */ }
    }

    static prop nativeThreadCount: Int64 {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

func getThreadCount(): Int64 { /* compiled code */ }

func getBlockingThreadCount(): Int64 { /* compiled code */ }

func getNativeThreadCount(): Int64 { /* compiled code */ }
