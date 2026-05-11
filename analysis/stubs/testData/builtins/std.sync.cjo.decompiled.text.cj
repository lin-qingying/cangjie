// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.sync

import std.core.*
import std.time.MonoTime
import std.time.DateTime

let MAX_TIMEOUT_NS: Int64

let TIMER_BLOCK_BUFFER_SIZE: Int64

let TIMER_LIST_DEFAULT_CAPACITY: Int64

class AtomicInt8 {
    var value: Int8

    init(val: Int8) { /* compiled code */ }

    func load(): Int8 { /* compiled code */ }

    func load(memoryOrder: std.sync.MemoryOrder): Int8 { /* compiled code */ }

    func store(val: Int8): Unit { /* compiled code */ }

    func store(val: Int8, memoryOrder: std.sync.MemoryOrder): Unit { /* compiled code */ }

    func swap(val: Int8): Int8 { /* compiled code */ }

    func swap(val: Int8, memoryOrder: std.sync.MemoryOrder): Int8 { /* compiled code */ }

    func compareAndSwap(old: Int8, new: Int8): Bool { /* compiled code */ }

    func compareAndSwap(old: Int8, new: Int8, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool { /* compiled code */ }

    func fetchAdd(val: Int8): Int8 { /* compiled code */ }

    func fetchAdd(val: Int8, memoryOrder: std.sync.MemoryOrder): Int8 { /* compiled code */ }

    func fetchSub(val: Int8): Int8 { /* compiled code */ }

    func fetchSub(val: Int8, memoryOrder: std.sync.MemoryOrder): Int8 { /* compiled code */ }

    func fetchAnd(val: Int8): Int8 { /* compiled code */ }

    func fetchAnd(val: Int8, memoryOrder: std.sync.MemoryOrder): Int8 { /* compiled code */ }

    func fetchOr(val: Int8): Int8 { /* compiled code */ }

    func fetchOr(val: Int8, memoryOrder: std.sync.MemoryOrder): Int8 { /* compiled code */ }

    func fetchXor(val: Int8): Int8 { /* compiled code */ }

    func fetchXor(val: Int8, memoryOrder: std.sync.MemoryOrder): Int8 { /* compiled code */ }
}

enum MemoryOrder {
    | SeqCst
}

func load(atomicType: ATOMIC_TYPE, memoryOrder: std.sync.MemoryOrder): TYPE { /* compiled code */ }

let DefaultMemoryOrder: std.sync.MemoryOrder

func store(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): Unit { /* compiled code */ }

func swap(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE { /* compiled code */ }

func compareAndSwap(atomicType: ATOMIC_TYPE, oldValue: TYPE, newValue: TYPE, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool { /* compiled code */ }

func fetchAdd(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE { /* compiled code */ }

func fetchSub(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE { /* compiled code */ }

func fetchAnd(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE { /* compiled code */ }

func fetchOr(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE { /* compiled code */ }

func fetchXor(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE { /* compiled code */ }

class AtomicInt16 {
    var value: Int16

    init(val: Int16) { /* compiled code */ }

    func load(): Int16 { /* compiled code */ }

    func load(memoryOrder: std.sync.MemoryOrder): Int16 { /* compiled code */ }

    func store(val: Int16): Unit { /* compiled code */ }

    func store(val: Int16, memoryOrder: std.sync.MemoryOrder): Unit { /* compiled code */ }

    func swap(val: Int16): Int16 { /* compiled code */ }

    func swap(val: Int16, memoryOrder: std.sync.MemoryOrder): Int16 { /* compiled code */ }

    func compareAndSwap(old: Int16, new: Int16): Bool { /* compiled code */ }

    func compareAndSwap(old: Int16, new: Int16, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool { /* compiled code */ }

    func fetchAdd(val: Int16): Int16 { /* compiled code */ }

    func fetchAdd(val: Int16, memoryOrder: std.sync.MemoryOrder): Int16 { /* compiled code */ }

    func fetchSub(val: Int16): Int16 { /* compiled code */ }

    func fetchSub(val: Int16, memoryOrder: std.sync.MemoryOrder): Int16 { /* compiled code */ }

    func fetchAnd(val: Int16): Int16 { /* compiled code */ }

    func fetchAnd(val: Int16, memoryOrder: std.sync.MemoryOrder): Int16 { /* compiled code */ }

    func fetchOr(val: Int16): Int16 { /* compiled code */ }

    func fetchOr(val: Int16, memoryOrder: std.sync.MemoryOrder): Int16 { /* compiled code */ }

    func fetchXor(val: Int16): Int16 { /* compiled code */ }

    func fetchXor(val: Int16, memoryOrder: std.sync.MemoryOrder): Int16 { /* compiled code */ }
}

class AtomicInt32 {
    var value: Int32

    init(val: Int32) { /* compiled code */ }

    func load(): Int32 { /* compiled code */ }

    func load(memoryOrder: std.sync.MemoryOrder): Int32 { /* compiled code */ }

    func store(val: Int32): Unit { /* compiled code */ }

    func store(val: Int32, memoryOrder: std.sync.MemoryOrder): Unit { /* compiled code */ }

    func swap(val: Int32): Int32 { /* compiled code */ }

    func swap(val: Int32, memoryOrder: std.sync.MemoryOrder): Int32 { /* compiled code */ }

    func compareAndSwap(old: Int32, new: Int32): Bool { /* compiled code */ }

    func compareAndSwap(old: Int32, new: Int32, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool { /* compiled code */ }

    func fetchAdd(val: Int32): Int32 { /* compiled code */ }

    func fetchAdd(val: Int32, memoryOrder: std.sync.MemoryOrder): Int32 { /* compiled code */ }

    func fetchSub(val: Int32): Int32 { /* compiled code */ }

    func fetchSub(val: Int32, memoryOrder: std.sync.MemoryOrder): Int32 { /* compiled code */ }

    func fetchAnd(val: Int32): Int32 { /* compiled code */ }

    func fetchAnd(val: Int32, memoryOrder: std.sync.MemoryOrder): Int32 { /* compiled code */ }

    func fetchOr(val: Int32): Int32 { /* compiled code */ }

    func fetchOr(val: Int32, memoryOrder: std.sync.MemoryOrder): Int32 { /* compiled code */ }

    func fetchXor(val: Int32): Int32 { /* compiled code */ }

    func fetchXor(val: Int32, memoryOrder: std.sync.MemoryOrder): Int32 { /* compiled code */ }
}

class AtomicInt64 {
    var value: Int64

    init(val: Int64) { /* compiled code */ }

    func load(): Int64 { /* compiled code */ }

    func load(memoryOrder: std.sync.MemoryOrder): Int64 { /* compiled code */ }

    func store(val: Int64): Unit { /* compiled code */ }

    func store(val: Int64, memoryOrder: std.sync.MemoryOrder): Unit { /* compiled code */ }

    func swap(val: Int64): Int64 { /* compiled code */ }

    func swap(val: Int64, memoryOrder: std.sync.MemoryOrder): Int64 { /* compiled code */ }

    func compareAndSwap(old: Int64, new: Int64): Bool { /* compiled code */ }

    func compareAndSwap(old: Int64, new: Int64, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool { /* compiled code */ }

    func fetchAdd(val: Int64): Int64 { /* compiled code */ }

    func fetchAdd(val: Int64, memoryOrder: std.sync.MemoryOrder): Int64 { /* compiled code */ }

    func fetchSub(val: Int64): Int64 { /* compiled code */ }

    func fetchSub(val: Int64, memoryOrder: std.sync.MemoryOrder): Int64 { /* compiled code */ }

    func fetchAnd(val: Int64): Int64 { /* compiled code */ }

    func fetchAnd(val: Int64, memoryOrder: std.sync.MemoryOrder): Int64 { /* compiled code */ }

    func fetchOr(val: Int64): Int64 { /* compiled code */ }

    func fetchOr(val: Int64, memoryOrder: std.sync.MemoryOrder): Int64 { /* compiled code */ }

    func fetchXor(val: Int64): Int64 { /* compiled code */ }

    func fetchXor(val: Int64, memoryOrder: std.sync.MemoryOrder): Int64 { /* compiled code */ }
}

class AtomicUInt8 {
    var value: UInt8

    init(val: UInt8) { /* compiled code */ }

    func load(): UInt8 { /* compiled code */ }

    func load(memoryOrder: std.sync.MemoryOrder): UInt8 { /* compiled code */ }

    func store(val: UInt8): Unit { /* compiled code */ }

    func store(val: UInt8, memoryOrder: std.sync.MemoryOrder): Unit { /* compiled code */ }

    func swap(val: UInt8): UInt8 { /* compiled code */ }

    func swap(val: UInt8, memoryOrder: std.sync.MemoryOrder): UInt8 { /* compiled code */ }

    func compareAndSwap(old: UInt8, new: UInt8): Bool { /* compiled code */ }

    func compareAndSwap(old: UInt8, new: UInt8, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool { /* compiled code */ }

    func fetchAdd(val: UInt8): UInt8 { /* compiled code */ }

    func fetchAdd(val: UInt8, memoryOrder: std.sync.MemoryOrder): UInt8 { /* compiled code */ }

    func fetchSub(val: UInt8): UInt8 { /* compiled code */ }

    func fetchSub(val: UInt8, memoryOrder: std.sync.MemoryOrder): UInt8 { /* compiled code */ }

    func fetchAnd(val: UInt8): UInt8 { /* compiled code */ }

    func fetchAnd(val: UInt8, memoryOrder: std.sync.MemoryOrder): UInt8 { /* compiled code */ }

    func fetchOr(val: UInt8): UInt8 { /* compiled code */ }

    func fetchOr(val: UInt8, memoryOrder: std.sync.MemoryOrder): UInt8 { /* compiled code */ }

    func fetchXor(val: UInt8): UInt8 { /* compiled code */ }

    func fetchXor(val: UInt8, memoryOrder: std.sync.MemoryOrder): UInt8 { /* compiled code */ }
}

class AtomicUInt16 {
    var value: UInt16

    init(val: UInt16) { /* compiled code */ }

    func load(): UInt16 { /* compiled code */ }

    func load(memoryOrder: std.sync.MemoryOrder): UInt16 { /* compiled code */ }

    func store(val: UInt16): Unit { /* compiled code */ }

    func store(val: UInt16, memoryOrder: std.sync.MemoryOrder): Unit { /* compiled code */ }

    func swap(val: UInt16): UInt16 { /* compiled code */ }

    func swap(val: UInt16, memoryOrder: std.sync.MemoryOrder): UInt16 { /* compiled code */ }

    func compareAndSwap(old: UInt16, new: UInt16): Bool { /* compiled code */ }

    func compareAndSwap(old: UInt16, new: UInt16, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool { /* compiled code */ }

    func fetchAdd(val: UInt16): UInt16 { /* compiled code */ }

    func fetchAdd(val: UInt16, memoryOrder: std.sync.MemoryOrder): UInt16 { /* compiled code */ }

    func fetchSub(val: UInt16): UInt16 { /* compiled code */ }

    func fetchSub(val: UInt16, memoryOrder: std.sync.MemoryOrder): UInt16 { /* compiled code */ }

    func fetchAnd(val: UInt16): UInt16 { /* compiled code */ }

    func fetchAnd(val: UInt16, memoryOrder: std.sync.MemoryOrder): UInt16 { /* compiled code */ }

    func fetchOr(val: UInt16): UInt16 { /* compiled code */ }

    func fetchOr(val: UInt16, memoryOrder: std.sync.MemoryOrder): UInt16 { /* compiled code */ }

    func fetchXor(val: UInt16): UInt16 { /* compiled code */ }

    func fetchXor(val: UInt16, memoryOrder: std.sync.MemoryOrder): UInt16 { /* compiled code */ }
}

class AtomicUInt32 {
    var value: UInt32

    init(val: UInt32) { /* compiled code */ }

    func load(): UInt32 { /* compiled code */ }

    func load(memoryOrder: std.sync.MemoryOrder): UInt32 { /* compiled code */ }

    func store(val: UInt32): Unit { /* compiled code */ }

    func store(val: UInt32, memoryOrder: std.sync.MemoryOrder): Unit { /* compiled code */ }

    func swap(val: UInt32): UInt32 { /* compiled code */ }

    func swap(val: UInt32, memoryOrder: std.sync.MemoryOrder): UInt32 { /* compiled code */ }

    func compareAndSwap(old: UInt32, new: UInt32): Bool { /* compiled code */ }

    func compareAndSwap(old: UInt32, new: UInt32, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool { /* compiled code */ }

    func fetchAdd(val: UInt32): UInt32 { /* compiled code */ }

    func fetchAdd(val: UInt32, memoryOrder: std.sync.MemoryOrder): UInt32 { /* compiled code */ }

    func fetchSub(val: UInt32): UInt32 { /* compiled code */ }

    func fetchSub(val: UInt32, memoryOrder: std.sync.MemoryOrder): UInt32 { /* compiled code */ }

    func fetchAnd(val: UInt32): UInt32 { /* compiled code */ }

    func fetchAnd(val: UInt32, memoryOrder: std.sync.MemoryOrder): UInt32 { /* compiled code */ }

    func fetchOr(val: UInt32): UInt32 { /* compiled code */ }

    func fetchOr(val: UInt32, memoryOrder: std.sync.MemoryOrder): UInt32 { /* compiled code */ }

    func fetchXor(val: UInt32): UInt32 { /* compiled code */ }

    func fetchXor(val: UInt32, memoryOrder: std.sync.MemoryOrder): UInt32 { /* compiled code */ }
}

class AtomicUInt64 {
    var value: UInt64

    init(val: UInt64) { /* compiled code */ }

    func load(): UInt64 { /* compiled code */ }

    func load(memoryOrder: std.sync.MemoryOrder): UInt64 { /* compiled code */ }

    func store(val: UInt64): Unit { /* compiled code */ }

    func store(val: UInt64, memoryOrder: std.sync.MemoryOrder): Unit { /* compiled code */ }

    func swap(val: UInt64): UInt64 { /* compiled code */ }

    func swap(val: UInt64, memoryOrder: std.sync.MemoryOrder): UInt64 { /* compiled code */ }

    func compareAndSwap(old: UInt64, new: UInt64): Bool { /* compiled code */ }

    func compareAndSwap(old: UInt64, new: UInt64, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool { /* compiled code */ }

    func fetchAdd(val: UInt64): UInt64 { /* compiled code */ }

    func fetchAdd(val: UInt64, memoryOrder: std.sync.MemoryOrder): UInt64 { /* compiled code */ }

    func fetchSub(val: UInt64): UInt64 { /* compiled code */ }

    func fetchSub(val: UInt64, memoryOrder: std.sync.MemoryOrder): UInt64 { /* compiled code */ }

    func fetchAnd(val: UInt64): UInt64 { /* compiled code */ }

    func fetchAnd(val: UInt64, memoryOrder: std.sync.MemoryOrder): UInt64 { /* compiled code */ }

    func fetchOr(val: UInt64): UInt64 { /* compiled code */ }

    func fetchOr(val: UInt64, memoryOrder: std.sync.MemoryOrder): UInt64 { /* compiled code */ }

    func fetchXor(val: UInt64): UInt64 { /* compiled code */ }

    func fetchXor(val: UInt64, memoryOrder: std.sync.MemoryOrder): UInt64 { /* compiled code */ }
}

class AtomicBool {
    var value: Int8

    init(val: Bool) { /* compiled code */ }

    func load(): Bool { /* compiled code */ }

    func load(memoryOrder: std.sync.MemoryOrder): Bool { /* compiled code */ }

    func store(val: Bool): Unit { /* compiled code */ }

    func store(val: Bool, memoryOrder: std.sync.MemoryOrder): Unit { /* compiled code */ }

    func swap(val: Bool): Bool { /* compiled code */ }

    func swap(val: Bool, memoryOrder: std.sync.MemoryOrder): Bool { /* compiled code */ }

    func compareAndSwap(old: Bool, new: Bool): Bool { /* compiled code */ }

    func compareAndSwap(old: Bool, new: Bool, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool { /* compiled code */ }

    func transBoolToInt8(v: Bool): Int8 { /* compiled code */ }

    func transInt8ToBool(v: Int8): Bool { /* compiled code */ }
}

class AtomicReference {
    var value4cjnative: T

    init(val: T) { /* compiled code */ }

    func load(): T { /* compiled code */ }

    func load(memoryOrder: std.sync.MemoryOrder): T { /* compiled code */ }

    func store(val: T): Unit { /* compiled code */ }

    func store(val: T, memoryOrder: std.sync.MemoryOrder): Unit { /* compiled code */ }

    func swap(val: T): T { /* compiled code */ }

    func swap(val: T, memoryOrder: std.sync.MemoryOrder): T { /* compiled code */ }

    func compareAndSwap(old: T, new: T): Bool { /* compiled code */ }

    func compareAndSwap(old: T, new: T, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool { /* compiled code */ }
}

class AtomicOptionReference {
    var value: std.core.Option<T>

    init() { /* compiled code */ }

    init(val: std.core.Option<T>) { /* compiled code */ }

    func load(): std.core.Option<T> { /* compiled code */ }

    func load(memoryOrder: std.sync.MemoryOrder): std.core.Option<T> { /* compiled code */ }

    func store(val: std.core.Option<T>): Unit { /* compiled code */ }

    func store(val: std.core.Option<T>, memoryOrder: std.sync.MemoryOrder): Unit { /* compiled code */ }

    func swap(val: std.core.Option<T>): std.core.Option<T> { /* compiled code */ }

    func swap(val: std.core.Option<T>, memoryOrder: std.sync.MemoryOrder): std.core.Option<T> { /* compiled code */ }

    func compareAndSwap(old: std.core.Option<T>, new: std.core.Option<T>): Bool { /* compiled code */ }

    func compareAndSwap(old: std.core.Option<T>, new: std.core.Option<T>, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool { /* compiled code */ }
}

class Barrier {
    let syncCounter: std.sync.SyncCounter

    init(count: Int64) { /* compiled code */ }

    func wait(timeout: std.core.Duration): Unit { /* compiled code */ }
}

class SyncCounter {
    var countValue: std.sync.AtomicInt64

    let syncList: std.sync.SyncList

    init(count: Int64) { /* compiled code */ }

    prop count: Int64 {
        get() { /* compiled code */ }
    }

    func dec(): Unit { /* compiled code */ }

    func waitUntilZero(timeout: std.core.Duration): Unit { /* compiled code */ }
}

interface IReentrantMutex {
    func lock(): Unit { /* compiled code */ }

    func tryLock(): Bool { /* compiled code */ }

    func unlock(): Unit { /* compiled code */ }
}

class IllegalSynchronizationStateException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

interface Lock {
    func lock(): Unit { /* compiled code */ }

    func tryLock(): Bool { /* compiled code */ }

    func unlock(): Unit { /* compiled code */ }
}

interface UniqueLock <: std.sync.Lock {
    func condition(): std.sync.Condition { /* compiled code */ }
}

interface Condition {
    func wait(): Unit { /* compiled code */ }

    func wait(timeout: std.core.Duration): Bool { /* compiled code */ }

    func waitUntil(predicate: () -> Bool): Unit { /* compiled code */ }

    func waitUntil(predicate: () -> Bool, timeout: std.core.Duration): Bool { /* compiled code */ }

    func notify(): Unit { /* compiled code */ }

    func notifyAll(): Unit { /* compiled code */ }
}

class Monitor <: std.sync.ReentrantMutex {
    init() { /* compiled code */ }

    init(mutex: std.sync.Mutex) { /* compiled code */ }

    func wait(timeout: std.core.Duration): Bool { /* compiled code */ }

    func notify(): Unit { /* compiled code */ }

    func notifyAll(): Unit { /* compiled code */ }
}

class Mutex <: std.core.Object & std.sync.UniqueLock {
    init() { /* compiled code */ }

    func lock(): Unit { /* compiled code */ }

    func tryLock(): Bool { /* compiled code */ }

    func unlock(): Unit { /* compiled code */ }

    func condition(): std.sync.Condition { /* compiled code */ }

    func checkMutexStatus(): Unit { /* compiled code */ }
}

class ReentrantMutex <: std.core.Object & std.sync.Lock {
    let mutex: std.sync.Mutex

    init() { /* compiled code */ }

    init(mutex: std.sync.Mutex) { /* compiled code */ }

    func lock(): Unit { /* compiled code */ }

    func tryLock(): Bool { /* compiled code */ }

    func unlock(): Unit { /* compiled code */ }

    func checkMutexStatus(): Unit { /* compiled code */ }
}

struct ConditionID {
    init(m: std.sync.MultiConditionMonitor, wqPtr: std.sync.WaitQueue) { /* compiled code */ }

    let monitor: std.sync.MultiConditionMonitor

    let waitQueue: std.sync.WaitQueue
}

class MultiConditionMonitor <: std.sync.ReentrantMutex {
    init() { /* compiled code */ }

    func newCondition(): std.sync.ConditionID { /* compiled code */ }

    func wait(condID: std.sync.ConditionID, timeout: std.core.Duration): Bool { /* compiled code */ }

    func notify(condID: std.sync.ConditionID): Unit { /* compiled code */ }

    func notifyAll(condID: std.sync.ConditionID): Unit { /* compiled code */ }
}

class WaitQueue {
    init() { /* compiled code */ }
}

class MutexCondition <: std.core.Object & std.sync.Condition {
    let condition: std.sync.Monitor

    init(mutex: std.sync.Mutex) { /* compiled code */ }

    func wait(): Unit { /* compiled code */ }

    func wait(timeout: std.core.Duration): Bool { /* compiled code */ }

    func waitUntil(predicate: () -> Bool): Unit { /* compiled code */ }

    func waitUntil(predicate: () -> Bool, timeout: std.core.Duration): Bool { /* compiled code */ }

    func notify(): Unit { /* compiled code */ }

    func notifyAll(): Unit { /* compiled code */ }

    func getTimeoutNanos(timeout: std.core.Duration): Int64 { /* compiled code */ }
}

func mutexInit(mutex: std.sync.Mutex): Bool { /* compiled code */ }

func mutexLock(mutex: std.sync.Mutex): Unit { /* compiled code */ }

func mutexTryLock(mutex: std.sync.Mutex): Bool { /* compiled code */ }

func mutexCheckStatus(mutex: std.sync.Mutex): Bool { /* compiled code */ }

func mutexUnlock(mutex: std.sync.Mutex): Unit { /* compiled code */ }

func monitorWait(monitor: std.sync.Monitor, timeout: Int64): Bool { /* compiled code */ }

func monitorNotify(monitor: std.sync.Monitor): Unit { /* compiled code */ }

func monitorNotifyAll(monitor: std.sync.Monitor): Unit { /* compiled code */ }

func multiConditionMonitorWait(multiConditionMonitor: std.sync.MultiConditionMonitor, obj: std.core.Object, timeout: Int64): Bool { /* compiled code */ }

func multiConditionMonitorNotify(multiConditionMonitor: std.sync.MultiConditionMonitor, obj: std.core.Object): Unit { /* compiled code */ }

func multiConditionMonitorNotifyAll(multiConditionMonitor: std.sync.MultiConditionMonitor, obj: std.core.Object): Unit { /* compiled code */ }

func waitQueueInit(obj: std.sync.WaitQueue): Bool { /* compiled code */ }

func monitorInit(obj: std.sync.Monitor): Bool { /* compiled code */ }

class ReadWriteLock {
    let _fair: Bool

    let _readLock: std.sync.Lock

    let _writeLock: std.sync.UniqueLock

    prop readLock: std.sync.Lock {
        get() { /* compiled code */ }
    }

    prop writeLock: std.sync.UniqueLock {
        get() { /* compiled code */ }
    }

    init(fair: Bool) { /* compiled code */ }

    func isFair(): Bool { /* compiled code */ }
}

class ReadLock <: std.core.Object & std.sync.Lock {
    func lock(): Unit { /* compiled code */ }

    func tryLock(): Bool { /* compiled code */ }

    func unlock(): Unit { /* compiled code */ }

    let _readWriteLockImpl: std.sync.ReadWriteLockImpl

    init(_readWriteLockImpl: std.sync.ReadWriteLockImpl) { /* compiled code */ }
}

class ReadWriteLockImpl {
    let isFair: Bool

    let state: std.sync.AtomicUInt64

    init(isFair: Bool) { /* compiled code */ }

    func getWriteCount(): Int64 { /* compiled code */ }

    let writeOwner: std.sync.AtomicInt64

    let waitingWriters: std.sync.AtomicInt64

    let threadReadCount: std.core.ThreadLocal<Int64>

    let readerSyncList: std.sync.SyncList

    let writerSyncList: std.sync.SyncList

    func readLock(): Unit { /* compiled code */ }

    func tryReadLock(): Bool { /* compiled code */ }

    func readUnlock(): Unit { /* compiled code */ }

    func writeLock(writeCount: Int64): Unit { /* compiled code */ }

    func tryWriteLock(): Bool { /* compiled code */ }

    func checkWriteLockStatus(): Unit { /* compiled code */ }

    func writeUnlock(): Unit { /* compiled code */ }

    func writeUnlock(writeCount: Int64): Unit { /* compiled code */ }
}

class WriteLock <: std.core.Object & std.sync.UniqueLock {
    func lock(): Unit { /* compiled code */ }

    func tryLock(): Bool { /* compiled code */ }

    func unlock(): Unit { /* compiled code */ }

    func condition(): std.sync.Condition { /* compiled code */ }

    let _readWriteLockImpl: std.sync.ReadWriteLockImpl

    init(_readWriteLockImpl: std.sync.ReadWriteLockImpl) { /* compiled code */ }
}

class ReadWriteLockCondition <: std.core.Object & std.sync.Condition {
    let waitQueue: std.sync.SyncList

    func wait(): Unit { /* compiled code */ }

    func wait(timeout: std.core.Duration): Bool { /* compiled code */ }

    func waitUntil(predicate: () -> Bool): Unit { /* compiled code */ }

    func waitUntil(predicate: () -> Bool, timeout: std.core.Duration): Bool { /* compiled code */ }

    func notify(): Unit { /* compiled code */ }

    func notifyAll(): Unit { /* compiled code */ }

    let lock: std.sync.ReadWriteLockImpl

    init(lock: std.sync.ReadWriteLockImpl) { /* compiled code */ }
}

class SyncList {
    var head: std.core.Option<std.sync.SyncNode>

    let m: std.sync.Monitor

    func waitIf(pred: () -> Bool, timeout: std.core.Duration): Unit { /* compiled code */ }

    func waitInLock(unlock: () -> Unit, timeout: std.core.Duration): Bool { /* compiled code */ }

    func notifyOne(): Bool { /* compiled code */ }

    func notifyAll(): Bool { /* compiled code */ }

    init() { /* compiled code */ }
}

enum ReadWriteMutexMode {
    | Unfair

    | Fair
}

class ReentrantReadWriteMutex {
    let readMutex_: std.sync.ReentrantReadMutex

    let writeMutex_: std.sync.ReentrantWriteMutex

    init(mode: std.sync.ReadWriteMutexMode) { /* compiled code */ }

    prop readMutex: std.sync.ReentrantReadMutex {
        get() { /* compiled code */ }
    }

    prop writeMutex: std.sync.ReentrantWriteMutex {
        get() { /* compiled code */ }
    }
}

class ReentrantReadMutex <: std.sync.ReentrantMutex {
    let rwMutex: std.sync.ReadWriteLockImpl

    init(rwMutex: std.sync.ReadWriteLockImpl) { /* compiled code */ }

    func lock(): Unit { /* compiled code */ }

    func tryLock(): Bool { /* compiled code */ }

    func unlock(): Unit { /* compiled code */ }
}

class ReentrantWriteMutex <: std.sync.ReentrantMutex {
    let rwMutex: std.sync.ReadWriteLockImpl

    init(rwMutex: std.sync.ReadWriteLockImpl) { /* compiled code */ }

    func lock(): Unit { /* compiled code */ }

    func tryLock(): Bool { /* compiled code */ }

    func unlock(): Unit { /* compiled code */ }
}

class Semaphore {
    var countValue: std.sync.AtomicInt64

    let monitor: std.sync.Monitor

    let initValue: Int64

    init(count: Int64) { /* compiled code */ }

    prop count: Int64 {
        get() { /* compiled code */ }
    }

    func acquire(amount: Int64): Unit { /* compiled code */ }

    func tryAcquire(amount: Int64): Bool { /* compiled code */ }

    func release(amount: Int64): Unit { /* compiled code */ }
}

let MAX_TIMEOUT_DURATION: std.core.Duration

class NotifyOnce {
    let m: std.sync.Monitor

    var notified: Bool

    func notify(): Unit { /* compiled code */ }

    func wait(timeout: std.core.Duration): Bool { /* compiled code */ }

    init() { /* compiled code */ }
}

class SyncNode {
    let n: std.sync.NotifyOnce

    var next: std.core.Option<std.sync.SyncNode>

    init() { /* compiled code */ }
}

class Timer <: std.core.Object & std.core.Equatable<std.sync.Timer> & std.core.Hashable {
    let id: Int64

    let scheduler: std.sync.TimerScheduler

    let task: () -> std.core.Option<std.core.Duration>

    var endTime: std.time.MonoTime

    var isEnabled: Bool

    init(delay: std.core.Duration, task: () -> std.core.Option<std.core.Duration>) { /* compiled code */ }

    static func after(delay: std.core.Duration, task: () -> std.core.Option<std.core.Duration>): std.sync.Timer { /* compiled code */ }

    static func once(delay: std.core.Duration, task: () -> Unit): std.sync.Timer { /* compiled code */ }

    static func repeat(delay: std.core.Duration, interval: std.core.Duration, task: () -> Unit, style: std.sync.CatchupStyle): std.sync.Timer { /* compiled code */ }

    static func repeatDuring(period: std.core.Duration, delay: std.core.Duration, interval: std.core.Duration, task: () -> Unit, style: std.sync.CatchupStyle): std.sync.Timer { /* compiled code */ }

    static func repeatTimes(count: Int64, delay: std.core.Duration, interval: std.core.Duration, task: () -> Unit, style: std.sync.CatchupStyle): std.sync.Timer { /* compiled code */ }

    func cancel(): Unit { /* compiled code */ }

    operator func ==(rhs: std.sync.Timer): Bool { /* compiled code */ }

    operator func !=(rhs: std.sync.Timer): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func reload(duration: std.core.Duration): Unit { /* compiled code */ }
}

class TimerScheduler {
    static func getScheduler(id: Int64): std.sync.TimerScheduler { /* compiled code */ }

    let queue: std.sync.TimerQueue

    var heap: std.sync.TimerHeap

    let monitor: std.sync.Monitor

    let isRunning: std.sync.AtomicBool

    let cancelCount: std.sync.AtomicInt64

    func add(timer: std.sync.Timer): std.core.Any { /* compiled code */ }

    func addCancelCount(): Int64 { /* compiled code */ }

    init() { /* compiled code */ }
}

enum CatchupStyle {
    | Delay

    | Burst

    | Skip
}

func delayStyleTick(interval: std.core.Duration): () -> std.core.Option<std.core.Duration> { /* compiled code */ }

func burstStyleTick(delay: std.core.Duration, interval: std.core.Duration): () -> std.core.Option<std.core.Duration> { /* compiled code */ }

func skipStyleTick(delay: std.core.Duration, interval: std.core.Duration): () -> std.core.Option<std.core.Duration> { /* compiled code */ }

func tickDuring(delay: std.core.Duration, period: std.core.Duration, nextTick: () -> std.core.Option<std.core.Duration>): () -> std.core.Option<std.core.Duration> { /* compiled code */ }

func tickTimes(count: Int64, nextTick: () -> std.core.Option<std.core.Duration>): () -> std.core.Option<std.core.Duration> { /* compiled code */ }

class TimerQueue {
    let front: std.sync.AtomicReference<std.sync.TimerBlock>

    let rear: std.sync.AtomicReference<std.sync.TimerBlock>

    init() { /* compiled code */ }

    func add(element: std.sync.Timer): Unit { /* compiled code */ }

    func remove(): std.core.Option<std.sync.Timer> { /* compiled code */ }

    func peek(): std.core.Option<std.sync.Timer> { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }
}

class TimerHeap {
    let data: std.sync.TimerList

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    prop isEmpty: Bool {
        get() { /* compiled code */ }
    }

    func add(e: std.sync.Timer): Unit { /* compiled code */ }

    func top(): std.sync.Timer { /* compiled code */ }

    func poll(): std.sync.Timer { /* compiled code */ }

    init() { /* compiled code */ }
}

class TimerList {
    var _data: std.core.Array<std.sync.Timer>

    var _size: Int64

    init() { /* compiled code */ }

    func add(element: std.sync.Timer): Unit { /* compiled code */ }

    func remove(at: Int64): std.sync.Timer { /* compiled code */ }

    operator func [](index: Int64): std.sync.Timer { /* compiled code */ }

    operator func [](index: Int64, value: std.sync.Timer): Unit { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }
}

enum PushResult {
    | BlockDone

    | Conflict

    | Succeed
}

enum PopResult {
    | BlockDone

    | NoEntry

    | Element(std.sync.Timer)
}

class TimerBlock {
    let isExtended: std.sync.AtomicBool

    let next: std.sync.AtomicOptionReference<std.sync.TimerBlock>

    let allocated: std.sync.AtomicInt64

    let committed: std.sync.AtomicInt64

    let reserved: std.sync.AtomicInt64

    let buffer: std.core.Array<std.sync.Timer>

    func push(element: std.sync.Timer): Bool { /* compiled code */ }

    func pop(): std.sync.PopResult { /* compiled code */ }

    func front(): std.sync.PopResult { /* compiled code */ }

    let version: UInt64

    init(version: UInt64) { /* compiled code */ }
}

func newBlock(tailBlock: std.sync.TimerBlock, tail: std.sync.AtomicReference<std.sync.TimerBlock>): Unit { /* compiled code */ }

func nextBlock(headBlock: std.sync.TimerBlock, head: std.sync.AtomicReference<std.sync.TimerBlock>, tail: std.sync.AtomicReference<std.sync.TimerBlock>): Bool { /* compiled code */ }
