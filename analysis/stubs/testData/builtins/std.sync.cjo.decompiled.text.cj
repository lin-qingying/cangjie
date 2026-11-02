// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.sync

import std.core.*
import std.time.MonoTime
import std.time.DateTime

let MAX_TIMEOUT_NS: Int64 = COMPILED_CODE /* compiled code */

let TIMER_BLOCK_BUFFER_SIZE: Int64 = COMPILED_CODE /* compiled code */

let TIMER_LIST_DEFAULT_CAPACITY: Int64 = COMPILED_CODE /* compiled code */

public class AtomicInt8 {
    private var value: Int8

    init(val: Int8) { /* compiled code */ }

    @Frozen
    public func load(): Int8 { /* compiled code */ }

    @Deprecated[message: "Use `public func load(): Int8` instead."]
    @Frozen
    public func load(memoryOrder!: std.sync.MemoryOrder): Int8 { /* compiled code */ }

    @Frozen
    public func store(val: Int8): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func store(val: Int8): Unit` instead."]
    public func store(val: Int8, memoryOrder!: std.sync.MemoryOrder): Unit { /* compiled code */ }

    @Frozen
    public func swap(val: Int8): Int8 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func swap(val: Int8): Int8` instead."]
    public func swap(val: Int8, memoryOrder!: std.sync.MemoryOrder): Int8 { /* compiled code */ }

    @Frozen
    public func compareAndSwap(old: Int8, new: Int8): Bool { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func compareAndSwap(old: Int8, new: Int8): Bool` instead."]
    public func compareAndSwap(old: Int8, new: Int8, successOrder!: std.sync.MemoryOrder, failureOrder!: std.sync.MemoryOrder): Bool { /* compiled code */ }

    @Frozen
    public func fetchAdd(val: Int8): Int8 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchAdd(val: Int8): Int8` instead."]
    public func fetchAdd(val: Int8, memoryOrder!: std.sync.MemoryOrder): Int8 { /* compiled code */ }

    @Frozen
    public func fetchSub(val: Int8): Int8 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchSub(val: Int8): Int8` instead."]
    public func fetchSub(val: Int8, memoryOrder!: std.sync.MemoryOrder): Int8 { /* compiled code */ }

    @Frozen
    public func fetchAnd(val: Int8): Int8 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchAnd(val: Int8): Int8` instead."]
    public func fetchAnd(val: Int8, memoryOrder!: std.sync.MemoryOrder): Int8 { /* compiled code */ }

    @Frozen
    public func fetchOr(val: Int8): Int8 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchOr(val: Int8): Int8` instead."]
    public func fetchOr(val: Int8, memoryOrder!: std.sync.MemoryOrder): Int8 { /* compiled code */ }

    @Frozen
    public func fetchXor(val: Int8): Int8 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchXor(val: Int8): Int8` instead."]
    public func fetchXor(val: Int8, memoryOrder!: std.sync.MemoryOrder): Int8 { /* compiled code */ }
}

@Deprecated
public enum MemoryOrder {
    | SeqCst
}

@Intrinsic
internal func load<ATOMIC_TYPE, TYPE>(atomicType: ATOMIC_TYPE, memoryOrder: std.sync.MemoryOrder): TYPE

@Deprecated
let DefaultMemoryOrder: std.sync.MemoryOrder

@Intrinsic
internal func store<ATOMIC_TYPE, TYPE>(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): Unit

@Intrinsic
internal func swap<ATOMIC_TYPE, TYPE>(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE

@Intrinsic
internal func compareAndSwap<ATOMIC_TYPE, TYPE>(atomicType: ATOMIC_TYPE, oldValue: TYPE, newValue: TYPE, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool

@Intrinsic
internal func fetchAdd<ATOMIC_TYPE, TYPE>(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE

@Intrinsic
internal func fetchSub<ATOMIC_TYPE, TYPE>(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE

@Intrinsic
internal func fetchAnd<ATOMIC_TYPE, TYPE>(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE

@Intrinsic
internal func fetchOr<ATOMIC_TYPE, TYPE>(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE

@Intrinsic
internal func fetchXor<ATOMIC_TYPE, TYPE>(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE

public class AtomicInt16 {
    private var value: Int16

    init(val: Int16) { /* compiled code */ }

    @Frozen
    public func load(): Int16 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func load(): Int16` instead."]
    public func load(memoryOrder!: std.sync.MemoryOrder): Int16 { /* compiled code */ }

    @Frozen
    public func store(val: Int16): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func store(val: Int16): Unit` instead."]
    public func store(val: Int16, memoryOrder!: std.sync.MemoryOrder): Unit { /* compiled code */ }

    @Frozen
    public func swap(val: Int16): Int16 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func swap(val: Int16): Int16` instead."]
    public func swap(val: Int16, memoryOrder!: std.sync.MemoryOrder): Int16 { /* compiled code */ }

    @Frozen
    public func compareAndSwap(old: Int16, new: Int16): Bool { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func compareAndSwap(old: Int16, new: Int16): Bool` instead."]
    public func compareAndSwap(old: Int16, new: Int16, successOrder!: std.sync.MemoryOrder, failureOrder!: std.sync.MemoryOrder): Bool { /* compiled code */ }

    @Frozen
    public func fetchAdd(val: Int16): Int16 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchAdd(val: Int16): Int16` instead."]
    public func fetchAdd(val: Int16, memoryOrder!: std.sync.MemoryOrder): Int16 { /* compiled code */ }

    @Frozen
    public func fetchSub(val: Int16): Int16 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchSub(val: Int16): Int16` instead."]
    public func fetchSub(val: Int16, memoryOrder!: std.sync.MemoryOrder): Int16 { /* compiled code */ }

    @Frozen
    public func fetchAnd(val: Int16): Int16 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchAnd(val: Int16): Int16` instead."]
    public func fetchAnd(val: Int16, memoryOrder!: std.sync.MemoryOrder): Int16 { /* compiled code */ }

    @Frozen
    public func fetchOr(val: Int16): Int16 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchOr(val: Int16): Int16` instead."]
    public func fetchOr(val: Int16, memoryOrder!: std.sync.MemoryOrder): Int16 { /* compiled code */ }

    @Frozen
    public func fetchXor(val: Int16): Int16 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchXor(val: Int16): Int16` instead."]
    public func fetchXor(val: Int16, memoryOrder!: std.sync.MemoryOrder): Int16 { /* compiled code */ }
}

public class AtomicInt32 {
    private var value: Int32

    init(val: Int32) { /* compiled code */ }

    @Frozen
    public func load(): Int32 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func load(): Int32` instead."]
    public func load(memoryOrder!: std.sync.MemoryOrder): Int32 { /* compiled code */ }

    @Frozen
    public func store(val: Int32): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func store(val: Int32): Unit` instead."]
    public func store(val: Int32, memoryOrder!: std.sync.MemoryOrder): Unit { /* compiled code */ }

    @Frozen
    public func swap(val: Int32): Int32 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func swap(val: Int32): Int32` instead."]
    public func swap(val: Int32, memoryOrder!: std.sync.MemoryOrder): Int32 { /* compiled code */ }

    @Frozen
    public func compareAndSwap(old: Int32, new: Int32): Bool { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func compareAndSwap(old: Int32, new: Int32): Bool` instead."]
    public func compareAndSwap(old: Int32, new: Int32, successOrder!: std.sync.MemoryOrder, failureOrder!: std.sync.MemoryOrder): Bool { /* compiled code */ }

    @Frozen
    public func fetchAdd(val: Int32): Int32 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchAdd(val: Int32): Int32` instead."]
    public func fetchAdd(val: Int32, memoryOrder!: std.sync.MemoryOrder): Int32 { /* compiled code */ }

    @Frozen
    public func fetchSub(val: Int32): Int32 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchSub(val: Int32): Int32` instead."]
    public func fetchSub(val: Int32, memoryOrder!: std.sync.MemoryOrder): Int32 { /* compiled code */ }

    @Frozen
    public func fetchAnd(val: Int32): Int32 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchAnd(val: Int32): Int32` instead."]
    public func fetchAnd(val: Int32, memoryOrder!: std.sync.MemoryOrder): Int32 { /* compiled code */ }

    @Frozen
    public func fetchOr(val: Int32): Int32 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchOr(val: Int32): Int32` instead."]
    public func fetchOr(val: Int32, memoryOrder!: std.sync.MemoryOrder): Int32 { /* compiled code */ }

    @Frozen
    public func fetchXor(val: Int32): Int32 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchXor(val: Int32): Int32` instead."]
    public func fetchXor(val: Int32, memoryOrder!: std.sync.MemoryOrder): Int32 { /* compiled code */ }
}

public class AtomicInt64 {
    private var value: Int64

    init(val: Int64) { /* compiled code */ }

    @Frozen
    public func load(): Int64 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func load(): Int64` instead."]
    public func load(memoryOrder!: std.sync.MemoryOrder): Int64 { /* compiled code */ }

    @Frozen
    public func store(val: Int64): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func store(val: Int64): Unit` instead."]
    public func store(val: Int64, memoryOrder!: std.sync.MemoryOrder): Unit { /* compiled code */ }

    @Frozen
    public func swap(val: Int64): Int64 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func swap(val: Int64): Int64` instead."]
    public func swap(val: Int64, memoryOrder!: std.sync.MemoryOrder): Int64 { /* compiled code */ }

    @Frozen
    public func compareAndSwap(old: Int64, new: Int64): Bool { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func compareAndSwap(old: Int64, new: Int64): Bool` instead."]
    public func compareAndSwap(old: Int64, new: Int64, successOrder!: std.sync.MemoryOrder, failureOrder!: std.sync.MemoryOrder): Bool { /* compiled code */ }

    @Frozen
    public func fetchAdd(val: Int64): Int64 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchAdd(val: Int64): Int64` instead."]
    public func fetchAdd(val: Int64, memoryOrder!: std.sync.MemoryOrder): Int64 { /* compiled code */ }

    @Frozen
    public func fetchSub(val: Int64): Int64 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchSub(val: Int64): Int64` instead."]
    public func fetchSub(val: Int64, memoryOrder!: std.sync.MemoryOrder): Int64 { /* compiled code */ }

    @Frozen
    public func fetchAnd(val: Int64): Int64 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchAnd(val: Int64): Int64` instead."]
    public func fetchAnd(val: Int64, memoryOrder!: std.sync.MemoryOrder): Int64 { /* compiled code */ }

    @Frozen
    public func fetchOr(val: Int64): Int64 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchOr(val: Int64): Int64` instead."]
    public func fetchOr(val: Int64, memoryOrder!: std.sync.MemoryOrder): Int64 { /* compiled code */ }

    @Frozen
    public func fetchXor(val: Int64): Int64 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchXor(val: Int64): Int64` instead."]
    public func fetchXor(val: Int64, memoryOrder!: std.sync.MemoryOrder): Int64 { /* compiled code */ }
}

public class AtomicUInt8 {
    private var value: UInt8

    init(val: UInt8) { /* compiled code */ }

    @Frozen
    public func load(): UInt8 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func load(): UInt8` instead."]
    public func load(memoryOrder!: std.sync.MemoryOrder): UInt8 { /* compiled code */ }

    @Frozen
    public func store(val: UInt8): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func store(val: UInt8): Unit` instead."]
    public func store(val: UInt8, memoryOrder!: std.sync.MemoryOrder): Unit { /* compiled code */ }

    @Frozen
    public func swap(val: UInt8): UInt8 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func swap(val: UInt8): UInt8` instead."]
    public func swap(val: UInt8, memoryOrder!: std.sync.MemoryOrder): UInt8 { /* compiled code */ }

    @Frozen
    public func compareAndSwap(old: UInt8, new: UInt8): Bool { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func compareAndSwap(old: UInt8, new: UInt8): Bool` instead."]
    public func compareAndSwap(old: UInt8, new: UInt8, successOrder!: std.sync.MemoryOrder, failureOrder!: std.sync.MemoryOrder): Bool { /* compiled code */ }

    @Frozen
    public func fetchAdd(val: UInt8): UInt8 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchAdd(val: UInt8): UInt8` instead."]
    public func fetchAdd(val: UInt8, memoryOrder!: std.sync.MemoryOrder): UInt8 { /* compiled code */ }

    @Frozen
    public func fetchSub(val: UInt8): UInt8 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchSub(val: UInt8): UInt8` instead."]
    public func fetchSub(val: UInt8, memoryOrder!: std.sync.MemoryOrder): UInt8 { /* compiled code */ }

    @Frozen
    public func fetchAnd(val: UInt8): UInt8 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchAnd(val: UInt8): UInt8` instead."]
    public func fetchAnd(val: UInt8, memoryOrder!: std.sync.MemoryOrder): UInt8 { /* compiled code */ }

    @Frozen
    public func fetchOr(val: UInt8): UInt8 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchOr(val: UInt8): UInt8` instead."]
    public func fetchOr(val: UInt8, memoryOrder!: std.sync.MemoryOrder): UInt8 { /* compiled code */ }

    @Frozen
    public func fetchXor(val: UInt8): UInt8 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchXor(val: UInt8): UInt8` instead."]
    public func fetchXor(val: UInt8, memoryOrder!: std.sync.MemoryOrder): UInt8 { /* compiled code */ }
}

public class AtomicUInt16 {
    private var value: UInt16

    init(val: UInt16) { /* compiled code */ }

    @Frozen
    public func load(): UInt16 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func load(): UInt16` instead."]
    public func load(memoryOrder!: std.sync.MemoryOrder): UInt16 { /* compiled code */ }

    @Frozen
    public func store(val: UInt16): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func store(val: UInt16): Unit` instead."]
    public func store(val: UInt16, memoryOrder!: std.sync.MemoryOrder): Unit { /* compiled code */ }

    @Frozen
    public func swap(val: UInt16): UInt16 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func swap(val: UInt16): UInt16` instead."]
    public func swap(val: UInt16, memoryOrder!: std.sync.MemoryOrder): UInt16 { /* compiled code */ }

    @Frozen
    public func compareAndSwap(old: UInt16, new: UInt16): Bool { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func compareAndSwap(old: UInt16, new: UInt16): Bool` instead."]
    public func compareAndSwap(old: UInt16, new: UInt16, successOrder!: std.sync.MemoryOrder, failureOrder!: std.sync.MemoryOrder): Bool { /* compiled code */ }

    @Frozen
    public func fetchAdd(val: UInt16): UInt16 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchAdd(val: UInt16): UInt16` instead."]
    public func fetchAdd(val: UInt16, memoryOrder!: std.sync.MemoryOrder): UInt16 { /* compiled code */ }

    @Frozen
    public func fetchSub(val: UInt16): UInt16 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchSub(val: UInt16): UInt16` instead."]
    public func fetchSub(val: UInt16, memoryOrder!: std.sync.MemoryOrder): UInt16 { /* compiled code */ }

    @Frozen
    public func fetchAnd(val: UInt16): UInt16 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchAnd(val: UInt16): UInt16` instead."]
    public func fetchAnd(val: UInt16, memoryOrder!: std.sync.MemoryOrder): UInt16 { /* compiled code */ }

    @Frozen
    public func fetchOr(val: UInt16): UInt16 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchOr(val: UInt16): UInt16` instead."]
    public func fetchOr(val: UInt16, memoryOrder!: std.sync.MemoryOrder): UInt16 { /* compiled code */ }

    @Frozen
    public func fetchXor(val: UInt16): UInt16 { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func fetchXor(val: UInt16): UInt16` instead."]
    public func fetchXor(val: UInt16, memoryOrder!: std.sync.MemoryOrder): UInt16 { /* compiled code */ }
}

public class AtomicUInt32 {
    private var value: UInt32

    init(val: UInt32) { /* compiled code */ }

    @Frozen
    public func load(): UInt32 { /* compiled code */ }

    @Deprecated[message: "Use `public func load(): UInt32` instead."]
    @Frozen
    public func load(memoryOrder!: std.sync.MemoryOrder): UInt32 { /* compiled code */ }

    @Frozen
    public func store(val: UInt32): Unit { /* compiled code */ }

    @Deprecated[message: "Use `public func store(val: UInt32): Unit` instead."]
    @Frozen
    public func store(val: UInt32, memoryOrder!: std.sync.MemoryOrder): Unit { /* compiled code */ }

    @Frozen
    public func swap(val: UInt32): UInt32 { /* compiled code */ }

    @Deprecated[message: "Use `public func swap(val: UInt32): UInt32` instead."]
    @Frozen
    public func swap(val: UInt32, memoryOrder!: std.sync.MemoryOrder): UInt32 { /* compiled code */ }

    @Frozen
    public func compareAndSwap(old: UInt32, new: UInt32): Bool { /* compiled code */ }

    @Deprecated[message: "Use `public func compareAndSwap(old: UInt32, new: UInt32): Bool` instead."]
    @Frozen
    public func compareAndSwap(old: UInt32, new: UInt32, successOrder!: std.sync.MemoryOrder, failureOrder!: std.sync.MemoryOrder): Bool { /* compiled code */ }

    @Frozen
    public func fetchAdd(val: UInt32): UInt32 { /* compiled code */ }

    @Deprecated[message: "Use `public func fetchAdd(val: UInt32): UInt32` instead."]
    @Frozen
    public func fetchAdd(val: UInt32, memoryOrder!: std.sync.MemoryOrder): UInt32 { /* compiled code */ }

    @Frozen
    public func fetchSub(val: UInt32): UInt32 { /* compiled code */ }

    @Deprecated[message: "Use `public func fetchSub(val: UInt32): UInt32` instead."]
    @Frozen
    public func fetchSub(val: UInt32, memoryOrder!: std.sync.MemoryOrder): UInt32 { /* compiled code */ }

    @Frozen
    public func fetchAnd(val: UInt32): UInt32 { /* compiled code */ }

    @Deprecated[message: "Use `public func fetchAnd(val: UInt32): UInt32` instead."]
    @Frozen
    public func fetchAnd(val: UInt32, memoryOrder!: std.sync.MemoryOrder): UInt32 { /* compiled code */ }

    @Frozen
    public func fetchOr(val: UInt32): UInt32 { /* compiled code */ }

    @Deprecated[message: "Use `public func fetchOr(val: UInt32): UInt32` instead."]
    @Frozen
    public func fetchOr(val: UInt32, memoryOrder!: std.sync.MemoryOrder): UInt32 { /* compiled code */ }

    @Frozen
    public func fetchXor(val: UInt32): UInt32 { /* compiled code */ }

    @Deprecated[message: "Use `public func fetchXor(val: UInt32): UInt32` instead."]
    @Frozen
    public func fetchXor(val: UInt32, memoryOrder!: std.sync.MemoryOrder): UInt32 { /* compiled code */ }
}

public class AtomicUInt64 {
    private var value: UInt64

    init(val: UInt64) { /* compiled code */ }

    @Frozen
    public func load(): UInt64 { /* compiled code */ }

    @Deprecated[message: "Use `public func load(): UInt64` instead."]
    @Frozen
    public func load(memoryOrder!: std.sync.MemoryOrder): UInt64 { /* compiled code */ }

    @Frozen
    public func store(val: UInt64): Unit { /* compiled code */ }

    @Deprecated[message: "Use `public func store(val: UInt64): Unit` instead."]
    @Frozen
    public func store(val: UInt64, memoryOrder!: std.sync.MemoryOrder): Unit { /* compiled code */ }

    @Frozen
    public func swap(val: UInt64): UInt64 { /* compiled code */ }

    @Deprecated[message: "Use `public func swap(val: UInt64): UInt64` instead."]
    @Frozen
    public func swap(val: UInt64, memoryOrder!: std.sync.MemoryOrder): UInt64 { /* compiled code */ }

    @Frozen
    public func compareAndSwap(old: UInt64, new: UInt64): Bool { /* compiled code */ }

    @Deprecated[message: "Use `public func compareAndSwap(old: UInt64, new: UInt64): Bool` instead."]
    @Frozen
    public func compareAndSwap(old: UInt64, new: UInt64, successOrder!: std.sync.MemoryOrder, failureOrder!: std.sync.MemoryOrder): Bool { /* compiled code */ }

    @Frozen
    public func fetchAdd(val: UInt64): UInt64 { /* compiled code */ }

    @Deprecated[message: "Use `public func fetchAdd(val: UInt64): UInt64` instead."]
    @Frozen
    public func fetchAdd(val: UInt64, memoryOrder!: std.sync.MemoryOrder): UInt64 { /* compiled code */ }

    @Frozen
    public func fetchSub(val: UInt64): UInt64 { /* compiled code */ }

    @Deprecated[message: "Use `public func fetchSub(val: UInt64): UInt64` instead."]
    @Frozen
    public func fetchSub(val: UInt64, memoryOrder!: std.sync.MemoryOrder): UInt64 { /* compiled code */ }

    @Frozen
    public func fetchAnd(val: UInt64): UInt64 { /* compiled code */ }

    @Deprecated[message: "Use `public func fetchAnd(val: UInt64): UInt64` instead."]
    @Frozen
    public func fetchAnd(val: UInt64, memoryOrder!: std.sync.MemoryOrder): UInt64 { /* compiled code */ }

    @Frozen
    public func fetchOr(val: UInt64): UInt64 { /* compiled code */ }

    @Deprecated[message: "Use `public func fetchOr(val: UInt64): UInt64` instead."]
    @Frozen
    public func fetchOr(val: UInt64, memoryOrder!: std.sync.MemoryOrder): UInt64 { /* compiled code */ }

    @Frozen
    public func fetchXor(val: UInt64): UInt64 { /* compiled code */ }

    @Deprecated[message: "Use `public func fetchXor(val: UInt64): UInt64` instead."]
    @Frozen
    public func fetchXor(val: UInt64, memoryOrder!: std.sync.MemoryOrder): UInt64 { /* compiled code */ }
}

public class AtomicBool {
    private var value: Int8

    init(val: Bool) { /* compiled code */ }

    @Frozen
    public func load(): Bool { /* compiled code */ }

    @Deprecated[message: "Use `public func load(): Bool` instead."]
    @Frozen
    public func load(memoryOrder!: std.sync.MemoryOrder): Bool { /* compiled code */ }

    @Frozen
    public func store(val: Bool): Unit { /* compiled code */ }

    @Deprecated[message: "Use `public func store(val: Bool): Unit` instead."]
    @Frozen
    public func store(val: Bool, memoryOrder!: std.sync.MemoryOrder): Unit { /* compiled code */ }

    @Frozen
    public func swap(val: Bool): Bool { /* compiled code */ }

    @Deprecated[message: "Use `public func swap(val: Bool): Bool` instead."]
    @Frozen
    public func swap(val: Bool, memoryOrder!: std.sync.MemoryOrder): Bool { /* compiled code */ }

    @Frozen
    public func compareAndSwap(old: Bool, new: Bool): Bool { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func compareAndSwap(old: Bool, new: Bool): Bool` instead."]
    public func compareAndSwap(old: Bool, new: Bool, successOrder!: std.sync.MemoryOrder, failureOrder!: std.sync.MemoryOrder): Bool { /* compiled code */ }

    private func transBoolToInt8(v: Bool): Int8 { /* compiled code */ }

    private func transInt8ToBool(v: Int8): Bool { /* compiled code */ }
}

public class AtomicReference<T> {
    private var value4cjnative: T

    init(val: T) { /* compiled code */ }

    @Frozen
    public func load(): T { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func load(): T` instead."]
    public func load(memoryOrder!: std.sync.MemoryOrder): T { /* compiled code */ }

    @Frozen
    public func store(val: T): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func store(val: T): Unit` instead."]
    public func store(val: T, memoryOrder!: std.sync.MemoryOrder): Unit { /* compiled code */ }

    @Frozen
    public func swap(val: T): T { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func swap(val: T): T` instead."]
    public func swap(val: T, memoryOrder!: std.sync.MemoryOrder): T { /* compiled code */ }

    @Frozen
    public func compareAndSwap(old: T, new: T): Bool { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func compareAndSwap(old: T, new: T): Bool` instead."]
    public func compareAndSwap(old: T, new: T, successOrder!: std.sync.MemoryOrder, failureOrder!: std.sync.MemoryOrder): Bool { /* compiled code */ }
}

public class AtomicOptionReference<T> {
    private var value: std.core.Option<T> = COMPILED_CODE /* compiled code */

    @Frozen
    init() { /* compiled code */ }

    @Frozen
    init(val: std.core.Option<T>) { /* compiled code */ }

    @Frozen
    public func load(): std.core.Option<T> { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func load(): Option<T>` instead."]
    public func load(memoryOrder!: std.sync.MemoryOrder): std.core.Option<T> { /* compiled code */ }

    @Frozen
    public func store(val: std.core.Option<T>): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func store(val: Option<T>): Unit` instead."]
    public func store(val: std.core.Option<T>, memoryOrder!: std.sync.MemoryOrder): Unit { /* compiled code */ }

    @Frozen
    public func swap(val: std.core.Option<T>): std.core.Option<T> { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func swap(val: Option<T>): Option<T>` instead."]
    public func swap(val: std.core.Option<T>, memoryOrder!: std.sync.MemoryOrder): std.core.Option<T> { /* compiled code */ }

    @Frozen
    public func compareAndSwap(old: std.core.Option<T>, new: std.core.Option<T>): Bool { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use `public func compareAndSwap(old: Option<T>, new: Option<T>): Bool` instead."]
    public func compareAndSwap(old: std.core.Option<T>, new: std.core.Option<T>, successOrder!: std.sync.MemoryOrder, failureOrder!: std.sync.MemoryOrder): Bool { /* compiled code */ }
}

public class Barrier {
    private let syncCounter: std.sync.SyncCounter

    init(count: Int64) { /* compiled code */ }

    public func wait(timeout!: std.core.Duration = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }
}

public class SyncCounter {
    private var countValue: std.sync.AtomicInt64

    private let syncList: std.sync.SyncList

    init(count: Int64) { /* compiled code */ }

    public prop count: Int64 {
        get() { /* compiled code */ }
    }

    public func dec(): Unit { /* compiled code */ }

    public func waitUntilZero(timeout!: std.core.Duration = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }
}

@Deprecated[message: "Use `public interface Lock` instead."]
public interface IReentrantMutex {
    public open func lock(): Unit

    public open func tryLock(): Bool

    public open func unlock(): Unit
}

public class IllegalSynchronizationStateException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public interface Lock {
    public open func lock(): Unit

    public open func tryLock(): Bool

    public open func unlock(): Unit
}

public interface UniqueLock <: std.sync.Lock {
    public open func condition(): std.sync.Condition
}

public interface Condition {
    public open func wait(): Unit

    public open func wait(timeout!: std.core.Duration): Bool

    public open func waitUntil(predicate: () -> Bool): Unit

    public open func waitUntil(predicate: () -> Bool, timeout!: std.core.Duration): Bool

    public open func notify(): Unit

    public open func notifyAll(): Unit
}

@Deprecated[message: "Use `public interface Condition` instead."]
public class Monitor <: std.sync.ReentrantMutex {
    init() { /* compiled code */ }

    init(mutex: std.sync.Mutex) { /* compiled code */ }

    public func wait(timeout!: std.core.Duration = COMPILED_CODE /* compiled code */): Bool { /* compiled code */ }

    public func notify(): Unit { /* compiled code */ }

    public func notifyAll(): Unit { /* compiled code */ }
}

public class Mutex <: std.core.Object & std.sync.UniqueLock {
    init() { /* compiled code */ }

    public func lock(): Unit { /* compiled code */ }

    public func tryLock(): Bool { /* compiled code */ }

    public func unlock(): Unit { /* compiled code */ }

    public func condition(): std.sync.Condition { /* compiled code */ }

    internal func checkMutexStatus(): Unit { /* compiled code */ }
}

@Deprecated[message: "Use `public class Mutex` instead."]
public open class ReentrantMutex <: std.core.Object & std.sync.Lock {
    private let mutex: std.sync.Mutex

    init() { /* compiled code */ }

    init(mutex: std.sync.Mutex) { /* compiled code */ }

    public open func lock(): Unit { /* compiled code */ }

    public open func tryLock(): Bool { /* compiled code */ }

    public open func unlock(): Unit { /* compiled code */ }

    internal func checkMutexStatus(): Unit { /* compiled code */ }
}

@Deprecated[message: "Use `public interface Condition` instead."]
public struct ConditionID {
    init(m: std.sync.MultiConditionMonitor, wqPtr: std.sync.WaitQueue) { /* compiled code */ }

    internal let monitor: std.sync.MultiConditionMonitor

    internal let waitQueue: std.sync.WaitQueue
}

@Deprecated[message: "Use `public class Mutex` instead."]
public class MultiConditionMonitor <: std.sync.ReentrantMutex {
    init() { /* compiled code */ }

    public func newCondition(): std.sync.ConditionID { /* compiled code */ }

    public func wait(condID: std.sync.ConditionID, timeout!: std.core.Duration = COMPILED_CODE /* compiled code */): Bool { /* compiled code */ }

    public func notify(condID: std.sync.ConditionID): Unit { /* compiled code */ }

    public func notifyAll(condID: std.sync.ConditionID): Unit { /* compiled code */ }
}

internal class WaitQueue {
    init() { /* compiled code */ }
}

internal class MutexCondition <: std.core.Object & std.sync.Condition {
    private let condition: std.sync.Monitor

    init(mutex: std.sync.Mutex) { /* compiled code */ }

    public func wait(): Unit { /* compiled code */ }

    public func wait(timeout!: std.core.Duration): Bool { /* compiled code */ }

    public func waitUntil(predicate: () -> Bool): Unit { /* compiled code */ }

    public func waitUntil(predicate: () -> Bool, timeout!: std.core.Duration): Bool { /* compiled code */ }

    public func notify(): Unit { /* compiled code */ }

    public func notifyAll(): Unit { /* compiled code */ }

    internal func getTimeoutNanos(timeout: std.core.Duration): Int64 { /* compiled code */ }
}

@Intrinsic
internal func mutexInit(mutex: std.sync.Mutex): Bool

@Intrinsic
internal func mutexLock(mutex: std.sync.Mutex): Unit

@Intrinsic
internal func mutexTryLock(mutex: std.sync.Mutex): Bool

@Intrinsic
internal func mutexCheckStatus(mutex: std.sync.Mutex): Bool

@Intrinsic
internal func mutexUnlock(mutex: std.sync.Mutex): Unit

@Intrinsic
internal func monitorWait(monitor: std.sync.Monitor, timeout: Int64): Bool

@Intrinsic
internal func monitorNotify(monitor: std.sync.Monitor): Unit

@Intrinsic
internal func monitorNotifyAll(monitor: std.sync.Monitor): Unit

@Intrinsic
internal func multiConditionMonitorWait(multiConditionMonitor: std.sync.MultiConditionMonitor, obj: std.core.Object, timeout: Int64): Bool

@Intrinsic
internal func multiConditionMonitorNotify(multiConditionMonitor: std.sync.MultiConditionMonitor, obj: std.core.Object): Unit

@Intrinsic
internal func multiConditionMonitorNotifyAll(multiConditionMonitor: std.sync.MultiConditionMonitor, obj: std.core.Object): Unit

@Intrinsic
internal func waitQueueInit(obj: std.sync.WaitQueue): Bool

@Intrinsic
internal func monitorInit(obj: std.sync.Monitor): Bool

public class ReadWriteLock {
    private let _fair: Bool

    private let _readLock: std.sync.Lock

    private let _writeLock: std.sync.UniqueLock

    public prop readLock: std.sync.Lock {
        get() { /* compiled code */ }
    }

    public prop writeLock: std.sync.UniqueLock {
        get() { /* compiled code */ }
    }

    init(fair!: Bool = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    public func isFair(): Bool { /* compiled code */ }
}

internal class ReadLock <: std.core.Object & std.sync.Lock {
    public func lock(): Unit { /* compiled code */ }

    public func tryLock(): Bool { /* compiled code */ }

    public func unlock(): Unit { /* compiled code */ }

    private let _readWriteLockImpl: std.sync.ReadWriteLockImpl

    ReadLock(_readWriteLockImpl: std.sync.ReadWriteLockImpl) { /* compiled code */ }
}

internal class ReadWriteLockImpl {
    internal let isFair: Bool

    internal let state: std.sync.AtomicUInt64

    init(isFair!: Bool) { /* compiled code */ }

    internal func getWriteCount(): Int64 { /* compiled code */ }

    private let writeOwner: std.sync.AtomicInt64

    private let waitingWriters: std.sync.AtomicInt64

    private let threadReadCount: std.core.ThreadLocal<Int64>

    private let readerSyncList: std.sync.SyncList

    private let writerSyncList: std.sync.SyncList

    internal func readLock(): Unit { /* compiled code */ }

    internal func tryReadLock(): Bool { /* compiled code */ }

    internal func readUnlock(): Unit { /* compiled code */ }

    internal func writeLock(writeCount!: Int64 = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    internal func tryWriteLock(): Bool { /* compiled code */ }

    internal func checkWriteLockStatus(): Unit { /* compiled code */ }

    internal func writeUnlock(): Unit { /* compiled code */ }

    internal func writeUnlock(writeCount!: Int64): Unit { /* compiled code */ }
}

internal class WriteLock <: std.core.Object & std.sync.UniqueLock {
    public func lock(): Unit { /* compiled code */ }

    public func tryLock(): Bool { /* compiled code */ }

    public func unlock(): Unit { /* compiled code */ }

    public func condition(): std.sync.Condition { /* compiled code */ }

    private let _readWriteLockImpl: std.sync.ReadWriteLockImpl

    WriteLock(_readWriteLockImpl: std.sync.ReadWriteLockImpl) { /* compiled code */ }
}

internal class ReadWriteLockCondition <: std.core.Object & std.sync.Condition {
    private let waitQueue: std.sync.SyncList

    public func wait(): Unit { /* compiled code */ }

    public func wait(timeout!: std.core.Duration): Bool { /* compiled code */ }

    public func waitUntil(predicate: () -> Bool): Unit { /* compiled code */ }

    public func waitUntil(predicate: () -> Bool, timeout!: std.core.Duration): Bool { /* compiled code */ }

    public func notify(): Unit { /* compiled code */ }

    public func notifyAll(): Unit { /* compiled code */ }

    private let lock: std.sync.ReadWriteLockImpl

    ReadWriteLockCondition(lock: std.sync.ReadWriteLockImpl) { /* compiled code */ }
}

internal class SyncList {
    private var head: std.core.Option<std.sync.SyncNode>

    private let m: std.sync.Monitor

    internal func waitIf(pred: () -> Bool, timeout!: std.core.Duration = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    internal func waitInLock(unlock: () -> Unit, timeout!: std.core.Duration = COMPILED_CODE /* compiled code */): Bool { /* compiled code */ }

    internal func notifyOne(): Bool { /* compiled code */ }

    internal func notifyAll(): Bool { /* compiled code */ }

    init() { /* compiled code */ }
}

@Deprecated
public enum ReadWriteMutexMode {
    | Unfair

    | Fair
}

@Deprecated[message: "Use `public class ReadWriteLock` instead."]
public class ReentrantReadWriteMutex {
    private let readMutex_: std.sync.ReentrantReadMutex

    private let writeMutex_: std.sync.ReentrantWriteMutex

    init(mode!: std.sync.ReadWriteMutexMode = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    public prop readMutex: std.sync.ReentrantReadMutex {
        get() { /* compiled code */ }
    }

    public prop writeMutex: std.sync.ReentrantWriteMutex {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "Use `public interface Lock` instead."]
public class ReentrantReadMutex <: std.sync.ReentrantMutex {
    private let rwMutex: std.sync.ReadWriteLockImpl

    init(rwMutex: std.sync.ReadWriteLockImpl) { /* compiled code */ }

    public func lock(): Unit { /* compiled code */ }

    public func tryLock(): Bool { /* compiled code */ }

    public func unlock(): Unit { /* compiled code */ }
}

@Deprecated[message: "Use `public interface UniqueLock` instead."]
public class ReentrantWriteMutex <: std.sync.ReentrantMutex {
    private let rwMutex: std.sync.ReadWriteLockImpl

    init(rwMutex: std.sync.ReadWriteLockImpl) { /* compiled code */ }

    public func lock(): Unit { /* compiled code */ }

    public func tryLock(): Bool { /* compiled code */ }

    public func unlock(): Unit { /* compiled code */ }
}

public class Semaphore {
    private var countValue: std.sync.AtomicInt64

    private let monitor: std.sync.Monitor

    private let initValue: Int64

    init(count: Int64) { /* compiled code */ }

    public prop count: Int64 {
        get() { /* compiled code */ }
    }

    public func acquire(amount!: Int64 = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    public func tryAcquire(amount!: Int64 = COMPILED_CODE /* compiled code */): Bool { /* compiled code */ }

    public func release(amount!: Int64 = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }
}

let MAX_TIMEOUT_DURATION: std.core.Duration

internal class NotifyOnce {
    internal let m: std.sync.Monitor

    internal var notified: Bool

    internal func notify(): Unit { /* compiled code */ }

    internal func wait(timeout!: std.core.Duration = COMPILED_CODE /* compiled code */): Bool { /* compiled code */ }

    init() { /* compiled code */ }
}

internal class SyncNode {
    internal let n: std.sync.NotifyOnce

    internal var next: std.core.Option<std.sync.SyncNode>

    init() { /* compiled code */ }
}

public class Timer <: std.core.Object & std.core.Equatable<std.sync.Timer> & std.core.Hashable {
    private let id: Int64

    private let scheduler: std.sync.TimerScheduler

    internal let task: () -> std.core.Option<std.core.Duration>

    internal var endTime: std.time.MonoTime

    internal var isEnabled: Bool

    init(delay: std.core.Duration, task: () -> std.core.Option<std.core.Duration>) { /* compiled code */ }

    public static func after(delay: std.core.Duration, task: () -> std.core.Option<std.core.Duration>): std.sync.Timer { /* compiled code */ }

    public static func once(delay: std.core.Duration, task: () -> Unit): std.sync.Timer { /* compiled code */ }

    public static func repeat(delay: std.core.Duration, interval: std.core.Duration, task: () -> Unit, style!: std.sync.CatchupStyle = COMPILED_CODE /* compiled code */): std.sync.Timer { /* compiled code */ }

    public static func repeatDuring(period: std.core.Duration, delay: std.core.Duration, interval: std.core.Duration, task: () -> Unit, style!: std.sync.CatchupStyle = COMPILED_CODE /* compiled code */): std.sync.Timer { /* compiled code */ }

    public static func repeatTimes(count: Int64, delay: std.core.Duration, interval: std.core.Duration, task: () -> Unit, style!: std.sync.CatchupStyle = COMPILED_CODE /* compiled code */): std.sync.Timer { /* compiled code */ }

    public func cancel(): Unit { /* compiled code */ }

    public operator func ==(rhs: std.sync.Timer): Bool { /* compiled code */ }

    public operator func !=(rhs: std.sync.Timer): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    internal func reload(duration: std.core.Duration): Unit { /* compiled code */ }
}

internal class TimerScheduler {
    internal static func getScheduler(id: Int64): std.sync.TimerScheduler { /* compiled code */ }

    private let queue: std.sync.TimerQueue

    private var heap: std.sync.TimerHeap

    private let monitor: std.sync.Monitor

    private let isRunning: std.sync.AtomicBool

    private let cancelCount: std.sync.AtomicInt64

    internal func add(timer: std.sync.Timer): std.core.Any { /* compiled code */ }

    internal func addCancelCount(): Int64 { /* compiled code */ }

    init() { /* compiled code */ }
}

public enum CatchupStyle {
    | Delay

    | Burst

    | Skip
}

internal func delayStyleTick(interval: std.core.Duration): std.core.Option<std.core.Duration> { /* compiled code */ }

internal func burstStyleTick(delay: std.core.Duration, interval: std.core.Duration): std.core.Option<std.core.Duration> { /* compiled code */ }

internal func skipStyleTick(delay: std.core.Duration, interval: std.core.Duration): std.core.Option<std.core.Duration> { /* compiled code */ }

internal func tickDuring(delay: std.core.Duration, period: std.core.Duration, nextTick: () -> std.core.Option<std.core.Duration>): std.core.Option<std.core.Duration> { /* compiled code */ }

internal func tickTimes(count: Int64, nextTick: () -> std.core.Option<std.core.Duration>): std.core.Option<std.core.Duration> { /* compiled code */ }

internal class TimerQueue {
    private let front: std.sync.AtomicReference<std.sync.TimerBlock>

    private let rear: std.sync.AtomicReference<std.sync.TimerBlock>

    init() { /* compiled code */ }

    internal func add(element: std.sync.Timer): Unit { /* compiled code */ }

    internal func remove(): std.core.Option<std.sync.Timer> { /* compiled code */ }

    internal func peek(): std.core.Option<std.sync.Timer> { /* compiled code */ }

    internal func isEmpty(): Bool { /* compiled code */ }
}

internal class TimerHeap {
    private let data: std.sync.TimerList

    internal prop size: Int64 {
        get() { /* compiled code */ }
    }

    internal prop isEmpty: Bool {
        get() { /* compiled code */ }
    }

    internal func add(e: std.sync.Timer): Unit { /* compiled code */ }

    internal func top(): std.sync.Timer { /* compiled code */ }

    internal func poll(): std.sync.Timer { /* compiled code */ }

    init() { /* compiled code */ }
}

internal class TimerList {
    private var _data: std.core.Array<std.sync.Timer>

    private var _size: Int64

    init() { /* compiled code */ }

    internal func add(element: std.sync.Timer): Unit { /* compiled code */ }

    internal func remove(at!: Int64): std.sync.Timer { /* compiled code */ }

    internal operator func [](index: Int64): std.sync.Timer { /* compiled code */ }

    internal operator func [](index: Int64, value!: std.sync.Timer): Unit { /* compiled code */ }

    internal func isEmpty(): Bool { /* compiled code */ }

    internal prop size: Int64 {
        get() { /* compiled code */ }
    }
}

internal enum PushResult {
    | BlockDone

    | Conflict

    | Succeed
}

internal enum PopResult {
    | BlockDone

    | NoEntry

    | Element(std.sync.Timer)
}

internal class TimerBlock {
    internal let isExtended: std.sync.AtomicBool

    internal let next: std.sync.AtomicOptionReference<std.sync.TimerBlock>

    private let allocated: std.sync.AtomicInt64

    internal let committed: std.sync.AtomicInt64

    internal let reserved: std.sync.AtomicInt64

    internal let buffer: std.core.Array<std.sync.Timer>

    internal func push(element: std.sync.Timer): Bool { /* compiled code */ }

    internal func pop(): std.sync.PopResult { /* compiled code */ }

    internal func front(): std.sync.PopResult { /* compiled code */ }

    internal let version: UInt64

    TimerBlock(version: UInt64) { /* compiled code */ }
}

internal func newBlock(tailBlock: std.sync.TimerBlock, tail: std.sync.AtomicReference<std.sync.TimerBlock>): Unit { /* compiled code */ }

internal func nextBlock(headBlock: std.sync.TimerBlock, head: std.sync.AtomicReference<std.sync.TimerBlock>, tail: std.sync.AtomicReference<std.sync.TimerBlock>): Bool { /* compiled code */ }
