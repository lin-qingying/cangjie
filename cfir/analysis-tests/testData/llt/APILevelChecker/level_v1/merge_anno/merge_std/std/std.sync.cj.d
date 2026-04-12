// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.









package std.sync

import std.time.MonoTime
import std.time.DateTime

/**
* Don't edit this, or the order should be the same as C's atomic memory_order.
*/
@Deprecated
@!APILevel[12, atomicservice : true]
public enum MemoryOrder {
    @!APILevel[12, atomicservice : true]
    SeqCst
}

@Deprecated
@!APILevel[12, atomicservice : true]
public let DefaultMemoryOrder: MemoryOrder = MemoryOrder.SeqCst

// NOTE:// The APIs of atomic types should have a naming parameter with a default argument,// like `atomicInt8.load(memoryOrder!: MemoryOrder.SeqCst)`.// However, most APIs are used with the default argument that will be obtained by// an extra function and passed to the API call.// The default argument construction incurs a huge performance overhead compared to the API call.// So, we split the API with two methods.//   - A method without the order parameter//   - A method with a naming parameter
@!APILevel[12, atomicservice : true]
public class AtomicInt8 {
    @!APILevel[12, atomicservice : true]
    public init(val: Int8)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func load(): Int8
    
    @Deprecated[message: "Use `public func load(): Int8` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): Int8
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func store(val: Int8): Unit
    
    @Frozen@Deprecated[message: "Use `public func store(val: Int8): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: Int8, memoryOrder!: MemoryOrder): Unit
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int8): Int8
    
    @Frozen@Deprecated[message: "Use `public func swap(val: Int8): Int8` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int8, memoryOrder!: MemoryOrder): Int8
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int8, new: Int8): Bool
    
    @Frozen@Deprecated[message: "Use `public func compareAndSwap(old: Int8, new: Int8): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int8, new: Int8, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int8): Int8
    
    @Frozen@Deprecated[message: "Use `public func fetchAdd(val: Int8): Int8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int8, memoryOrder!: MemoryOrder): Int8
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int8): Int8
    
    @Frozen@Deprecated[message: "Use `public func fetchSub(val: Int8): Int8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int8, memoryOrder!: MemoryOrder): Int8
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int8): Int8
    
    @Frozen@Deprecated[message: "Use `public func fetchAnd(val: Int8): Int8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int8, memoryOrder!: MemoryOrder): Int8
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int8): Int8
    
    @Frozen@Deprecated[message: "Use `public func fetchOr(val: Int8): Int8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int8, memoryOrder!: MemoryOrder): Int8
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int8): Int8
    
    @Frozen@Deprecated[message: "Use `public func fetchXor(val: Int8): Int8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int8, memoryOrder!: MemoryOrder): Int8
}

@!APILevel[12, atomicservice : true]
public class AtomicInt16 {
    @!APILevel[12, atomicservice : true]
    public init(val: Int16)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func load(): Int16
    
    @Frozen@Deprecated[message: "Use `public func load(): Int16` instead."]
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): Int16
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func store(val: Int16): Unit
    
    @Frozen@Deprecated[message: "Use `public func store(val: Int16): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: Int16, memoryOrder!: MemoryOrder): Unit
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int16): Int16
    
    @Frozen@Deprecated[message: "Use `public func swap(val: Int16): Int16` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int16, memoryOrder!: MemoryOrder): Int16
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int16, new: Int16): Bool
    
    @Frozen@Deprecated[message: "Use `public func compareAndSwap(old: Int16, new: Int16): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int16, new: Int16, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int16): Int16
    
    @Frozen@Deprecated[message: "Use `public func fetchAdd(val: Int16): Int16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int16, memoryOrder!: MemoryOrder): Int16
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int16): Int16
    
    @Frozen@Deprecated[message: "Use `public func fetchSub(val: Int16): Int16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int16, memoryOrder!: MemoryOrder): Int16
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int16): Int16
    
    @Frozen@Deprecated[message: "Use `public func fetchAnd(val: Int16): Int16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int16, memoryOrder!: MemoryOrder): Int16
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int16): Int16
    
    @Frozen@Deprecated[message: "Use `public func fetchOr(val: Int16): Int16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int16, memoryOrder!: MemoryOrder): Int16
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int16): Int16
    
    @Frozen@Deprecated[message: "Use `public func fetchXor(val: Int16): Int16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int16, memoryOrder!: MemoryOrder): Int16
}

@!APILevel[12, atomicservice : true]
public class AtomicInt32 {
    @!APILevel[12, atomicservice : true]
    public init(val: Int32)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func load(): Int32
    
    @Frozen@Deprecated[message: "Use `public func load(): Int32` instead."]
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): Int32
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func store(val: Int32): Unit
    
    @Frozen@Deprecated[message: "Use `public func store(val: Int32): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: Int32, memoryOrder!: MemoryOrder): Unit
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int32): Int32
    
    @Frozen@Deprecated[message: "Use `public func swap(val: Int32): Int32` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int32, memoryOrder!: MemoryOrder): Int32
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int32, new: Int32): Bool
    
    @Frozen@Deprecated[message: "Use `public func compareAndSwap(old: Int32, new: Int32): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int32, new: Int32, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int32): Int32
    
    @Frozen@Deprecated[message: "Use `public func fetchAdd(val: Int32): Int32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int32, memoryOrder!: MemoryOrder): Int32
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int32): Int32
    
    @Frozen@Deprecated[message: "Use `public func fetchSub(val: Int32): Int32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int32, memoryOrder!: MemoryOrder): Int32
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int32): Int32
    
    @Frozen@Deprecated[message: "Use `public func fetchAnd(val: Int32): Int32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int32, memoryOrder!: MemoryOrder): Int32
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int32): Int32
    
    @Frozen@Deprecated[message: "Use `public func fetchOr(val: Int32): Int32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int32, memoryOrder!: MemoryOrder): Int32
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int32): Int32
    
    @Frozen@Deprecated[message: "Use `public func fetchXor(val: Int32): Int32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int32, memoryOrder!: MemoryOrder): Int32
}

@!APILevel[12, atomicservice : true]
public class AtomicInt64 {
    @!APILevel[12, atomicservice : true]
    public init(val: Int64)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func load(): Int64
    
    @Frozen@Deprecated[message: "Use `public func load(): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): Int64
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func store(val: Int64): Unit
    
    @Frozen@Deprecated[message: "Use `public func store(val: Int64): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: Int64, memoryOrder!: MemoryOrder): Unit
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int64): Int64
    
    @Frozen@Deprecated[message: "Use `public func swap(val: Int64): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int64, memoryOrder!: MemoryOrder): Int64
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int64, new: Int64): Bool
    
    @Frozen@Deprecated[message: "Use `public func compareAndSwap(old: Int64, new: Int64): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int64, new: Int64, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int64): Int64
    
    @Frozen@Deprecated[message: "Use `public func fetchAdd(val: Int64): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int64, memoryOrder!: MemoryOrder): Int64
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int64): Int64
    
    @Frozen@Deprecated[message: "Use `public func fetchSub(val: Int64): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int64, memoryOrder!: MemoryOrder): Int64
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int64): Int64
    
    @Frozen@Deprecated[message: "Use `public func fetchAnd(val: Int64): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int64, memoryOrder!: MemoryOrder): Int64
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int64): Int64
    
    @Frozen@Deprecated[message: "Use `public func fetchOr(val: Int64): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int64, memoryOrder!: MemoryOrder): Int64
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int64): Int64
    
    @Frozen@Deprecated[message: "Use `public func fetchXor(val: Int64): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int64, memoryOrder!: MemoryOrder): Int64
}

@!APILevel[12, atomicservice : true]
public class AtomicUInt8 {
    @!APILevel[12, atomicservice : true]
    public init(val: UInt8)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func load(): UInt8
    
    @Frozen@Deprecated[message: "Use `public func load(): UInt8` instead."]
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): UInt8
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt8): Unit
    
    @Frozen@Deprecated[message: "Use `public func store(val: UInt8): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt8, memoryOrder!: MemoryOrder): Unit
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt8): UInt8
    
    @Frozen@Deprecated[message: "Use `public func swap(val: UInt8): UInt8` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt8, memoryOrder!: MemoryOrder): UInt8
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt8, new: UInt8): Bool
    
    @Frozen@Deprecated[message: "Use `public func compareAndSwap(old: UInt8, new: UInt8): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt8, new: UInt8, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt8): UInt8
    
    @Frozen@Deprecated[message: "Use `public func fetchAdd(val: UInt8): UInt8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt8, memoryOrder!: MemoryOrder): UInt8
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt8): UInt8
    
    @Frozen@Deprecated[message: "Use `public func fetchSub(val: UInt8): UInt8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt8, memoryOrder!: MemoryOrder): UInt8
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt8): UInt8
    
    @Frozen@Deprecated[message: "Use `public func fetchAnd(val: UInt8): UInt8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt8, memoryOrder!: MemoryOrder): UInt8
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt8): UInt8
    
    @Frozen@Deprecated[message: "Use `public func fetchOr(val: UInt8): UInt8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt8, memoryOrder!: MemoryOrder): UInt8
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt8): UInt8
    
    @Frozen@Deprecated[message: "Use `public func fetchXor(val: UInt8): UInt8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt8, memoryOrder!: MemoryOrder): UInt8
}

@!APILevel[12, atomicservice : true]
public class AtomicUInt16 {
    @!APILevel[12, atomicservice : true]
    public init(val: UInt16)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func load(): UInt16
    
    @Frozen@Deprecated[message: "Use `public func load(): UInt16` instead."]
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): UInt16
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt16): Unit
    
    @Frozen@Deprecated[message: "Use `public func store(val: UInt16): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt16, memoryOrder!: MemoryOrder): Unit
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt16): UInt16
    
    @Frozen@Deprecated[message: "Use `public func swap(val: UInt16): UInt16` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt16, memoryOrder!: MemoryOrder): UInt16
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt16, new: UInt16): Bool
    
    @Frozen@Deprecated[message: "Use `public func compareAndSwap(old: UInt16, new: UInt16): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt16, new: UInt16, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt16): UInt16
    
    @Frozen@Deprecated[message: "Use `public func fetchAdd(val: UInt16): UInt16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt16, memoryOrder!: MemoryOrder): UInt16
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt16): UInt16
    
    @Frozen@Deprecated[message: "Use `public func fetchSub(val: UInt16): UInt16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt16, memoryOrder!: MemoryOrder): UInt16
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt16): UInt16
    
    @Frozen@Deprecated[message: "Use `public func fetchAnd(val: UInt16): UInt16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt16, memoryOrder!: MemoryOrder): UInt16
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt16): UInt16
    
    @Frozen@Deprecated[message: "Use `public func fetchOr(val: UInt16): UInt16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt16, memoryOrder!: MemoryOrder): UInt16
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt16): UInt16
    
    @Frozen@Deprecated[message: "Use `public func fetchXor(val: UInt16): UInt16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt16, memoryOrder!: MemoryOrder): UInt16
}

@!APILevel[12, atomicservice : true]
public class AtomicUInt32 {
    @!APILevel[12, atomicservice : true]
    public init(val: UInt32)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func load(): UInt32
    
    @Deprecated[message: "Use `public func load(): UInt32` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): UInt32
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt32): Unit
    
    @Deprecated[message: "Use `public func store(val: UInt32): Unit` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt32, memoryOrder!: MemoryOrder): Unit
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt32): UInt32
    
    @Deprecated[message: "Use `public func swap(val: UInt32): UInt32` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt32, memoryOrder!: MemoryOrder): UInt32
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt32, new: UInt32): Bool
    
    @Deprecated[message: "Use `public func compareAndSwap(old: UInt32, new: UInt32): Bool` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt32, new: UInt32, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt32): UInt32
    
    @Deprecated[message: "Use `public func fetchAdd(val: UInt32): UInt32` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt32, memoryOrder!: MemoryOrder): UInt32
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt32): UInt32
    
    @Deprecated[message: "Use `public func fetchSub(val: UInt32): UInt32` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt32, memoryOrder!: MemoryOrder): UInt32
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt32): UInt32
    
    @Deprecated[message: "Use `public func fetchAnd(val: UInt32): UInt32` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt32, memoryOrder!: MemoryOrder): UInt32
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt32): UInt32
    
    @Deprecated[message: "Use `public func fetchOr(val: UInt32): UInt32` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt32, memoryOrder!: MemoryOrder): UInt32
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt32): UInt32
    
    @Deprecated[message: "Use `public func fetchXor(val: UInt32): UInt32` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt32, memoryOrder!: MemoryOrder): UInt32
}

@!APILevel[12, atomicservice : true]
public class AtomicUInt64 {
    @!APILevel[12, atomicservice : true]
    public init(val: UInt64)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func load(): UInt64
    
    @Deprecated[message: "Use `public func load(): UInt64` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): UInt64
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt64): Unit
    
    @Deprecated[message: "Use `public func store(val: UInt64): Unit` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt64, memoryOrder!: MemoryOrder): Unit
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt64): UInt64
    
    @Deprecated[message: "Use `public func swap(val: UInt64): UInt64` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt64, memoryOrder!: MemoryOrder): UInt64
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt64, new: UInt64): Bool
    
    @Deprecated[message: "Use `public func compareAndSwap(old: UInt64, new: UInt64): Bool` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt64, new: UInt64, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt64): UInt64
    
    @Deprecated[message: "Use `public func fetchAdd(val: UInt64): UInt64` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt64, memoryOrder!: MemoryOrder): UInt64
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt64): UInt64
    
    @Deprecated[message: "Use `public func fetchSub(val: UInt64): UInt64` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt64, memoryOrder!: MemoryOrder): UInt64
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt64): UInt64
    
    @Deprecated[message: "Use `public func fetchAnd(val: UInt64): UInt64` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt64, memoryOrder!: MemoryOrder): UInt64
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt64): UInt64
    
    @Deprecated[message: "Use `public func fetchOr(val: UInt64): UInt64` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt64, memoryOrder!: MemoryOrder): UInt64
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt64): UInt64
    
    @Deprecated[message: "Use `public func fetchXor(val: UInt64): UInt64` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt64, memoryOrder!: MemoryOrder): UInt64
}

@!APILevel[12, atomicservice : true]
public class AtomicBool {
    @!APILevel[12, atomicservice : true]
    public init(val: Bool)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func load(): Bool
    
    @Deprecated[message: "Use `public func load(): Bool` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): Bool
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func store(val: Bool): Unit
    
    @Deprecated[message: "Use `public func store(val: Bool): Unit` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func store(val: Bool, memoryOrder!: MemoryOrder): Unit
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func swap(val: Bool): Bool
    
    @Deprecated[message: "Use `public func swap(val: Bool): Bool` instead."]@Frozen
    @!APILevel[12, atomicservice : true]
    public func swap(val: Bool, memoryOrder!: MemoryOrder): Bool
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Bool, new: Bool): Bool
    
    @Frozen@Deprecated[message: "Use `public func compareAndSwap(old: Bool, new: Bool): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Bool, new: Bool, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
}

@!APILevel[12, atomicservice : true]
public class AtomicReference<T> where T <: Object {
    @!APILevel[12, atomicservice : true]
    public init(val: T)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func load(): T
    
    @Frozen@Deprecated[message: "Use `public func load(): T` instead."]
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): T
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func store(val: T): Unit
    
    @Frozen@Deprecated[message: "Use `public func store(val: T): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: T, memoryOrder!: MemoryOrder): Unit
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func swap(val: T): T
    
    @Frozen@Deprecated[message: "Use `public func swap(val: T): T` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: T, memoryOrder!: MemoryOrder): T
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: T, new: T): Bool
    
    @Frozen@Deprecated[message: "Use `public func compareAndSwap(old: T, new: T): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: T, new: T, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
}

/**
* AtomicOptionReference is an atomic reference type which could store null value, i.e. Option<T>.None.
*/
@!APILevel[12, atomicservice : true]
public class AtomicOptionReference<T> where T <: Object {
    /**
    * Initialize an AtomicOptionReference with null initial value.
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * Initialize an AtomicOptionReference with the given initial value.
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init(val: Option<T>)
    
    /**
    * Load the current value.
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func load(): Option<T>
    
    /**
    * Load the current value with the given memory order.
    */
    @Frozen@Deprecated[message: "Use `public func load(): Option<T>` instead."]
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): Option<T>
    
    /**
    * Store the given value.
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func store(val: Option<T>): Unit
    
    /**
    * Store the given value with the given memory order.
    */
    @Frozen@Deprecated[message: "Use `public func store(val: Option<T>): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: Option<T>, memoryOrder!: MemoryOrder): Unit
    
    /**
    * Atomically store the given value and return the old value.
    * @return the old value
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func swap(val: Option<T>): Option<T>
    
    /**
    * Atomically store the given value with the given memory order and return the old value.
    * @return the old value
    */
    @Frozen@Deprecated[message: "Use `public func swap(val: Option<T>): Option<T>` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: Option<T>, memoryOrder!: MemoryOrder): Option<T>
    
    /**
    * Atomically store the new value if the current value is equal to the old value.
    * @return `true` if store the new value successfully
    * @return `false` indicates that the current value is not equal to the old value
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Option<T>, new: Option<T>): Bool
    
    /**
    * Atomically store the new value with the given memory order if the current value is equal to the old value.
    * @return `true` if store the new value successfully
    * @return `false` indicates that the current value is not equal to the old value
    */
    @Frozen@Deprecated[message: "Use `public func compareAndSwap(old: Option<T>, new: Option<T>): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(
        old: Option<T>,
        new: Option<T>,
        successOrder!: MemoryOrder,
        failureOrder!: MemoryOrder
    ): Bool
}

// Currently, we only support sequential-consistency.
@Deprecated
@!APILevel[12, atomicservice : true]
public enum MemoryOrder {
    @!APILevel[12, atomicservice : true]
    SeqCst
}

@Deprecated
@!APILevel[12, atomicservice : true]
public let DefaultMemoryOrder: MemoryOrder = MemoryOrder.SeqCst

// NOTE:// The APIs of atomic types should have a naming parameter with a default argument,// like `atomicInt8.load(memoryOrder!: MemoryOrder.SeqCst)`.// However, most APIs are used with the default argument that will be obtained by// an extra function and passed to the API call.// The default argument construction incurs a huge performance overhead compared to the API call.// So, we split the API with two methods.//   - A method without the order parameter//   - A method with a naming parameter
@!APILevel[12, atomicservice : true]
public class AtomicInt8 {
    @!APILevel[12, atomicservice : true]
    public init(val: Int8)
    
    @!APILevel[12, atomicservice : true]
    public func load(): Int8
    
    @Deprecated[message: "Use `public func load(): Int8` instead."]
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): Int8
    
    @!APILevel[12, atomicservice : true]
    public func store(val: Int8): Unit
    
    @Deprecated[message: "Use `public func store(val: Int8): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: Int8, memoryOrder!: MemoryOrder): Unit
    
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int8): Int8
    
    @Deprecated[message: "Use `public func swap(val: Int8): Int8` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int8, memoryOrder!: MemoryOrder): Int8
    
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int8, new: Int8): Bool
    
    @Deprecated[message: "Use `public func compareAndSwap(old: Int8, new: Int8): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int8, new: Int8, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int8): Int8
    
    @Deprecated[message: "Use `public func fetchAdd(val: Int8): Int8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int8, memoryOrder!: MemoryOrder): Int8
    
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int8): Int8
    
    @Deprecated[message: "Use `public func fetchSub(val: Int8): Int8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int8, memoryOrder!: MemoryOrder): Int8
    
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int8): Int8
    
    @Deprecated[message: "Use `public func fetchAnd(val: Int8): Int8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int8, memoryOrder!: MemoryOrder): Int8
    
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int8): Int8
    
    @Deprecated[message: "Use `public func fetchOr(val: Int8): Int8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int8, memoryOrder!: MemoryOrder): Int8
    
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int8): Int8
    
    @Deprecated[message: "Use `public func fetchXor(val: Int8): Int8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int8, memoryOrder!: MemoryOrder): Int8
}

@!APILevel[12, atomicservice : true]
public class AtomicInt16 {
    @!APILevel[12, atomicservice : true]
    public init(val: Int16)
    
    @!APILevel[12, atomicservice : true]
    public func load(): Int16
    
    @Deprecated[message: "Use `public func load(): Int16` instead."]
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): Int16
    
    @!APILevel[12, atomicservice : true]
    public func store(val: Int16): Unit
    
    @Deprecated[message: "Use `public func store(val: Int16): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: Int16, memoryOrder!: MemoryOrder): Unit
    
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int16): Int16
    
    @Deprecated[message: "Use `public func swap(val: Int16): Int16` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int16, memoryOrder!: MemoryOrder): Int16
    
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int16, new: Int16): Bool
    
    @Deprecated[message: "Use `public func compareAndSwap(old: Int16, new: Int16): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int16, new: Int16, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int16): Int16
    
    @Deprecated[message: "Use `public func fetchAdd(val: Int16): Int16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int16, memoryOrder!: MemoryOrder): Int16
    
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int16): Int16
    
    @Deprecated[message: "Use `public func fetchSub(val: Int16): Int16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int16, memoryOrder!: MemoryOrder): Int16
    
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int16): Int16
    
    @Deprecated[message: "Use `public func fetchAnd(val: Int16): Int16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int16, memoryOrder!: MemoryOrder): Int16
    
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int16): Int16
    
    @Deprecated[message: "Use `public func fetchOr(val: Int16): Int16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int16, memoryOrder!: MemoryOrder): Int16
    
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int16): Int16
    
    @Deprecated[message: "Use `public func fetchXor(val: Int16): Int16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int16, memoryOrder!: MemoryOrder): Int16
}

@!APILevel[12, atomicservice : true]
public class AtomicInt32 {
    @!APILevel[12, atomicservice : true]
    public init(val: Int32)
    
    @!APILevel[12, atomicservice : true]
    public func load(): Int32
    
    @Deprecated[message: "Use `public func load(): Int32` instead."]
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): Int32
    
    @!APILevel[12, atomicservice : true]
    public func store(val: Int32): Unit
    
    @Deprecated[message: "Use `public func store(val: Int32): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: Int32, memoryOrder!: MemoryOrder): Unit
    
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int32): Int32
    
    @Deprecated[message: "Use `public func swap(val: Int32): Int32` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int32, memoryOrder!: MemoryOrder): Int32
    
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int32, new: Int32): Bool
    
    @Deprecated[message: "Use `public func compareAndSwap(old: Int32, new: Int32): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int32, new: Int32, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int32): Int32
    
    @Deprecated[message: "Use `public func fetchAdd(val: Int32): Int32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int32, memoryOrder!: MemoryOrder): Int32
    
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int32): Int32
    
    @Deprecated[message: "Use `public func fetchSub(val: Int32): Int32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int32, memoryOrder!: MemoryOrder): Int32
    
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int32): Int32
    
    @Deprecated[message: "Use `public func fetchAnd(val: Int32): Int32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int32, memoryOrder!: MemoryOrder): Int32
    
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int32): Int32
    
    @Deprecated[message: "Use `public func fetchOr(val: Int32): Int32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int32, memoryOrder!: MemoryOrder): Int32
    
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int32): Int32
    
    @Deprecated[message: "Use `public func fetchXor(val: Int32): Int32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int32, memoryOrder!: MemoryOrder): Int32
}

@!APILevel[12, atomicservice : true]
public class AtomicInt64 {
    @!APILevel[12, atomicservice : true]
    public init(val: Int64)
    
    @!APILevel[12, atomicservice : true]
    public func load(): Int64
    
    @Deprecated[message: "Use `public func load(): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func load( memoryOrder!: MemoryOrder): Int64
    
    @!APILevel[12, atomicservice : true]
    public func store(val: Int64): Unit
    
    @Deprecated[message: "Use `public func store(val: Int64): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: Int64, memoryOrder!: MemoryOrder): Unit
    
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int64): Int64
    
    @Deprecated[message: "Use `public func swap(val: Int64): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: Int64, memoryOrder!: MemoryOrder): Int64
    
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int64, new: Int64): Bool
    
    @Deprecated[message: "Use `public func compareAndSwap(old: Int64, new: Int64): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Int64, new: Int64, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int64): Int64
    
    @Deprecated[message: "Use `public func fetchAdd(val: Int64): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: Int64, memoryOrder!: MemoryOrder): Int64
    
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int64): Int64
    
    @Deprecated[message: "Use `public func fetchSub(val: Int64): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: Int64, memoryOrder!: MemoryOrder): Int64
    
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int64): Int64
    
    @Deprecated[message: "Use `public func fetchAnd(val: Int64): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: Int64, memoryOrder!: MemoryOrder): Int64
    
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int64): Int64
    
    @Deprecated[message: "Use `public func fetchOr(val: Int64): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: Int64, memoryOrder!: MemoryOrder): Int64
    
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int64): Int64
    
    @Deprecated[message: "Use `public func fetchXor(val: Int64): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: Int64, memoryOrder!: MemoryOrder): Int64
}

@!APILevel[12, atomicservice : true]
public class AtomicUInt8 {
    @!APILevel[12, atomicservice : true]
    public init(val: UInt8)
    
    @!APILevel[12, atomicservice : true]
    public func load(): UInt8
    
    @Deprecated[message: "Use `public func load(): UInt8` instead."]
    @!APILevel[12, atomicservice : true]
    public func load( memoryOrder!: MemoryOrder): UInt8
    
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt8): Unit
    
    @Deprecated[message: "Use `public func store(val: UInt8): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt8, memoryOrder!: MemoryOrder): Unit
    
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt8): UInt8
    
    @Deprecated[message: "Use `public func swap(val: UInt8): UInt8` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt8, memoryOrder!: MemoryOrder): UInt8
    
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt8, new: UInt8): Bool
    
    @Deprecated[message: "Use `public func compareAndSwap(old: UInt8, new: UInt8): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt8, new: UInt8, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt8): UInt8
    
    @Deprecated[message: "Use `public func fetchAdd(val: UInt8): UInt8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt8, memoryOrder!: MemoryOrder): UInt8
    
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt8): UInt8
    
    @Deprecated[message: "Use `public func fetchSub(val: UInt8): UInt8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt8, memoryOrder!: MemoryOrder): UInt8
    
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt8): UInt8
    
    @Deprecated[message: "Use `public func fetchAnd(val: UInt8): UInt8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt8, memoryOrder!: MemoryOrder): UInt8
    
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt8): UInt8
    
    @Deprecated[message: "Use `public func fetchOr(val: UInt8): UInt8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt8, memoryOrder!: MemoryOrder): UInt8
    
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt8): UInt8
    
    @Deprecated[message: "Use `public func fetchXor(val: UInt8): UInt8` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt8, memoryOrder!: MemoryOrder): UInt8
}

@!APILevel[12, atomicservice : true]
public class AtomicUInt16 {
    @!APILevel[12, atomicservice : true]
    public init(val: UInt16)
    
    @!APILevel[12, atomicservice : true]
    public func load(): UInt16
    
    @Deprecated[message: "Use `public func load(): UInt16` instead."]
    @!APILevel[12, atomicservice : true]
    public func load( memoryOrder!: MemoryOrder): UInt16
    
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt16): Unit
    
    @Deprecated[message: "Use `public func store(val: UInt16): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt16, memoryOrder!: MemoryOrder): Unit
    
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt16): UInt16
    
    @Deprecated[message: "Use `public func swap(val: UInt16): UInt16` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt16, memoryOrder!: MemoryOrder): UInt16
    
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt16, new: UInt16): Bool
    
    @Deprecated[message: "Use `public func compareAndSwap(old: UInt16, new: UInt16): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt16, new: UInt16, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt16): UInt16
    
    @Deprecated[message: "Use `public func fetchAdd(val: UInt16): UInt16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt16, memoryOrder!: MemoryOrder): UInt16
    
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt16): UInt16
    
    @Deprecated[message: "Use `public func fetchSub(val: UInt16): UInt16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt16, memoryOrder!: MemoryOrder): UInt16
    
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt16): UInt16
    
    @Deprecated[message: "Use `public func fetchAnd(val: UInt16): UInt16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt16, memoryOrder!: MemoryOrder): UInt16
    
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt16): UInt16
    
    @Deprecated[message: "Use `public func fetchOr(val: UInt16): UInt16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt16, memoryOrder!: MemoryOrder): UInt16
    
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt16): UInt16
    
    @Deprecated[message: "Use `public func fetchXor(val: UInt16): UInt16` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt16, memoryOrder!: MemoryOrder): UInt16
}

@!APILevel[12, atomicservice : true]
public class AtomicUInt32 {
    @!APILevel[12, atomicservice : true]
    public init(val: UInt32)
    
    @!APILevel[12, atomicservice : true]
    public func load(): UInt32
    
    @Deprecated[message: "Use `public func load(): UInt32` instead."]
    @!APILevel[12, atomicservice : true]
    public func load( memoryOrder!: MemoryOrder): UInt32
    
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt32): Unit
    
    @Deprecated[message: "Use `public func store(val: UInt32): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt32, memoryOrder!: MemoryOrder): Unit
    
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt32): UInt32
    
    @Deprecated[message: "Use `public func swap(val: UInt32): UInt32` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt32, memoryOrder!: MemoryOrder): UInt32
    
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt32, new: UInt32): Bool
    
    @Deprecated[message: "Use `public func compareAndSwap(old: UInt32, new: UInt32): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt32, new: UInt32, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt32): UInt32
    
    @Deprecated[message: "Use `public func fetchAdd(val: UInt32): UInt32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt32, memoryOrder!: MemoryOrder): UInt32
    
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt32): UInt32
    
    @Deprecated[message: "Use `public func fetchSub(val: UInt32): UInt32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt32, memoryOrder!: MemoryOrder): UInt32
    
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt32): UInt32
    
    @Deprecated[message: "Use `public func fetchAnd(val: UInt32): UInt32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt32, memoryOrder!: MemoryOrder): UInt32
    
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt32): UInt32
    
    @Deprecated[message: "Use `public func fetchOr(val: UInt32): UInt32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt32, memoryOrder!: MemoryOrder): UInt32
    
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt32): UInt32
    
    @Deprecated[message: "Use `public func fetchXor(val: UInt32): UInt32` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt32, memoryOrder!: MemoryOrder): UInt32
}

@!APILevel[12, atomicservice : true]
public class AtomicUInt64 {
    @!APILevel[12, atomicservice : true]
    public init(val: UInt64)
    
    @!APILevel[12, atomicservice : true]
    public func load(): UInt64
    
    @Deprecated[message: "Use `public func load(): UInt64` instead."]
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): UInt64
    
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt64): Unit
    
    @Deprecated[message: "Use `public func store(val: UInt64): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: UInt64, memoryOrder!: MemoryOrder): Unit
    
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt64): UInt64
    
    @Deprecated[message: "Use `public func swap(val: UInt64): UInt64` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: UInt64, memoryOrder!: MemoryOrder): UInt64
    
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt64, new: UInt64): Bool
    
    @Deprecated[message: "Use `public func compareAndSwap(old: UInt64, new: UInt64): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: UInt64, new: UInt64, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
    
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt64): UInt64
    
    @Deprecated[message: "Use `public func fetchAdd(val: UInt64): UInt64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAdd(val: UInt64, memoryOrder!: MemoryOrder): UInt64
    
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt64): UInt64
    
    @Deprecated[message: "Use `public func fetchSub(val: UInt64): UInt64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchSub(val: UInt64, memoryOrder!: MemoryOrder): UInt64
    
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt64): UInt64
    
    @Deprecated[message: "Use `public func fetchAnd(val: UInt64): UInt64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchAnd(val: UInt64, memoryOrder!: MemoryOrder): UInt64
    
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt64): UInt64
    
    @Deprecated[message: "Use `public func fetchOr(val: UInt64): UInt64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchOr(val: UInt64, memoryOrder!: MemoryOrder): UInt64
    
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt64): UInt64
    
    @Deprecated[message: "Use `public func fetchXor(val: UInt64): UInt64` instead."]
    @!APILevel[12, atomicservice : true]
    public func fetchXor(val: UInt64, memoryOrder!: MemoryOrder): UInt64
}

@!APILevel[12, atomicservice : true]
public class AtomicBool {
    @!APILevel[12, atomicservice : true]
    public init(val: Bool)
    
    @!APILevel[12, atomicservice : true]
    public func load(): Bool
    
    @Deprecated[message: "Use `public func load(): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): Bool
    
    @!APILevel[12, atomicservice : true]
    public func store(val: Bool): Unit
    
    @Deprecated[message: "Use `public func store(val: Bool): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: Bool, memoryOrder!: MemoryOrder): Unit
    
    @!APILevel[12, atomicservice : true]
    public func swap(val: Bool): Bool
    
    @Deprecated[message: "Use `public func swap(val: Bool): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: Bool, memoryOrder!: MemoryOrder): Bool
    
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Bool, new: Bool): Bool
    
    @Deprecated[message: "Use `public func compareAndSwap(old: Bool, new: Bool): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Bool, new: Bool, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
}

@!APILevel[12, atomicservice : true]
public class AtomicReference<T> where T <: Object {
    @!APILevel[12, atomicservice : true]
    public init(val: T)
    
    @!APILevel[12, atomicservice : true]
    public func load(): T
    
    @Deprecated[message: "Use `public func load(): T` instead."]
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): T
    
    @!APILevel[12, atomicservice : true]
    public func store(val: T): Unit
    
    @Deprecated[message: "Use `public func store(val: T): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: T, memoryOrder!: MemoryOrder): Unit
    
    @!APILevel[12, atomicservice : true]
    public func swap(val: T): T
    
    @Deprecated[message: "Use `public func swap(val: T): T` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: T, memoryOrder!: MemoryOrder): T
    
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: T, new: T): Bool
    
    @Deprecated[message: "Use `public func compareAndSwap(old: T, new: T): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: T, new: T, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
}

/**
* AtomicOptionReference is an atomic reference type which could store null value, i.e. Option<T>.None.
*/
@!APILevel[12, atomicservice : true]
public class AtomicOptionReference<T> where T <: Object {
    /**
    * Initialize an AtomicOptionReference with null initial value.
    */
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * Initialize an AtomicOptionReference with the given initial value.
    */
    @!APILevel[12, atomicservice : true]
    public init(val: Option<T>)
    
    /**
    * Load the current value.
    */
    @!APILevel[12, atomicservice : true]
    public func load(): Option<T>
    
    /**
    * Load the current value with the given memory order.
    */
    @Deprecated[message: "Use `public func load(): Option<T>` instead."]
    @!APILevel[12, atomicservice : true]
    public func load(memoryOrder!: MemoryOrder): Option<T>
    
    /**
    * Store the given value.
    */
    @!APILevel[12, atomicservice : true]
    public func store(val: Option<T>): Unit
    
    /**
    * Store the given value with the given memory order.
    */
    @Deprecated[message: "Use `public func store(val: Option<T>): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func store(val: Option<T>, memoryOrder!: MemoryOrder): Unit
    
    /**
    * Atomically store the given value and return the old value.
    * @return the old value
    */
    @!APILevel[12, atomicservice : true]
    public func swap(val: Option<T>): Option<T>
    
    /**
    * Atomically store the given value with the given memory order and return the old value.
    * @return the old value
    */
    @Deprecated[message: "Use `public func swap(val: Option<T>): Option<T>` instead."]
    @!APILevel[12, atomicservice : true]
    public func swap(val: Option<T>, memoryOrder!: MemoryOrder): Option<T>
    
    /**
    * Atomically store the new value if the current value is equal to the old value.
    * @return `true` if store the new value successfully
    * @return `false` indicates that the current value is not equal to the old value
    */
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Option<T>, new: Option<T>): Bool
    
    /**
    * Atomically store the new value with the given memory order if the current value is equal to the old value.
    * @return `true` if store the new value successfully
    * @return `false` indicates that the current value is not equal to the old value
    */
    @Deprecated[message: "Use `public func compareAndSwap(old: Option<T>, new: Option<T>): Bool` instead."]
    @!APILevel[12, atomicservice : true]
    public func compareAndSwap(old: Option<T>, new: Option<T>, successOrder!: MemoryOrder, failureOrder!: MemoryOrder): Bool
}

@!APILevel[12, atomicservice : true]
public class Barrier {
    /**
    * Construct a barrier with an initial value.
    * @throw IllegalArgumentException, if `count` is negative.
    */
    @!APILevel[12, atomicservice : true]
    public init(count: Int64)
    
    /**
    * Decrease the count.
    * If the count becomes zero, wake up all blocked threads,
    * Otherwise, the current thread waits until the count becomes zero or `timeout` passed.
    * If the count is zero, it has no effects.
    * All events before `countDown()` happens-before events after `countDownAndWait()`.
    * NOTE: `timeout` should be `Duration.Max` by default,
    * however, the value `Duration.Max` is not supported currently.
    */
    @!APILevel[12, atomicservice : true]
    public func wait(timeout!: Duration = Duration.Max): Unit
}

// Common interface for reentrant mutual exclusion concurrency primitives.//  ! It is a responsibility of implementor to guarantee that underlying mutex actually supports nested locking.//  ! It is a responsibility of implementor to throw ISSE in case of violation of contract.//  ! `synchronized` keyword is not compatible with this interface for performance reasons
@Deprecated[message: "Use `public interface Lock` instead."]
@!APILevel[12, atomicservice : true]
public interface IReentrantMutex {
}

@!APILevel[12, atomicservice : true]
public class IllegalSynchronizationStateException <: Exception {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

/**
* @brief Base interface for reentrant mutual exclusion concurrency primitives.
*
* Instance of the Lock interface can be passed as arguments to `synchronized` expression,
* which is equivalent to the following transformation
*        source code      |        semantics
* ------------------------------------------------------
*   synchronized(l) {     |  try { l.lock()
*     foo()               |    foo()
*   }                     |  } finally { l.unlock() }
*/
@!APILevel[12, atomicservice : true]
public interface Lock {
}

@!APILevel[12, atomicservice : true]
public interface UniqueLock <: Lock {
}

/**
* Condition (a.k.a. wait queue) associated with a mutex.
*/
@!APILevel[12, atomicservice : true]
public interface Condition {
}

/**
* Class that encapsulates single reentrant mutex and single condition instance (a.k.a. wait queue) associated with this mutex.
*/
@Deprecated[message: "Use `public interface Condition` instead."]
@!APILevel[12, atomicservice : true]
public class Monitor <: ReentrantMutex {
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * @brief Block until paired `notify` is invoked or `timeout` nanoseconds passed.
    * @return `true` if event was signalled by other thread;
    * @return `false` on timeout, otherwise.
    * @throws IllegalSynchronizationStateException if current thread does not hold this mutex
    * @throws IllegalArgumentException if `timeout <= Duration.Zero`
    */
    @!APILevel[12, atomicservice : true]
    public func wait(timeout!: Duration = Duration.Max): Bool
    
    /**
    * @brief Wake up a single thread waiting on this mutex (no particular admission policy implied).
    * @throws IllegalSynchronizationStateException if current thread does not hold this mutex
    */
    @!APILevel[12, atomicservice : true]
    public func notify(): Unit
    
    /**
    * @brief Wake up all threads waiting on this mutex (no particular admission policy implied).
    * @throws IllegalSynchronizationStateException if current thread does not hold this mutex
    */
    @!APILevel[12, atomicservice : true]
    public func notifyAll(): Unit
}

/**
* @brief Record represents unique identifier of an existing wait queue associated with given mutex.
* Passing invalid ConditionID to the MultiConditionMonitor results into ISSE.
*/
@Deprecated[message: "Use `public interface Condition` instead."]
@!APILevel[12, atomicservice : true]
public struct ConditionID {
}

/**
* @brief Class that encapsulates single reentrant mutex and a set of dynamically created condition instances associated with this mutex.
*/
@Deprecated[message: "Use `public class Mutex` instead."]
@!APILevel[12, atomicservice : true]
public class MultiConditionMonitor <: ReentrantMutex {
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * @return A new ConditionID associated with this mutex. May be used to implement "single mutex -- multiple wait queues" concurrent primitives.
    * @throws IllegalSynchronizationStateException if current thread does not hold this mutex.
    */
    @!APILevel[12, atomicservice : true]
    public func newCondition(): ConditionID
    
    /**
    * @brief Block until paired `notify` is invoked or `timeout` nanoseconds passed
    * @return `true` if specified condition was signalled by other thread;
    * @return `false` if timeouted.
    * @throws IllegalSynchronizationStateException if current thread does not hold this mutex
    * @throws IllegalSynchronizationStateException if `id` isn't provided by `newCondition` of this MultiConditionMonitor instance
    * @throws IllegalArgumentException if `timeout <= Duration.Zero`
    */
    @!APILevel[12, atomicservice : true]
    public func wait(condID: ConditionID, timeout!: Duration = Duration.Max): Bool
    
    /**
    * @brief Wake up a single thread waiting on this mutex (no particular admission policy implied)
    * @throws IllegalSynchronizationStateException if current thread does not hold this mutex
    * @throws IllegalSynchronizationStateException if `id` was not provided by `newCondition` of this MultiConditionMonitor instance
    */
    @!APILevel[12, atomicservice : true]
    public func notify(condID: ConditionID): Unit
    
    /**
    * @brief Wake up all threads waiting on this mutex (no particular admission policy implied)
    * @throws IllegalSynchronizationStateException if current thread does not hold this mutex
    * @throws IllegalSynchronizationStateException if `id` was not provided by `newCondition` of this MultiConditionMonitor instance
    */
    @!APILevel[12, atomicservice : true]
    public func notifyAll(condID: ConditionID): Unit
}

// Base class for built-in reentrant mutual exclusion concurrency primitives.// ReentrantMutex and its subclasses instances may be passed as argument to `synchronized` keyword which will result in the following transformation//        source code      |        semantics// ------------------------------------------------------//   synchronized(m) {     |            m.lock()//        foo()            |  try     { foo()      }//   }                     |  finally { m.unlock() }
@Deprecated[message: "Use `public class Mutex` instead."]
@!APILevel[12, atomicservice : true]
public open class ReentrantMutex <: Lock {
    @!APILevel[12, atomicservice : true]
    public init()
    
    // reentrant lock, blocks until success
    @!APILevel[12, atomicservice : true]
    public open func lock(): Unit
    
    // returns false on fail, does not block
    @!APILevel[12, atomicservice : true]
    public open func tryLock(): Bool
    
    // if mutex is locked recursively, this method should be invoked N times to fully unlock mutex// throws ISSE("Mutex is not locked by current thread") if current thread does not hold this mutex
    @!APILevel[12, atomicservice : true]
    public open func unlock(): Unit
}

// Class that encapsulates single reentrant mutex and single condition instance (a.k.a. wait queue) associated with this mutex.
@Deprecated[message: "Use `public interface Condition` instead."]
@!APILevel[12, atomicservice : true]
public class Monitor <: ReentrantMutex {
    @!APILevel[12, atomicservice : true]
    public init()
    
    // blocks until paired `notify` is invoked or `timeout` nanoseconds passed// returns `true` if event was signalled by other thread, returns `false` on timeout// throws ISSE("Mutex is not locked by current thread") if current thread does not hold this mutex
    @!APILevel[12, atomicservice : true]
    public func wait(timeout!: Duration = Duration.Max): Bool
    
    // wakes up a single thread waiting on this mutex (no particular admission policy implied)// throws ISSE("Mutex is not locked by current thread") if current thread does not hold this mutex
    @!APILevel[12, atomicservice : true]
    public func notify(): Unit
    
    // wakes up all threads waiting on this mutex (no particular admission policy implied)// throws ISSE("Mutex is not locked by current thread") if current thread does not hold this mutex
    @!APILevel[12, atomicservice : true]
    public func notifyAll(): Unit
}

@!APILevel[12, atomicservice : true]

// Record represents unique identifier of an existing wait queue associated with given mutex.// Passing invalid ConditionID to the MultiConditionMonitor results into ISSE.
@Deprecated[message: "Use `public interface Condition` instead."]
@!APILevel[12, atomicservice : true]
public struct ConditionID {
}

// Class that encapsulates single reentrant mutex and a set of dynamically created condition instances associated with this mutex.
@Deprecated[message: "Use `public class Mutex` instead."]
@!APILevel[12, atomicservice : true]
public class MultiConditionMonitor <: ReentrantMutex {
    @!APILevel[12, atomicservice : true]
    public init()
    
    // returns new ConditionID associated with this mutex. May be used to implement "single mutex -- multiple wait queues" concurrent primitives.// throws ISSE("Mutex is not locked by current thread") if current thread does not hold this mutex
    @!APILevel[12, atomicservice : true]
    public func newCondition(): ConditionID
    
    // blocks until paired `notify` is invoked or `timeout` nanoseconds passed// returns `true` if specified condition was signalled by other thread, returns `false` if timeouted// throws ISSE("Mutex is not locked by current thread") if current thread does not hold this mutex// throws ISSE("Invalid condition") if `condID` was not provided by `newCondition` of this MultiConditionMonitor instance
    @!APILevel[12, atomicservice : true]
    public func wait(condID: ConditionID, timeout!: Duration = Duration.Max): Bool
    
    // wakes up a single thread waiting on this mutex (no particular admission policy implied)// throws ISSE("Mutex is not locked by current thread") if current thread does not hold this mutex// throws ISSE("Invalid condition") if `condID` was not provided by `newCondition` of this MultiConditionMonitor instance
    @!APILevel[12, atomicservice : true]
    public func notify(condID: ConditionID): Unit
    
    // wakes up all threads waiting on this mutex (no particular admission policy implied)// throws ISSE("Mutex is not locked by current thread") if current thread does not hold this mutex// throws ISSE("Invalid condition") if `condID` was not provided by `newCondition` of this MultiConditionMonitor instance
    @!APILevel[12, atomicservice : true]
    public func notifyAll(condID: ConditionID): Unit
}

/**
* @brief Base class for built-in reentrant mutual exclusion concurrency primitives.
* Instance of the Lock interface can be passed as arguments to `synchronized` expression,
* which is equivalent to the following transformation
*        source code      |        semantics
* ------------------------------------------------------
*   synchronized(m) {     |  try { m.lock()
*     foo()               |    foo()
*   }                     |  } finally { m.unlock() }
*/
@!APILevel[12, atomicservice : true]
public class Mutex <: UniqueLock {
    /*
    * @throws IllegalSynchronizationStateException if `ptr` is null
    */
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * @brief Acquire the ownership of the lock, blocks if the lock is not available.
    * Support reentrant.
    */
    @!APILevel[12, atomicservice : true]
    public func lock(): Unit
    
    /**
    * @brief Try to lock the mutex.
    * @return false if the lock is not available;
    * @return returns true, otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func tryLock(): Bool
    
    /**
    * @brief Unlock the mutex.
    * If there are other threads blocking on this lock, then wake up one of them.
    * @throws IllegalSynchronizationStateException if the current thread does not hold the lock.
    */
    @!APILevel[12, atomicservice : true]
    public func unlock(): Unit
    
    /**
    * @brief Generate a Condition instance for the mutex.
    * @throws IllegalSynchronizationStateException if the current thread does not hold the lock.
    */
    @!APILevel[12, atomicservice : true]
    public func condition(): Condition
}

/**
* Instances of the Lock interface may be passed as argument to `synchronized` keyword which will result in the following transformation
*        source code      |        semantics
* ------------------------------------------------------
*   synchronized(m) {     |            m.lock()
*        foo()            |  try     { foo()      }
*   }                     |  finally { m.unlock() }
*
* note: Should invode the intrinsic method provided by the backend in the future, instead of proxy the MultiConditionMonitor.
*/
@!APILevel[12, atomicservice : true]
public class Mutex <: UniqueLock {
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * @brief Acquire the ownership of the lock, blocks if the lock is not available.
    * Support reentrant.
    */
    @!APILevel[12, atomicservice : true]
    public func lock(): Unit
    
    /**
    * @brief Try to lock the mutex.
    * @return false if the lock is not available;
    * @return returns true, otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func tryLock(): Bool
    
    /**
    * if mutex is locked recursively, this method should be invoked N times to fully unlock mutex
    * @throws IllegalSynchronizationStateException if current thread does not hold this mutex
    */
    @!APILevel[12, atomicservice : true]
    public func unlock(): Unit
    
    /**
    * @brief Generate a Condition instance for the mutex.
    * @throws IllegalSynchronizationStateException if the current thread does not hold the lock.
    */
    @!APILevel[12, atomicservice : true]
    public func condition(): Condition
}

@!APILevel[12, atomicservice : true]
public class ReadWriteLock {
    @!APILevel[12, atomicservice : true]
    public prop readLock: Lock
    
    @!APILevel[12, atomicservice : true]
    public prop writeLock: UniqueLock
    
    /**
    * @param fair - Set up the fair mode.
    */
    @!APILevel[12, atomicservice : true]
    public init(fair!: Bool = false)
    
    @!APILevel[12, atomicservice : true]
    public func isFair(): Bool
}

/**
* @brief Base class for built-in reentrant mutual exclusion concurrency primitives.
* ReentrantMutex and its subclasses instances can be passed as arguments to `synchronized` expression,
* which is equivalent to the following transformation
*        source code      |        semantics
* ------------------------------------------------------
*   synchronized(m) {     |  try { m.lock()
*     foo()               |    foo()
*   }                     |  } finally { m.unlock() }
*/
@Deprecated[message: "Use `public class Mutex` instead."]
@!APILevel[12, atomicservice : true]
public open class ReentrantMutex <: Lock {
    /* @throws IllegalSynchronizationStateException if the Mutex initialization is failed.*/
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * @brief Acquire the ownership of the lock, blocks if the lock is not available.
    * Support reentrant.
    */
    @!APILevel[12, atomicservice : true]
    public open func lock(): Unit
    
    /**
    * @brief Try to lock the mutex.
    * @return false if the lock is not available;
    * @return returns true, otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public open func tryLock(): Bool
    
    /**
    * @brief Unlock the mutex.
    * If there are other threads blocking on this lock, then wake up one of them.
    * @throws IllegalSynchronizationStateException if the current thread does not hold the lock
    */
    @!APILevel[12, atomicservice : true]
    public open func unlock(): Unit
}

@Deprecated
@!APILevel[12, atomicservice : true]
public enum ReadWriteMutexMode {
    @!APILevel[12, atomicservice : true]
    Unfair |
    @!APILevel[12, atomicservice : true]
    Fair
}

@Deprecated[message: "Use `public class ReadWriteLock` instead."]
@!APILevel[12, atomicservice : true]
public class ReentrantReadWriteMutex {
    @!APILevel[12, atomicservice : true]
    public init(mode!: ReadWriteMutexMode = ReadWriteMutexMode.Unfair)
    
    @!APILevel[12, atomicservice : true]
    public prop readMutex: ReentrantReadMutex
    
    @!APILevel[12, atomicservice : true]
    public prop writeMutex: ReentrantWriteMutex
}

@Deprecated[message: "Use `public interface Lock` instead."]
@!APILevel[12, atomicservice : true]
public class ReentrantReadMutex <: ReentrantMutex {
    /**
    * Acquire the read-mutex.
    * Under fair mode,
    *   non-reentrantly hold the lock if there are no other threads holding or waiting for the write-mutex;
    *   otherwise, blocked.
    * Under nonfair mode,
    *   block if there are other threads holding the write-mutex;
    *   hold the mutex if there are no waiting writer threads;
    *   otherwise, holding the mutex or being blocked is decided by the implementation.
    * Multiple threads can hold the read-mutex concurrently, and one thread can acquire recursively.
    * If one thread holds the write-mutex, it can also hold the read-mutex.
    */
    @!APILevel[12, atomicservice : true]
    public func lock(): Unit
    
    /**
    * Try to acquire the read-mutex.
    * Return true if it succeeds to hold the read-mutex;
    * otherwise, return false.
    * This method does not obey the fairness constraints.
    */
    @!APILevel[12, atomicservice : true]
    public func tryLock(): Bool
    
    /**
    * Release the read-mutex.
    * This method should be invoked N times to fully unlock the mutex.
    * If fully released, waiting writer threads are notified to acquire the write-mutex.
    * Throws ISSE("Mutex is not hold by the current thread") if the current thread does not hold this read-mutex.
    */
    @!APILevel[12, atomicservice : true]
    public func unlock(): Unit
}

@Deprecated[message: "Use `public interface UniqueLock` instead."]
@!APILevel[12, atomicservice : true]
public class ReentrantWriteMutex <: ReentrantMutex {
    /**
    * Acquire the write-mutex.
    * Only one thread can hold the write-mutex, and it can acquire recursively.
    * Blocked if there are other threads holding the read-mutex or write-mutex.
    * Throws ISSE("Read-mutex is hold by the current thread") if the current thread holds the read-mutex.
    */
    @!APILevel[12, atomicservice : true]
    public func lock(): Unit
    
    /**
    * Try to acquire the write-mutex.
    * Return true if it succeeds to hold the write-mutex;
    * otherwise, return false.
    * This method does not obey the fairness constraints.
    */
    @!APILevel[12, atomicservice : true]
    public func tryLock(): Bool
    
    /**
    * Release the write-mutex.
    * This method should be invoked N times to fully unlock the mutex.
    * Under fair mode,
    *   if fully released, waiting reader threads are notified first to acquire the write-mutex.
    * Under nonfair mode,
    *   if fully released, notifying waiting reader or writer threads are decided by the implementation.
    * Throws ISSE("Mutex is not hold by the current thread") if the current thread does not hold this write-mutex.
    */
    @!APILevel[12, atomicservice : true]
    public func unlock(): Unit
}

@!APILevel[12, atomicservice : true]
public class Semaphore {
    /**
    * Construct a semephore with an initial value.
    * Throw IllegalArgumentException, if `count` is negative.
    */
    @!APILevel[12, atomicservice : true]
    public init(count: Int64)
    
    /**
    * Return the current available counts.
    */
    @!APILevel[12, atomicservice : true]
    public prop count: Int64
    
    /**
    * Acquire `amount` from the semaphore.
    * If there is no sufficient available counts,
    * the current thread will be blocked until enough counts become available.
    * The acquisition is atomic, which means it will never acquire partial counts before blocked.
    * Throw IllegalArgumentException, if `amount` is negative or larger than the initial value.
    * The acquisition order is not guranteed under contention.
    */
    @!APILevel[12, atomicservice : true]
    public func acquire(amount!: Int64 = 1): Unit
    
    /**
    * Try to acquire `amount` from the semaphore.
    * If there is no sufficient available counts, return false;
    * otherwise, return true.
    * Throw IllegalArgumentException, if `amount` is negative or larger than the initial value.
    */
    @!APILevel[12, atomicservice : true]
    public func tryAcquire(amount!: Int64 = 1): Bool
    
    /**
    * Release `amount` to the semaphore.
    * Wake blocked threads if there are enough counts for them.
    * The number of accumulated counts will never exceed the initial value.
    * Throw IllegalArgumentException, if `amount` is negative or larger than the initial value.
    * All events before `release()` happens-before events after `acquire/tryAcquire()`.
    */
    @!APILevel[12, atomicservice : true]
    public func release(amount!: Int64 = 1): Unit
}

@!APILevel[12, atomicservice : true]
public class SyncCounter {
    /**
    * Construct a counter with an initial value.
    * @throw IllegalArgumentException, if `count` is negative.
    */
    @!APILevel[12, atomicservice : true]
    public init(count: Int64)
    
    /**
    * Get the remaining count.
    */
    @!APILevel[12, atomicservice : true]
    public prop count: Int64
    
    /**
    * Decrease the count.
    * If the count becomes zero, wake up all blocked threads.
    * If the count is equal or less than zero, it has no effects.
    */
    @!APILevel[12, atomicservice : true]
    public func dec(): Unit
    
    /**
    * Wait until the count becomes zero or `timeout` passed.
    * If the count is zero, it will never be blocked.
    * All events before `dec()` happens-before events after `waitUntilZero()`.
    * NOTE: `timeout` should be `Duration.Max` by default,
    * however, the value `Duration.Max` is not supported currently.
    */
    @!APILevel[12, atomicservice : true]
    public func waitUntilZero(timeout!: Duration = Duration.Max): Unit
}

/**
* Timer is used to execute a specified task one or more times at a specified time point or after a specified interval.
* 1. Each Timer creates a new thread to execute the task associated with the Timer.
* 2. A Timer can be bound to only one task during initialization and cannot be reset after initialization.
* 3. A Timer ends its lifetime when the task associated with the Timer finish or are cancelled.
*    The Timer can then be reclaimed by the GC.
*    On the other hand, a Timer will not be reclaimed by GC in any case until the associated task have finished
*    or are actively cancelled, ensuring that the associated task will be executed.
* 4. If the system is busy, the task triggering time may be affected.
*    The Timer does not ensure that the task triggering time is punctual.
*    Timer ensures that tasks are executed if the trigger time of the task is less than or equal tothe current time.
*    Subsequent tasks will be postponed.
* 5. Timer does not actively catch the exception thrown by the associated task.
*    As long as the task has exceptions that are not caught, the Timer will be invalid.
*/
@!APILevel[12, atomicservice : true]
public class Timer <: Equatable<Timer> & Hashable {
    /**
    * Create a timer, the number of times the associated task is scheduled to run depends on its return value.
    * If @p delay duration is less than `Duration.Zero`, the associated task will be scheduled to run immediately.
    * If @p task return `Option.None`, the timer will stop to schedule the task to run,
    * if `Option.Some(v)` and `v > Duration.Zero`, the minimum interval before the next run will be set to v,
    * otherwise, the associated task will be scheduled to run immediately.
    *
    * @param delay The time from now until the associated task is scheduled to run for the first time.
    * @param task Associated task that are scheduled to run by the timer.
    *
    * @return A Timer instance.
    */
    @!APILevel[12, atomicservice : true]
    public static func after(delay: Duration, task: () -> Option<Duration>): Timer
    
    /**
    * Create a disposable timer, the associated task will be scheduled to run only once.
    * If @p delay duration is less than `Duration.Zero`, the associated task will be scheduled to run immediately.
    *
    * @param delay The time from now until the associated task is scheduled to run for the first time.
    * @param task Associated task that are scheduled to run by the timer.
    */
    @!APILevel[12, atomicservice : true]
    public static func once(delay: Duration, task: () -> Unit): Timer
    
    /**
    * Create a periodic timer, the associated task will be scheduled to run repeatedly.
    * If @p delay duration is less than `Duration.Zero`, the associated task will be scheduled to run immediately.
    *
    * @param delay The time from now until the associated task is scheduled to run for the first time.
    * @param interval The minimum interval between two execution times of the associated task.
    * @param task Associated task that are scheduled to run by the timer.
    * @param style The catchup style, default is Burst.
    *
    * @throw IllegalArgumentException if @p interval is less than or equal to `Duration.Zero`.
    */
    @!APILevel[12, atomicservice : true]
    public static func repeat(delay: Duration, interval: Duration, task: () -> Unit, style!: CatchupStyle = Burst): Timer
    
    /**
    * Create a periodic timer, the associated task will be scheduled to run repeatedly.
    * If @p delay time point is less than now, the associated task will be scheduled to run immediately.
    *
    * @param period Maximum duration for which the associated task can be scheduled since @p delay.
    * @param delay The time from now until the associated task is scheduled to run for the first time.
    * @param interval The minimum interval between two execution times of the associated task.
    * @param task Associated task that are scheduled to run by the timer.
    * @param style The catchup style, default is Burst.
    *
    * @throw IllegalArgumentException when the @p period is less than or equal to `Duration.Zero`,
    *                                 or @p interval is less than or equal to `Duration.Zero`.
    */
    @!APILevel[12, atomicservice : true]
    public static func repeatDuring(period: Duration, delay: Duration, interval: Duration, task: () -> Unit, style!: CatchupStyle = Burst): Timer
    
    /**
    * Create a periodic timer, the associated task will be scheduled to run repeatedly.
    * If @p delay duration is less than `Duration.Zero`, the associated task will be scheduled to run immediately.
    *
    * @param count Number of times the associated task is scheduled to run.
    * @param delay The time from now until the associated task is scheduled to run for the first time.
    * @param interval The minimum interval between two execution times of the associated task.
    * @param task Associated task that are scheduled to run by the timer.
    * @param style The catchup style, default is Burst.
    *
    * @throw IllegalArgumentException when @p count is less than or equal to 0,
    *                                 or @p interval is less than or equal to `Duration.Zero`.
    */
    @!APILevel[12, atomicservice : true]
    public static func repeatTimes(count: Int64, delay: Duration, interval: Duration, task: () -> Unit, style!: CatchupStyle = Burst): Timer
    
    /**
    * Cancel the timer, the associated task will no longer be scheduled to run.
    * If the associated task is running when calling `cancel`, cancel will not interrupt the current run.
    * The method does not block the current thread.
    * Calling `cancel` more than once is equal to calling only once.
    */
    @!APILevel[12, atomicservice : true]
    public func cancel(): Unit
    
    /**
    * Check if the two Timer instances are the same one actually.
    */
    @!APILevel[12, atomicservice : true]
    public operator func ==(rhs: Timer): Bool
    
    /**
    * Check if the two Timer instances are different actually.
    */
    @!APILevel[12, atomicservice : true]
    public operator func !=(rhs: Timer): Bool
    
    /**
    * Return the instance id as the hash code of the timer.
    */
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
}

/**
* The catchup style of the periodic timer.
*/
@!APILevel[12, atomicservice : true]
public enum CatchupStyle {
    @!APILevel[12, atomicservice : true]
    Delay |
    @!APILevel[12, atomicservice : true]
    Burst |
    @!APILevel[12, atomicservice : true]
    Skip
}

