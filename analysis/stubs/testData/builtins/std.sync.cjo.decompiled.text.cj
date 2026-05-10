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

    func load: Int8

    func load(memoryOrder: std.sync.MemoryOrder): Int8

    func store(val: Int8): Unit

    func store(val: Int8, memoryOrder: std.sync.MemoryOrder): Unit

    func swap(val: Int8): Int8

    func swap(val: Int8, memoryOrder: std.sync.MemoryOrder): Int8

    func compareAndSwap(old: Int8, new: Int8): Bool

    func compareAndSwap(old: Int8, new: Int8, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool

    func fetchAdd(val: Int8): Int8

    func fetchAdd(val: Int8, memoryOrder: std.sync.MemoryOrder): Int8

    func fetchSub(val: Int8): Int8

    func fetchSub(val: Int8, memoryOrder: std.sync.MemoryOrder): Int8

    func fetchAnd(val: Int8): Int8

    func fetchAnd(val: Int8, memoryOrder: std.sync.MemoryOrder): Int8

    func fetchOr(val: Int8): Int8

    func fetchOr(val: Int8, memoryOrder: std.sync.MemoryOrder): Int8

    func fetchXor(val: Int8): Int8

    func fetchXor(val: Int8, memoryOrder: std.sync.MemoryOrder): Int8
}

enum MemoryOrder {
    | SeqCst
}

func load(atomicType: ATOMIC_TYPE, memoryOrder: std.sync.MemoryOrder): TYPE

let DefaultMemoryOrder: std.sync.MemoryOrder

func store(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): Unit

func swap(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE

func compareAndSwap(atomicType: ATOMIC_TYPE, oldValue: TYPE, newValue: TYPE, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool

func fetchAdd(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE

func fetchSub(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE

func fetchAnd(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE

func fetchOr(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE

func fetchXor(atomicType: ATOMIC_TYPE, value: TYPE, memoryOrder: std.sync.MemoryOrder): TYPE

class AtomicInt16 {
    var value: Int16

    init(val: Int16) { /* compiled code */ }

    func load: Int16

    func load(memoryOrder: std.sync.MemoryOrder): Int16

    func store(val: Int16): Unit

    func store(val: Int16, memoryOrder: std.sync.MemoryOrder): Unit

    func swap(val: Int16): Int16

    func swap(val: Int16, memoryOrder: std.sync.MemoryOrder): Int16

    func compareAndSwap(old: Int16, new: Int16): Bool

    func compareAndSwap(old: Int16, new: Int16, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool

    func fetchAdd(val: Int16): Int16

    func fetchAdd(val: Int16, memoryOrder: std.sync.MemoryOrder): Int16

    func fetchSub(val: Int16): Int16

    func fetchSub(val: Int16, memoryOrder: std.sync.MemoryOrder): Int16

    func fetchAnd(val: Int16): Int16

    func fetchAnd(val: Int16, memoryOrder: std.sync.MemoryOrder): Int16

    func fetchOr(val: Int16): Int16

    func fetchOr(val: Int16, memoryOrder: std.sync.MemoryOrder): Int16

    func fetchXor(val: Int16): Int16

    func fetchXor(val: Int16, memoryOrder: std.sync.MemoryOrder): Int16
}

class AtomicInt32 {
    var value: Int32

    init(val: Int32) { /* compiled code */ }

    func load: Int32

    func load(memoryOrder: std.sync.MemoryOrder): Int32

    func store(val: Int32): Unit

    func store(val: Int32, memoryOrder: std.sync.MemoryOrder): Unit

    func swap(val: Int32): Int32

    func swap(val: Int32, memoryOrder: std.sync.MemoryOrder): Int32

    func compareAndSwap(old: Int32, new: Int32): Bool

    func compareAndSwap(old: Int32, new: Int32, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool

    func fetchAdd(val: Int32): Int32

    func fetchAdd(val: Int32, memoryOrder: std.sync.MemoryOrder): Int32

    func fetchSub(val: Int32): Int32

    func fetchSub(val: Int32, memoryOrder: std.sync.MemoryOrder): Int32

    func fetchAnd(val: Int32): Int32

    func fetchAnd(val: Int32, memoryOrder: std.sync.MemoryOrder): Int32

    func fetchOr(val: Int32): Int32

    func fetchOr(val: Int32, memoryOrder: std.sync.MemoryOrder): Int32

    func fetchXor(val: Int32): Int32

    func fetchXor(val: Int32, memoryOrder: std.sync.MemoryOrder): Int32
}

class AtomicInt64 {
    var value: Int64

    init(val: Int64) { /* compiled code */ }

    func load: Int64

    func load(memoryOrder: std.sync.MemoryOrder): Int64

    func store(val: Int64): Unit

    func store(val: Int64, memoryOrder: std.sync.MemoryOrder): Unit

    func swap(val: Int64): Int64

    func swap(val: Int64, memoryOrder: std.sync.MemoryOrder): Int64

    func compareAndSwap(old: Int64, new: Int64): Bool

    func compareAndSwap(old: Int64, new: Int64, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool

    func fetchAdd(val: Int64): Int64

    func fetchAdd(val: Int64, memoryOrder: std.sync.MemoryOrder): Int64

    func fetchSub(val: Int64): Int64

    func fetchSub(val: Int64, memoryOrder: std.sync.MemoryOrder): Int64

    func fetchAnd(val: Int64): Int64

    func fetchAnd(val: Int64, memoryOrder: std.sync.MemoryOrder): Int64

    func fetchOr(val: Int64): Int64

    func fetchOr(val: Int64, memoryOrder: std.sync.MemoryOrder): Int64

    func fetchXor(val: Int64): Int64

    func fetchXor(val: Int64, memoryOrder: std.sync.MemoryOrder): Int64
}

class AtomicUInt8 {
    var value: UInt8

    init(val: UInt8) { /* compiled code */ }

    func load: UInt8

    func load(memoryOrder: std.sync.MemoryOrder): UInt8

    func store(val: UInt8): Unit

    func store(val: UInt8, memoryOrder: std.sync.MemoryOrder): Unit

    func swap(val: UInt8): UInt8

    func swap(val: UInt8, memoryOrder: std.sync.MemoryOrder): UInt8

    func compareAndSwap(old: UInt8, new: UInt8): Bool

    func compareAndSwap(old: UInt8, new: UInt8, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool

    func fetchAdd(val: UInt8): UInt8

    func fetchAdd(val: UInt8, memoryOrder: std.sync.MemoryOrder): UInt8

    func fetchSub(val: UInt8): UInt8

    func fetchSub(val: UInt8, memoryOrder: std.sync.MemoryOrder): UInt8

    func fetchAnd(val: UInt8): UInt8

    func fetchAnd(val: UInt8, memoryOrder: std.sync.MemoryOrder): UInt8

    func fetchOr(val: UInt8): UInt8

    func fetchOr(val: UInt8, memoryOrder: std.sync.MemoryOrder): UInt8

    func fetchXor(val: UInt8): UInt8

    func fetchXor(val: UInt8, memoryOrder: std.sync.MemoryOrder): UInt8
}

class AtomicUInt16 {
    var value: UInt16

    init(val: UInt16) { /* compiled code */ }

    func load: UInt16

    func load(memoryOrder: std.sync.MemoryOrder): UInt16

    func store(val: UInt16): Unit

    func store(val: UInt16, memoryOrder: std.sync.MemoryOrder): Unit

    func swap(val: UInt16): UInt16

    func swap(val: UInt16, memoryOrder: std.sync.MemoryOrder): UInt16

    func compareAndSwap(old: UInt16, new: UInt16): Bool

    func compareAndSwap(old: UInt16, new: UInt16, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool

    func fetchAdd(val: UInt16): UInt16

    func fetchAdd(val: UInt16, memoryOrder: std.sync.MemoryOrder): UInt16

    func fetchSub(val: UInt16): UInt16

    func fetchSub(val: UInt16, memoryOrder: std.sync.MemoryOrder): UInt16

    func fetchAnd(val: UInt16): UInt16

    func fetchAnd(val: UInt16, memoryOrder: std.sync.MemoryOrder): UInt16

    func fetchOr(val: UInt16): UInt16

    func fetchOr(val: UInt16, memoryOrder: std.sync.MemoryOrder): UInt16

    func fetchXor(val: UInt16): UInt16

    func fetchXor(val: UInt16, memoryOrder: std.sync.MemoryOrder): UInt16
}

class AtomicUInt32 {
    var value: UInt32

    init(val: UInt32) { /* compiled code */ }

    func load: UInt32

    func load(memoryOrder: std.sync.MemoryOrder): UInt32

    func store(val: UInt32): Unit

    func store(val: UInt32, memoryOrder: std.sync.MemoryOrder): Unit

    func swap(val: UInt32): UInt32

    func swap(val: UInt32, memoryOrder: std.sync.MemoryOrder): UInt32

    func compareAndSwap(old: UInt32, new: UInt32): Bool

    func compareAndSwap(old: UInt32, new: UInt32, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool

    func fetchAdd(val: UInt32): UInt32

    func fetchAdd(val: UInt32, memoryOrder: std.sync.MemoryOrder): UInt32

    func fetchSub(val: UInt32): UInt32

    func fetchSub(val: UInt32, memoryOrder: std.sync.MemoryOrder): UInt32

    func fetchAnd(val: UInt32): UInt32

    func fetchAnd(val: UInt32, memoryOrder: std.sync.MemoryOrder): UInt32

    func fetchOr(val: UInt32): UInt32

    func fetchOr(val: UInt32, memoryOrder: std.sync.MemoryOrder): UInt32

    func fetchXor(val: UInt32): UInt32

    func fetchXor(val: UInt32, memoryOrder: std.sync.MemoryOrder): UInt32
}

class AtomicUInt64 {
    var value: UInt64

    init(val: UInt64) { /* compiled code */ }

    func load: UInt64

    func load(memoryOrder: std.sync.MemoryOrder): UInt64

    func store(val: UInt64): Unit

    func store(val: UInt64, memoryOrder: std.sync.MemoryOrder): Unit

    func swap(val: UInt64): UInt64

    func swap(val: UInt64, memoryOrder: std.sync.MemoryOrder): UInt64

    func compareAndSwap(old: UInt64, new: UInt64): Bool

    func compareAndSwap(old: UInt64, new: UInt64, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool

    func fetchAdd(val: UInt64): UInt64

    func fetchAdd(val: UInt64, memoryOrder: std.sync.MemoryOrder): UInt64

    func fetchSub(val: UInt64): UInt64

    func fetchSub(val: UInt64, memoryOrder: std.sync.MemoryOrder): UInt64

    func fetchAnd(val: UInt64): UInt64

    func fetchAnd(val: UInt64, memoryOrder: std.sync.MemoryOrder): UInt64

    func fetchOr(val: UInt64): UInt64

    func fetchOr(val: UInt64, memoryOrder: std.sync.MemoryOrder): UInt64

    func fetchXor(val: UInt64): UInt64

    func fetchXor(val: UInt64, memoryOrder: std.sync.MemoryOrder): UInt64
}

class AtomicBool {
    var value: Int8

    init(val: Bool) { /* compiled code */ }

    func load: Bool

    func load(memoryOrder: std.sync.MemoryOrder): Bool

    func store(val: Bool): Unit

    func store(val: Bool, memoryOrder: std.sync.MemoryOrder): Unit

    func swap(val: Bool): Bool

    func swap(val: Bool, memoryOrder: std.sync.MemoryOrder): Bool

    func compareAndSwap(old: Bool, new: Bool): Bool

    func compareAndSwap(old: Bool, new: Bool, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool

    func transBoolToInt8(v: Bool): Int8

    func transInt8ToBool(v: Int8): Bool
}

class AtomicReference {
    var value4cjnative: T

    init(val: T) { /* compiled code */ }

    func load: T

    func load(memoryOrder: std.sync.MemoryOrder): T

    func store(val: T): Unit

    func store(val: T, memoryOrder: std.sync.MemoryOrder): Unit

    func swap(val: T): T

    func swap(val: T, memoryOrder: std.sync.MemoryOrder): T

    func compareAndSwap(old: T, new: T): Bool

    func compareAndSwap(old: T, new: T, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool
}

class AtomicOptionReference {
    var value: std.core.Option<T>

    init() { /* compiled code */ }

    init(val: std.core.Option<T>) { /* compiled code */ }

    func load: std.core.Option<T>

    func load(memoryOrder: std.sync.MemoryOrder): std.core.Option<T>

    func store(val: std.core.Option<T>): Unit

    func store(val: std.core.Option<T>, memoryOrder: std.sync.MemoryOrder): Unit

    func swap(val: std.core.Option<T>): std.core.Option<T>

    func swap(val: std.core.Option<T>, memoryOrder: std.sync.MemoryOrder): std.core.Option<T>

    func compareAndSwap(old: std.core.Option<T>, new: std.core.Option<T>): Bool

    func compareAndSwap(old: std.core.Option<T>, new: std.core.Option<T>, successOrder: std.sync.MemoryOrder, failureOrder: std.sync.MemoryOrder): Bool
}

class Barrier {
    let syncCounter: std.sync.SyncCounter

    init(count: Int64) { /* compiled code */ }

    func wait(timeout: std.core.Duration): Unit
}

class SyncCounter {
    var countValue: std.sync.AtomicInt64

    let syncList: std.sync.SyncList

    init(count: Int64) { /* compiled code */ }

    prop count: Int64

    func dec: Unit

    func waitUntilZero(timeout: std.core.Duration): Unit
}

interface IReentrantMutex {
    func lock: Unit

    func tryLock: Bool

    func unlock: Unit
}

class IllegalSynchronizationStateException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

interface Lock {
    func lock: Unit

    func tryLock: Bool

    func unlock: Unit
}

interface UniqueLock <: std.sync.Lock {
    func condition: std.sync.Condition
}

interface Condition {
    func wait: Unit

    func wait(timeout: std.core.Duration): Bool

    func waitUntil(predicate: () -> Bool): Unit

    func waitUntil(predicate: () -> Bool, timeout: std.core.Duration): Bool

    func notify: Unit

    func notifyAll: Unit
}

class Monitor <: std.sync.ReentrantMutex {
    init() { /* compiled code */ }

    init(mutex: std.sync.Mutex) { /* compiled code */ }

    func wait(timeout: std.core.Duration): Bool

    func notify: Unit

    func notifyAll: Unit
}

class Mutex <: std.core.Object & std.sync.UniqueLock {
    init() { /* compiled code */ }

    func lock: Unit

    func tryLock: Bool

    func unlock: Unit

    func condition: std.sync.Condition

    func checkMutexStatus: Unit
}

class ReentrantMutex <: std.core.Object & std.sync.Lock {
    let mutex: std.sync.Mutex

    init() { /* compiled code */ }

    init(mutex: std.sync.Mutex) { /* compiled code */ }

    func lock: Unit

    func tryLock: Bool

    func unlock: Unit

    func checkMutexStatus: Unit
}

struct ConditionID {
    init(m: std.sync.MultiConditionMonitor, wqPtr: std.sync.WaitQueue) { /* compiled code */ }

    let monitor: std.sync.MultiConditionMonitor

    let waitQueue: std.sync.WaitQueue
}

class MultiConditionMonitor <: std.sync.ReentrantMutex {
    init() { /* compiled code */ }

    func newCondition: std.sync.ConditionID

    func wait(condID: std.sync.ConditionID, timeout: std.core.Duration): Bool

    func notify(condID: std.sync.ConditionID): Unit

    func notifyAll(condID: std.sync.ConditionID): Unit
}

class WaitQueue {
    init() { /* compiled code */ }
}

class MutexCondition <: std.core.Object & std.sync.Condition {
    let condition: std.sync.Monitor

    init(mutex: std.sync.Mutex) { /* compiled code */ }

    func wait: Unit

    func wait(timeout: std.core.Duration): Bool

    func waitUntil(predicate: () -> Bool): Unit

    func waitUntil(predicate: () -> Bool, timeout: std.core.Duration): Bool

    func notify: Unit

    func notifyAll: Unit

    func getTimeoutNanos(timeout: std.core.Duration): Int64
}

func mutexInit(mutex: std.sync.Mutex): Bool

func mutexLock(mutex: std.sync.Mutex): Unit

func mutexTryLock(mutex: std.sync.Mutex): Bool

func mutexCheckStatus(mutex: std.sync.Mutex): Bool

func mutexUnlock(mutex: std.sync.Mutex): Unit

func monitorWait(monitor: std.sync.Monitor, timeout: Int64): Bool

func monitorNotify(monitor: std.sync.Monitor): Unit

func monitorNotifyAll(monitor: std.sync.Monitor): Unit

func multiConditionMonitorWait(multiConditionMonitor: std.sync.MultiConditionMonitor, obj: std.core.Object, timeout: Int64): Bool

func multiConditionMonitorNotify(multiConditionMonitor: std.sync.MultiConditionMonitor, obj: std.core.Object): Unit

func multiConditionMonitorNotifyAll(multiConditionMonitor: std.sync.MultiConditionMonitor, obj: std.core.Object): Unit

func waitQueueInit(obj: std.sync.WaitQueue): Bool

func monitorInit(obj: std.sync.Monitor): Bool

class ReadWriteLock {
    let _fair: Bool

    let _readLock: std.sync.Lock

    let _writeLock: std.sync.UniqueLock

    prop readLock: std.sync.Lock

    prop writeLock: std.sync.UniqueLock

    init(fair: Bool) { /* compiled code */ }

    func isFair: Bool
}

class ReadLock <: std.core.Object & std.sync.Lock {
    func lock: Unit

    func tryLock: Bool

    func unlock: Unit

    let _readWriteLockImpl: std.sync.ReadWriteLockImpl

    init(_readWriteLockImpl: std.sync.ReadWriteLockImpl) { /* compiled code */ }
}

class ReadWriteLockImpl {
    let isFair: Bool

    let state: std.sync.AtomicUInt64

    init(isFair: Bool) { /* compiled code */ }

    func getWriteCount: Int64

    let writeOwner: std.sync.AtomicInt64

    let waitingWriters: std.sync.AtomicInt64

    let threadReadCount: std.core.ThreadLocal<Int64>

    let readerSyncList: std.sync.SyncList

    let writerSyncList: std.sync.SyncList

    func readLock: Unit

    func tryReadLock: Bool

    func readUnlock: Unit

    func writeLock(writeCount: Int64): Unit

    func tryWriteLock: Bool

    func checkWriteLockStatus: Unit

    func writeUnlock: Unit

    func writeUnlock(writeCount: Int64): Unit
}

class WriteLock <: std.core.Object & std.sync.UniqueLock {
    func lock: Unit

    func tryLock: Bool

    func unlock: Unit

    func condition: std.sync.Condition

    let _readWriteLockImpl: std.sync.ReadWriteLockImpl

    init(_readWriteLockImpl: std.sync.ReadWriteLockImpl) { /* compiled code */ }
}

class ReadWriteLockCondition <: std.core.Object & std.sync.Condition {
    let waitQueue: std.sync.SyncList

    func wait: Unit

    func wait(timeout: std.core.Duration): Bool

    func waitUntil(predicate: () -> Bool): Unit

    func waitUntil(predicate: () -> Bool, timeout: std.core.Duration): Bool

    func notify: Unit

    func notifyAll: Unit

    let lock: std.sync.ReadWriteLockImpl

    init(lock: std.sync.ReadWriteLockImpl) { /* compiled code */ }
}

class SyncList {
    var head: std.core.Option<std.sync.SyncNode>

    let m: std.sync.Monitor

    func waitIf(pred: () -> Bool, timeout: std.core.Duration): Unit

    func waitInLock(unlock: () -> Unit, timeout: std.core.Duration): Bool

    func notifyOne: Bool

    func notifyAll: Bool

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

    prop readMutex: std.sync.ReentrantReadMutex

    prop writeMutex: std.sync.ReentrantWriteMutex
}

class ReentrantReadMutex <: std.sync.ReentrantMutex {
    let rwMutex: std.sync.ReadWriteLockImpl

    init(rwMutex: std.sync.ReadWriteLockImpl) { /* compiled code */ }

    func lock: Unit

    func tryLock: Bool

    func unlock: Unit
}

class ReentrantWriteMutex <: std.sync.ReentrantMutex {
    let rwMutex: std.sync.ReadWriteLockImpl

    init(rwMutex: std.sync.ReadWriteLockImpl) { /* compiled code */ }

    func lock: Unit

    func tryLock: Bool

    func unlock: Unit
}

class Semaphore {
    var countValue: std.sync.AtomicInt64

    let monitor: std.sync.Monitor

    let initValue: Int64

    init(count: Int64) { /* compiled code */ }

    prop count: Int64

    func acquire(amount: Int64): Unit

    func tryAcquire(amount: Int64): Bool

    func release(amount: Int64): Unit
}

let MAX_TIMEOUT_DURATION: std.core.Duration

class NotifyOnce {
    let m: std.sync.Monitor

    var notified: Bool

    func notify: Unit

    func wait(timeout: std.core.Duration): Bool

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

    func after(delay: std.core.Duration, task: () -> std.core.Option<std.core.Duration>): std.sync.Timer

    func once(delay: std.core.Duration, task: () -> Unit): std.sync.Timer

    func repeat(delay: std.core.Duration, interval: std.core.Duration, task: () -> Unit, style: std.sync.CatchupStyle): std.sync.Timer

    func repeatDuring(period: std.core.Duration, delay: std.core.Duration, interval: std.core.Duration, task: () -> Unit, style: std.sync.CatchupStyle): std.sync.Timer

    func repeatTimes(count: Int64, delay: std.core.Duration, interval: std.core.Duration, task: () -> Unit, style: std.sync.CatchupStyle): std.sync.Timer

    func cancel: Unit

    operator func ==(rhs: std.sync.Timer): Bool

    operator func !=(rhs: std.sync.Timer): Bool

    func hashCode: Int64

    func reload(duration: std.core.Duration): Unit
}

class TimerScheduler {
    func getScheduler(id: Int64): std.sync.TimerScheduler

    let queue: std.sync.TimerQueue

    var heap: std.sync.TimerHeap

    let monitor: std.sync.Monitor

    let isRunning: std.sync.AtomicBool

    let cancelCount: std.sync.AtomicInt64

    func add(timer: std.sync.Timer): std.core.Any

    func addCancelCount: Int64

    init() { /* compiled code */ }
}

enum CatchupStyle {
    | Delay

    | Burst

    | Skip
}

func delayStyleTick(interval: std.core.Duration): () -> std.core.Option<std.core.Duration>

func burstStyleTick(delay: std.core.Duration, interval: std.core.Duration): () -> std.core.Option<std.core.Duration>

func skipStyleTick(delay: std.core.Duration, interval: std.core.Duration): () -> std.core.Option<std.core.Duration>

func tickDuring(delay: std.core.Duration, period: std.core.Duration, nextTick: () -> std.core.Option<std.core.Duration>): () -> std.core.Option<std.core.Duration>

func tickTimes(count: Int64, nextTick: () -> std.core.Option<std.core.Duration>): () -> std.core.Option<std.core.Duration>

class TimerQueue {
    let front: std.sync.AtomicReference<std.sync.TimerBlock>

    let rear: std.sync.AtomicReference<std.sync.TimerBlock>

    init() { /* compiled code */ }

    func add(element: std.sync.Timer): Unit

    func remove: std.core.Option<std.sync.Timer>

    func peek: std.core.Option<std.sync.Timer>

    func isEmpty: Bool
}

class TimerHeap {
    let data: std.sync.TimerList

    prop size: Int64

    prop isEmpty: Bool

    func add(e: std.sync.Timer): Unit

    func top: std.sync.Timer

    func poll: std.sync.Timer

    init() { /* compiled code */ }
}

class TimerList {
    var _data: std.core.Array<std.sync.Timer>

    var _size: Int64

    init() { /* compiled code */ }

    func add(element: std.sync.Timer): Unit

    func remove(at: Int64): std.sync.Timer

    operator func [](index: Int64): std.sync.Timer

    operator func [](index: Int64, value: std.sync.Timer): Unit

    func isEmpty: Bool

    prop size: Int64
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

    func push(element: std.sync.Timer): Bool

    func pop: std.sync.PopResult

    func front: std.sync.PopResult

    let version: UInt64

    init(version: UInt64) { /* compiled code */ }
}

func newBlock(tailBlock: std.sync.TimerBlock, tail: std.sync.AtomicReference<std.sync.TimerBlock>): Unit

func nextBlock(headBlock: std.sync.TimerBlock, head: std.sync.AtomicReference<std.sync.TimerBlock>, tail: std.sync.AtomicReference<std.sync.TimerBlock>): Bool
