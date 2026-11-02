// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.runtime

import std.core.*
import std.fs.*

@Frozen
@Intrinsic
public func blackBox<T>(input: T): T

@Intrinsic
internal func invokeGC(heavy: Bool): Unit

@Intrinsic
internal func getCjGCCount(): Int64

@Intrinsic
internal func getCjGCTime(): Int64

@Intrinsic
internal func getCjGCFreedSize(): Int64

@Intrinsic
internal func setCjGCThreshold(value: UInt64): Unit

@Deprecated[message: "Use 'public func gc(heavy!: Bool = false): Unit' instead."]
public func GC(heavy!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

@Deprecated[message: "Use 'public func setGCThreshold(value: UInt64): Unit' instead."]
public func SetGCThreshold(value: UInt64): Unit { /* compiled code */ }

public func gc(heavy!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func setGCThreshold(value: UInt64): Unit { /* compiled code */ }

public func getGCCount(): Int64 { /* compiled code */ }

public func getGCTime(): Int64 { /* compiled code */ }

public func getGCFreedSize(): Int64 { /* compiled code */ }

@Intrinsic
internal func getMaxCjHeapSize(): Int64

@Intrinsic
internal func getAllocatedCjHeapSize(): Int64

@Intrinsic
internal func getRealHeapSize(): Int64

@Intrinsic
internal func dumpCjHeapData(fd: Int32): Bool

internal class MemoryInfoException <: std.core.Exception {
    init(message: std.core.String) { /* compiled code */ }

    protected func getClassName(): std.core.String { /* compiled code */ }
}

@Deprecated[message: "All static Properties are converted to public functions."]
public struct MemoryInfo {
    public static prop maxHeapSize: Int64 {
        get() { /* compiled code */ }
    }

    public static prop allocatedHeapSize: Int64 {
        get() { /* compiled code */ }
    }

    public static prop heapPhysicalMemory: Int64 {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public func getMaxHeapSize(): Int64 { /* compiled code */ }

public func getAllocatedHeapSize(): Int64 { /* compiled code */ }

public func getUsedHeapSize(): Int64 { /* compiled code */ }

public func dumpHeapData(path: std.fs.Path): Unit { /* compiled code */ }

@Intrinsic
internal func startCjCPUProfiling(): Bool

@Intrinsic
internal func stopCjCPUProfiling(fd: Int32): Bool

internal foreign func CJ_OS_ProcessorCount(): Int64

internal class ProfilingInfoException <: std.core.Exception {
    init(message: std.core.String) { /* compiled code */ }

    protected func getClassName(): std.core.String { /* compiled code */ }
}

@Deprecated[message: "All static Properties are converted to public functions."]
public struct ProcessorInfo {
    public static prop processorCount: Int64 {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public func getProcessorCount(): Int64 { /* compiled code */ }

public func startCPUProfiling(): Unit { /* compiled code */ }

public func stopCPUProfiling(path: std.fs.Path): Unit { /* compiled code */ }

@Intrinsic
internal func getThreadNumber(): Int64

@Intrinsic
internal func getBlockingThreadNumber(): Int64

@Intrinsic
internal func getNativeThreadNumber(): Int64

@Deprecated[message: "All static Properties are converted to public functions."]
public struct ThreadInfo {
    public static prop threadCount: Int64 {
        get() { /* compiled code */ }
    }

    public static prop blockingThreadCount: Int64 {
        get() { /* compiled code */ }
    }

    public static prop nativeThreadCount: Int64 {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public func getThreadCount(): Int64 { /* compiled code */ }

public func getBlockingThreadCount(): Int64 { /* compiled code */ }

public func getNativeThreadCount(): Int64 { /* compiled code */ }
