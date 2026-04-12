// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.




package std.runtime

import std.fs.*
import {
    std.io.*,
    std.process.*,
    std.fs.*,
    std.sync.ReentrantMutex
}

@Deprecated[message: "Use 'public func gc(heavy!: Bool = false): Unit' instead."]
@!APILevel[since: "12", atomicservice : true]
public func GC(heavy!: Bool = false): Unit

/*
* Set GCThreshold that user expected for GC as reference threshold.
*/
@Deprecated[message: "Use 'public func setGCThreshold(value: UInt64): Unit' instead."]
@!APILevel[since: "12", atomicservice : true]
public func SetGCThreshold(value: UInt64) : Unit

@!APILevel[since: "12", atomicservice : true]
public func gc(heavy!: Bool = false): Unit

@!APILevel[since: "12", atomicservice : true]
public func setGCThreshold(value: UInt64) : Unit

@!APILevel[since: "12", atomicservice : true]
public func getGCCount(): Int64

@!APILevel[since: "12", atomicservice : true]
public func getGCTime(): Int64

@!APILevel[since: "12", atomicservice : true]
public func getGCFreedSize(): Int64

@Deprecated[message: "All static Properties are converted to public funtions."]
@!APILevel[since: "12", atomicservice : true]
public struct MemoryInfo {
    /**
    * Get the maximum heap size that can be used.
    */
    @!APILevel[since: "12", atomicservice : true]
    static public prop maxHeapSize: Int64
    
    /**
    * Get the heap size that is allocated.
    */
    @!APILevel[since: "12", atomicservice : true]
    static public prop allocatedHeapSize: Int64
    
    /**
    * Get the physical memory that is used by heap.
    */
    @!APILevel[since: "12", atomicservice : true]
    static public prop heapPhysicalMemory: Int64
}

@!APILevel[since: "12", atomicservice : true]
public func getMaxHeapSize(): Int64

@!APILevel[since: "12", atomicservice : true]
public func getAllocatedHeapSize(): Int64

@!APILevel[since: "12", atomicservice : true]
public func getUsedHeapSize(): Int64

@!APILevel[since: "12", atomicservice : true]
public func dumpHeapData(path: Path): Unit

@Deprecated[message: "All static Properties are converted to public funtions."]
@!APILevel[since: "12", atomicservice : true]
public struct ProcessorInfo {
    // 获取处理器数量
    @!APILevel[since: "12", atomicservice : true]
    static public prop processorCount: Int64
}

@!APILevel[since: "12", atomicservice : true]
public func getProcessorCount(): Int64

@!APILevel[since: "12", atomicservice : true]
public func startCPUProfiling(): Unit

@!APILevel[since: "12", atomicservice : true]
public func stopCPUProfiling(path: Path): Unit

@Deprecated[message: "All static Properties are converted to public funtions."]
@!APILevel[since: "12", atomicservice : true]
public struct ThreadInfo {
    /**
    * Get current number of threads.
    */
    @!APILevel[since: "12", atomicservice : true]
    static public prop threadCount: Int64
    
    /**
    * Get the number of blocked threads.
    */
    @!APILevel[since: "12", atomicservice : true]
    static public prop blockingThreadCount: Int64
    
    /**
    * Get the actual number of physical threads.
    */
    @!APILevel[since: "12", atomicservice : true]
    static public prop nativeThreadCount: Int64
}

@!APILevel[since: "12", atomicservice : true]
public func getThreadCount(): Int64

@!APILevel[since: "12", atomicservice : true]
public func getBlockingThreadCount(): Int64

@!APILevel[since: "12", atomicservice : true]
public func getNativeThreadCount(): Int64

