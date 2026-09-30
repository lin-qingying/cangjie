// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.collection.concurrent

import std.sync.*
import std.time.*
import std.core.*
import std.collection.MapEntryView
import std.collection.ArrayList

@Annotation
let BUF_SIZE_BITS: Int64

@Annotation
let BUF_SIZE: Int64

class KVList<K, V> {
    var myData: std.core.Array<(Int64, K, V)>

    var mySize: Int64

    @Frozen
    func find(hash: Int64, key: K): std.core.Option<V> { /* compiled code */ }

    @Frozen
    operator func [](index: Int64): (Int64, K, V) { /* compiled code */ }
}

@Annotation
enum FindKey<V> {
    | FIND(V)

    | NotFind

    | NotMigrate
}

struct Bucket<K, V> {
    @Annotation
    let atomic_ref_entries: std.sync.AtomicOptionReference<std.collection.concurrent.KVList<K, V>>

    @Frozen
    func find(hash: Int64, key: K): std.collection.concurrent.FindKey<V> { /* compiled code */ }

    func copyAddIfAbsent(hash: Int64, key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func refList(): std.core.Option<std.collection.concurrent.KVList<K, V>> { /* compiled code */ }

    init() { /* compiled code */ }
}

class HNode<K, V> {
    let buckets: std.core.Array<std.collection.concurrent.Bucket<K, V>>

    var freezeFlags: std.core.Array<Bool>

    let lockG: std.collection.concurrent.MutexLockGroup

    let version: Int64

    @Annotation
    let migratingIndex: std.sync.AtomicInt64

    @Annotation
    let migratedIndex: std.sync.AtomicInt64

    let pre: std.sync.AtomicOptionReference<std.collection.concurrent.HNode<K, V>>

    @Frozen
    func getValue(hash: Int64, key: K): std.core.Option<V> { /* compiled code */ }

    func tryGetBucket(index: Int64): Bool { /* compiled code */ }
}

class MutexLockGroup {
    let mutexes: std.core.Array<std.sync.Mutex>

    let mod: Int64

    func unlock(index: Int64): Unit { /* compiled code */ }
}

class ConcurrentHashMapIterator<K, V> <: std.core.Iterator<(K, V)> {
    let curHT: std.collection.concurrent.HNode<K, V>

    @Annotation
    var curBucketIndex: Int64

    @Annotation
    var curEntries: std.core.Option<std.collection.concurrent.KVList<K, V>>

    @Annotation
    var entriesIndex: Int64

    init(cmap: std.collection.concurrent.ConcurrentHashMap<K, V>) { /* compiled code */ }

    @Frozen
    func next(): std.core.Option<(K, V)> { /* compiled code */ }

    func nextElement(): Unit { /* compiled code */ }
}

class ConcurrentHashMap<K, V> <: std.core.Object & std.collection.concurrent.ConcurrentMap<K, V> & std.core.Collection<(K, V)> {
    @Annotation
    let counts: std.core.Array<std.sync.AtomicInt64>

    @Annotation
    const COUNT_SIZE: Int64

    @Annotation
    let curHNodeVersion: std.sync.AtomicInt64

    let head: std.sync.AtomicReference<std.collection.concurrent.HNode<K, V>>

    let lockG: std.core.Array<std.collection.concurrent.MutexLockGroup>

    let concurrencyLevel: Int64

    init(concurrencyLevel: Int64) { /* compiled code */ }

    init(capacity: Int64, concurrencyLevel: Int64) { /* compiled code */ }

    init(elements: std.core.Collection<(K, V)>, concurrencyLevel: Int64) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> (K, V), concurrencyLevel: Int64) { /* compiled code */ }

    @Frozen
    func get(key: K): std.core.Option<V> { /* compiled code */ }

    @Frozen
    func contains(key: K): Bool { /* compiled code */ }

    @Deprecated[message: "Use member function `public func add(key: K, value: V): ?V` instead."]
    func put(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    @Frozen
    func add(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func addIfAbsent(key: K, value: V): ?V` instead."]
    func putIfAbsent(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    @Frozen
    func addIfAbsent(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func remove(key: K): std.core.Option<V> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func entryView(K, (MapEntryView<K, V>) -> Unit)` instead."]
    func remove(key: K, predicate: (V) -> Bool): std.core.Option<V> { /* compiled code */ }

    @Frozen
    func replace(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func entryView(key: K, fn: (MapEntryView<K, V>)->Unit): ?V` instead."]
    func replace(key: K, eval: (V) -> V): std.core.Option<V> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func entryView(key: K, fn: (MapEntryView<K, V>)->Unit): ?V` instead."]
    func replace(key: K, predicate: (V) -> Bool, eval: (V) -> V): std.core.Option<V> { /* compiled code */ }

    func entryView(key: K, fn: (std.collection.MapEntryView<K, V>) -> Unit): std.core.Option<V> { /* compiled code */ }

    operator func [](key: K): V { /* compiled code */ }

    operator func [](key: K, value: V): Unit { /* compiled code */ }

    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    func iterator(): std.collection.concurrent.ConcurrentHashMapIterator<K, V> { /* compiled code */ }

    func addInternal(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func replaceInternel(key: K, predicate: (V) -> Bool, eval: (V) -> V): std.core.Option<V> { /* compiled code */ }

    func tryResize(hash: Int64): Unit { /* compiled code */ }

    func countInc(ret: std.core.Option<V>, hash: Int64): Unit { /* compiled code */ }
}

class Block<E> {
    let version: UInt64

    @Annotation
    let isExtended: std.sync.AtomicBool

    @Annotation
    let next: std.sync.AtomicOptionReference<std.collection.concurrent.Block<E>>

    @Annotation
    let allocated: std.sync.AtomicInt64

    @Annotation
    let committed: std.sync.AtomicInt64

    @Annotation
    let reserved: std.sync.AtomicInt64

    @Annotation
    let buffer: std.core.Array<E>

    func getAllocated(): (UInt64, Int64) { /* compiled code */ }

    func getReserved(): (UInt64, Int64) { /* compiled code */ }
}

class ConcurrentLinkedQueueIterator<E> <: std.core.Iterator<E> {
    let queue: std.collection.concurrent.ConcurrentLinkedQueue<E>

    var curBlock: std.collection.concurrent.Block<E>

    var curIndex: Int64

    @Annotation
    var hasNext: Bool

    init(queue: std.collection.concurrent.ConcurrentLinkedQueue<E>, front: std.collection.concurrent.Block<E>) { /* compiled code */ }

    @Frozen
    func next(): std.core.Option<E> { /* compiled code */ }

    func getNextBlock(): Bool { /* compiled code */ }
}

class ConcurrentLinkedQueue<E> <: std.core.Object & std.core.Collection<E> {
    let front: std.sync.AtomicReference<std.collection.concurrent.Block<E>>

    let rear: std.sync.AtomicReference<std.collection.concurrent.Block<E>>

    init() { /* compiled code */ }

    @Deprecated[message: "Use the other version of `init` instead."]
    init(elements: std.core.Collection<E>) { /* compiled code */ }

    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use member function `func addIfAbsent(key: K, value: V): ?V` instead."]
    func enqueue(element: E): Bool { /* compiled code */ }

    func add(element: E): Bool { /* compiled code */ }

    @Deprecated[message: "Use member function `public func remove(): Option<E>` instead."]
    func dequeue(): std.core.Option<E> { /* compiled code */ }

    func remove(): std.core.Option<E> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func peek(): Option<E>` instead."]
    func head(): std.core.Option<E> { /* compiled code */ }

    func peek(): std.core.Option<E> { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    func toArray(): std.core.Array<E> { /* compiled code */ }

    @Frozen
    func iterator(): std.core.Iterator<E> { /* compiled code */ }
}

class ArrayBlockingQueue<E> {
    let capacity: Int64

    let buffer: std.core.Array<E>

    let state: CPointer<UInt8>

    @Annotation
    let allocated: std.sync.AtomicUInt64

    @Annotation
    let reserved: std.sync.AtomicUInt64

    @Annotation
    let sendQSize: std.sync.AtomicUInt64

    @Annotation
    let receiveQSize: std.sync.AtomicUInt64

    @Annotation
    let sendMonitor: std.sync.Monitor

    @Annotation
    let receiveMonitor: std.sync.Monitor

    let unsignedCapacity: UInt64

    init(capacity: Int64) { /* compiled code */ }

    @Deprecated[message: "Use the other version of `init` instead."]
    init(capacity: Int64, elements: std.core.Collection<E>) { /* compiled code */ }

    ~init() { /* compiled code */ }

    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use member function `public func add(element: E): Unit` instead."]
    func enqueue(element: E): Unit { /* compiled code */ }

    func add(element: E): Unit { /* compiled code */ }

    @Deprecated[message: "Use member function `public func add(element: E, timeout: Duration): Bool` instead."]
    func enqueue(element: E, timeout: std.core.Duration): Bool { /* compiled code */ }

    func add(element: E, timeout: std.core.Duration): Bool { /* compiled code */ }

    @Deprecated[message: "Use member function `public func remove(): E` instead."]
    func dequeue(): E { /* compiled code */ }

    func remove(): E { /* compiled code */ }

    @Deprecated[message: "Use member function `public func remove(timeout: Duration): Option<E>` instead."]
    func dequeue(timeout: std.core.Duration): std.core.Option<E> { /* compiled code */ }

    func remove(timeout: std.core.Duration): std.core.Option<E> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func peek(): Option<E>` instead."]
    func head(): std.core.Option<E> { /* compiled code */ }

    func peek(): std.core.Option<E> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func tryAdd(element: E): Bool` instead."]
    func tryEnqueue(element: E): Bool { /* compiled code */ }

    func tryAdd(element: E): Bool { /* compiled code */ }

    @Deprecated[message: "Use member function `public func tryRemove(): Option<E>` instead."]
    func tryDequeue(): std.core.Option<E> { /* compiled code */ }

    func tryRemove(): std.core.Option<E> { /* compiled code */ }
}

@Deprecated[message: "Use LinkedBlockingQueue instead."]
type BlockingQueue<E> = std.collection.concurrent.LinkedBlockingQueue<E>

class LinkedBlockingQueue<E> {
    let capacity: Int64

    @Annotation
    let sendMonitor: std.sync.Monitor

    @Annotation
    let receiveMonitor: std.sync.Monitor

    @Annotation
    let sendQSize: std.sync.AtomicInt64

    @Annotation
    let receiveQSize: std.sync.AtomicInt64

    let front: std.sync.AtomicReference<std.collection.concurrent.Block<E>>

    let rear: std.sync.AtomicReference<std.collection.concurrent.Block<E>>

    init(capacity: Int64) { /* compiled code */ }

    init() { /* compiled code */ }

    @Deprecated[message: "Use the other version of `init` instead."]
    init(capacity: Int64, elements: std.core.Array<E>) { /* compiled code */ }

    @Deprecated[message: "Use the other version of `init` instead."]
    init(capacity: Int64, elements: std.core.Collection<E>) { /* compiled code */ }

    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use member function `public func add(element: E): Unit` instead."]
    func enqueue(element: E): Unit { /* compiled code */ }

    func add(element: E): Unit { /* compiled code */ }

    @Deprecated[message: "Use member function `public func add(element: E, timeout: Duration): Bool` instead."]
    func enqueue(element: E, timeout: std.core.Duration): Bool { /* compiled code */ }

    func add(element: E, timeout: std.core.Duration): Bool { /* compiled code */ }

    @Deprecated[message: "Use member function `public func remove(): E` instead."]
    func dequeue(): E { /* compiled code */ }

    func remove(): E { /* compiled code */ }

    @Deprecated[message: "Use member function `public func remove(timeout: Duration): Option<E>` instead."]
    func dequeue(timeout: std.core.Duration): std.core.Option<E> { /* compiled code */ }

    func remove(timeout: std.core.Duration): std.core.Option<E> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func peek(): Option<E>` instead."]
    func head(): std.core.Option<E> { /* compiled code */ }

    func peek(): std.core.Option<E> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func tryAdd(element: E): Bool` instead."]
    func tryEnqueue(element: E): Bool { /* compiled code */ }

    func tryAdd(element: E): Bool { /* compiled code */ }

    @Deprecated[message: "Use member function `public func tryRemove(): Option<E>` instead."]
    func tryDequeue(): std.core.Option<E> { /* compiled code */ }

    func tryRemove(): std.core.Option<E> { /* compiled code */ }
}

type KeyValue<K, V> = (Int64, K, V)

interface ConcurrentMap<K, V> {
    abstract open func get(key: K): std.core.Option<V>

    abstract open func contains(key: K): Bool

    @Deprecated[message: "Use member function `func add(key: K, value: V): ?V` instead."]
    abstract open func put(key: K, value: V): std.core.Option<V>

    abstract open func add(key: K, value: V): std.core.Option<V>

    @Deprecated[message: "Use member function `func addIfAbsent(key: K, value: V): ?V` instead."]
    abstract open func putIfAbsent(key: K, value: V): std.core.Option<V>

    abstract open func addIfAbsent(key: K, value: V): std.core.Option<V>

    abstract open func remove(key: K): std.core.Option<V>

    @Deprecated[message: "Use member function `func entryView(key: K, fn: (MapEntryView<K, V>)->Unit): ?V` instead."]
    abstract open func remove(key: K, predicate: (V) -> Bool): std.core.Option<V>

    abstract open func replace(key: K, value: V): std.core.Option<V>

    @Deprecated[message: "Use member function `func entryView(key: K, fn: (MapEntryView<K, V>)->Unit): ?V` instead."]
    abstract open func replace(key: K, eval: (V) -> V): std.core.Option<V>

    @Deprecated[message: "Use member function `func entryView(key: K, fn: (MapEntryView<K, V>)->Unit): ?V` instead."]
    abstract open func replace(key: K, predicate: (V) -> Bool, eval: (V) -> V): std.core.Option<V>

    abstract open func entryView(key: K, fn: (std.collection.MapEntryView<K, V>) -> Unit): std.core.Option<V>

    abstract open operator func [](key: K): V

    abstract open operator func [](key: K, value: V): Unit
}

@Deprecated[message: "Use ConcurrentLinkedQueue instead."]
type NonBlockingQueue<E> = std.collection.concurrent.ConcurrentLinkedQueue<E>
