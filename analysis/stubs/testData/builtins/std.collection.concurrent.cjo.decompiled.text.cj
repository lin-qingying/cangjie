// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.collection.concurrent

import std.sync.*
import std.time.*
import std.core.*
import std.collection.MapEntryView
import std.collection.ArrayList

let BUF_SIZE_BITS: Int64 = COMPILED_CODE /* compiled code */

let BUF_SIZE: Int64 = COMPILED_CODE /* compiled code */

internal class KVList<K, V> {
    internal var myData: std.core.Array<(Int64, K, V)>

    internal var mySize: Int64

    @Frozen
    internal func find(hash: Int64, key: K): std.core.Option<V> { /* compiled code */ }

    @Frozen
    internal operator func [](index: Int64): (Int64, K, V) { /* compiled code */ }
}

internal enum FindKey<V> {
    | FIND(V)

    | NotFind

    | NotMigrate
}

internal struct Bucket<K, V> {
    internal let atomic_ref_entries: std.sync.AtomicOptionReference<std.collection.concurrent.KVList<K, V>> = COMPILED_CODE /* compiled code */

    @Frozen
    internal func find(hash: Int64, key: K): std.collection.concurrent.FindKey<V> { /* compiled code */ }

    internal func copyAddIfAbsent(hash: Int64, key: K, value: V): std.core.Option<V> { /* compiled code */ }

    internal func refList(): std.core.Option<std.collection.concurrent.KVList<K, V>> { /* compiled code */ }

    init() { /* compiled code */ }
}

internal class HNode<K, V> {
    internal let buckets: std.core.Array<std.collection.concurrent.Bucket<K, V>>

    internal var freezeFlags: std.core.Array<Bool>

    internal let lockG: std.collection.concurrent.MutexLockGroup

    internal let version: Int64

    internal let migratingIndex: std.sync.AtomicInt64 = COMPILED_CODE /* compiled code */

    internal let migratedIndex: std.sync.AtomicInt64 = COMPILED_CODE /* compiled code */

    internal let pre: std.sync.AtomicOptionReference<std.collection.concurrent.HNode<K, V>>

    @Frozen
    internal func getValue(hash: Int64, key: K): std.core.Option<V> { /* compiled code */ }

    internal func tryGetBucket(index: Int64): Bool { /* compiled code */ }
}

internal class MutexLockGroup {
    internal let mutexes: std.core.Array<std.sync.Mutex>

    private let mod: Int64

    internal func unlock(index: Int64): Unit { /* compiled code */ }
}

public class ConcurrentHashMapIterator<K, V> <: std.core.Iterator<(K, V)> {
    private let curHT: std.collection.concurrent.HNode<K, V>

    private var curBucketIndex: Int64 = COMPILED_CODE /* compiled code */

    private var curEntries: std.core.Option<std.collection.concurrent.KVList<K, V>> = COMPILED_CODE /* compiled code */

    private var entriesIndex: Int64 = COMPILED_CODE /* compiled code */

    init(cmap: std.collection.concurrent.ConcurrentHashMap<K, V>) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<(K, V)> { /* compiled code */ }

    private func nextElement(): Unit { /* compiled code */ }
}

public class ConcurrentHashMap<K, V> <: std.core.Object & std.collection.concurrent.ConcurrentMap<K, V> & std.core.Collection<(K, V)> {
    private let counts: std.core.Array<std.sync.AtomicInt64> = COMPILED_CODE /* compiled code */

    private static const COUNT_SIZE: Int64 = COMPILED_CODE /* compiled code */

    internal let curHNodeVersion: std.sync.AtomicInt64 = COMPILED_CODE /* compiled code */

    internal let head: std.sync.AtomicReference<std.collection.concurrent.HNode<K, V>>

    internal let lockG: std.core.Array<std.collection.concurrent.MutexLockGroup>

    internal let concurrencyLevel: Int64

    init(concurrencyLevel!: Int64 = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    init(capacity: Int64, concurrencyLevel!: Int64 = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    init(elements: std.core.Collection<(K, V)>, concurrencyLevel!: Int64 = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> (K, V), concurrencyLevel!: Int64 = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    @Frozen
    public func get(key: K): std.core.Option<V> { /* compiled code */ }

    @Frozen
    public func contains(key: K): Bool { /* compiled code */ }

    @Deprecated[message: "Use member function `public func add(key: K, value: V): ?V` instead."]
    public func put(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    @Frozen
    public func add(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func addIfAbsent(key: K, value: V): ?V` instead."]
    public func putIfAbsent(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    @Frozen
    public func addIfAbsent(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    public func remove(key: K): std.core.Option<V> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func entryView(K, (MapEntryView<K, V>) -> Unit)` instead."]
    public func remove(key: K, predicate: (V) -> Bool): std.core.Option<V> { /* compiled code */ }

    @Frozen
    public func replace(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func entryView(key: K, fn: (MapEntryView<K, V>)->Unit): ?V` instead."]
    public func replace(key: K, eval: (V) -> V): std.core.Option<V> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func entryView(key: K, fn: (MapEntryView<K, V>)->Unit): ?V` instead."]
    public func replace(key: K, predicate: (V) -> Bool, eval: (V) -> V): std.core.Option<V> { /* compiled code */ }

    public func entryView(key: K, fn: (std.collection.MapEntryView<K, V>) -> Unit): std.core.Option<V> { /* compiled code */ }

    public operator func [](key: K): V { /* compiled code */ }

    public operator func [](key: K, value!: V): Unit { /* compiled code */ }

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    public func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    public func iterator(): std.collection.concurrent.ConcurrentHashMapIterator<K, V> { /* compiled code */ }

    private func addInternal(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    private func replaceInternel(key: K, predicate: (V) -> Bool, eval: (V) -> V): std.core.Option<V> { /* compiled code */ }

    private func tryResize(hash: Int64): Unit { /* compiled code */ }

    private func countInc(ret: std.core.Option<V>, hash: Int64): Unit { /* compiled code */ }
}

internal class Block<E> {
    internal let version: UInt64

    internal let isExtended: std.sync.AtomicBool = COMPILED_CODE /* compiled code */

    internal let next: std.sync.AtomicOptionReference<std.collection.concurrent.Block<E>> = COMPILED_CODE /* compiled code */

    private let allocated: std.sync.AtomicInt64 = COMPILED_CODE /* compiled code */

    internal let committed: std.sync.AtomicInt64 = COMPILED_CODE /* compiled code */

    internal let reserved: std.sync.AtomicInt64 = COMPILED_CODE /* compiled code */

    internal let buffer: std.core.Array<E> = COMPILED_CODE /* compiled code */

    internal func getAllocated(): (UInt64, Int64) { /* compiled code */ }

    internal func getReserved(): (UInt64, Int64) { /* compiled code */ }
}

internal class ConcurrentLinkedQueueIterator<E> <: std.core.Iterator<E> {
    internal let queue: std.collection.concurrent.ConcurrentLinkedQueue<E>

    internal var curBlock: std.collection.concurrent.Block<E>

    internal var curIndex: Int64

    internal var hasNext: Bool = COMPILED_CODE /* compiled code */

    init(queue: std.collection.concurrent.ConcurrentLinkedQueue<E>, front: std.collection.concurrent.Block<E>) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<E> { /* compiled code */ }

    internal func getNextBlock(): Bool { /* compiled code */ }
}

public class ConcurrentLinkedQueue<E> <: std.core.Object & std.core.Collection<E> {
    private let front: std.sync.AtomicReference<std.collection.concurrent.Block<E>>

    private let rear: std.sync.AtomicReference<std.collection.concurrent.Block<E>>

    init() { /* compiled code */ }

    @Deprecated[message: "Use the other version of `init` instead."]
    init(elements: std.core.Collection<E>) { /* compiled code */ }

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use member function `func addIfAbsent(key: K, value: V): ?V` instead."]
    public func enqueue(element: E): Bool { /* compiled code */ }

    public func add(element: E): Bool { /* compiled code */ }

    @Deprecated[message: "Use member function `public func remove(): Option<E>` instead."]
    public func dequeue(): std.core.Option<E> { /* compiled code */ }

    public func remove(): std.core.Option<E> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func peek(): Option<E>` instead."]
    public func head(): std.core.Option<E> { /* compiled code */ }

    public func peek(): std.core.Option<E> { /* compiled code */ }

    public func isEmpty(): Bool { /* compiled code */ }

    public func toArray(): std.core.Array<E> { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<E> { /* compiled code */ }
}

public class ArrayBlockingQueue<E> {
    public let capacity: Int64

    internal let buffer: std.core.Array<E>

    internal let state: std.core.CPointer<UInt8>

    internal let allocated: std.sync.AtomicUInt64 = COMPILED_CODE /* compiled code */

    internal let reserved: std.sync.AtomicUInt64 = COMPILED_CODE /* compiled code */

    internal let sendQSize: std.sync.AtomicUInt64 = COMPILED_CODE /* compiled code */

    internal let receiveQSize: std.sync.AtomicUInt64 = COMPILED_CODE /* compiled code */

    internal let sendMonitor: std.sync.Monitor = COMPILED_CODE /* compiled code */

    internal let receiveMonitor: std.sync.Monitor = COMPILED_CODE /* compiled code */

    internal let unsignedCapacity: UInt64

    init(capacity: Int64) { /* compiled code */ }

    @Deprecated[message: "Use the other version of `init` instead."]
    init(capacity: Int64, elements: std.core.Collection<E>) { /* compiled code */ }

    ~init() { /* compiled code */ }

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use member function `public func add(element: E): Unit` instead."]
    public func enqueue(element: E): Unit { /* compiled code */ }

    public func add(element: E): Unit { /* compiled code */ }

    @Deprecated[message: "Use member function `public func add(element: E, timeout: Duration): Bool` instead."]
    public func enqueue(element: E, timeout: std.core.Duration): Bool { /* compiled code */ }

    public func add(element: E, timeout: std.core.Duration): Bool { /* compiled code */ }

    @Deprecated[message: "Use member function `public func remove(): E` instead."]
    public func dequeue(): E { /* compiled code */ }

    public func remove(): E { /* compiled code */ }

    @Deprecated[message: "Use member function `public func remove(timeout: Duration): Option<E>` instead."]
    public func dequeue(timeout: std.core.Duration): std.core.Option<E> { /* compiled code */ }

    public func remove(timeout: std.core.Duration): std.core.Option<E> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func peek(): Option<E>` instead."]
    public func head(): std.core.Option<E> { /* compiled code */ }

    public func peek(): std.core.Option<E> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func tryAdd(element: E): Bool` instead."]
    public func tryEnqueue(element: E): Bool { /* compiled code */ }

    public func tryAdd(element: E): Bool { /* compiled code */ }

    @Deprecated[message: "Use member function `public func tryRemove(): Option<E>` instead."]
    public func tryDequeue(): std.core.Option<E> { /* compiled code */ }

    public func tryRemove(): std.core.Option<E> { /* compiled code */ }
}

@Deprecated[message: "Use LinkedBlockingQueue instead."]
public type BlockingQueue<E> = std.collection.concurrent.LinkedBlockingQueue<E>

public class LinkedBlockingQueue<E> {
    public let capacity: Int64

    private let sendMonitor: std.sync.Monitor = COMPILED_CODE /* compiled code */

    private let receiveMonitor: std.sync.Monitor = COMPILED_CODE /* compiled code */

    private let sendQSize: std.sync.AtomicInt64 = COMPILED_CODE /* compiled code */

    private let receiveQSize: std.sync.AtomicInt64 = COMPILED_CODE /* compiled code */

    private let front: std.sync.AtomicReference<std.collection.concurrent.Block<E>>

    private let rear: std.sync.AtomicReference<std.collection.concurrent.Block<E>>

    init(capacity: Int64) { /* compiled code */ }

    init() { /* compiled code */ }

    @Deprecated[message: "Use the other version of `init` instead."]
    init(capacity: Int64, elements: std.core.Array<E>) { /* compiled code */ }

    @Deprecated[message: "Use the other version of `init` instead."]
    init(capacity: Int64, elements: std.core.Collection<E>) { /* compiled code */ }

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use member function `public func add(element: E): Unit` instead."]
    public func enqueue(element: E): Unit { /* compiled code */ }

    public func add(element: E): Unit { /* compiled code */ }

    @Deprecated[message: "Use member function `public func add(element: E, timeout: Duration): Bool` instead."]
    public func enqueue(element: E, timeout: std.core.Duration): Bool { /* compiled code */ }

    public func add(element: E, timeout: std.core.Duration): Bool { /* compiled code */ }

    @Deprecated[message: "Use member function `public func remove(): E` instead."]
    public func dequeue(): E { /* compiled code */ }

    public func remove(): E { /* compiled code */ }

    @Deprecated[message: "Use member function `public func remove(timeout: Duration): Option<E>` instead."]
    public func dequeue(timeout: std.core.Duration): std.core.Option<E> { /* compiled code */ }

    public func remove(timeout: std.core.Duration): std.core.Option<E> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func peek(): Option<E>` instead."]
    public func head(): std.core.Option<E> { /* compiled code */ }

    public func peek(): std.core.Option<E> { /* compiled code */ }

    @Deprecated[message: "Use member function `public func tryAdd(element: E): Bool` instead."]
    public func tryEnqueue(element: E): Bool { /* compiled code */ }

    public func tryAdd(element: E): Bool { /* compiled code */ }

    @Deprecated[message: "Use member function `public func tryRemove(): Option<E>` instead."]
    public func tryDequeue(): std.core.Option<E> { /* compiled code */ }

    public func tryRemove(): std.core.Option<E> { /* compiled code */ }
}

internal type KeyValue<K, V> = (Int64, K, V)

public interface ConcurrentMap<K, V> {
    public open func get(key: K): std.core.Option<V>

    public open func contains(key: K): Bool

    @Deprecated[message: "Use member function `func add(key: K, value: V): ?V` instead."]
    public open func put(key: K, value: V): std.core.Option<V>

    public open func add(key: K, value: V): std.core.Option<V>

    @Deprecated[message: "Use member function `func addIfAbsent(key: K, value: V): ?V` instead."]
    public open func putIfAbsent(key: K, value: V): std.core.Option<V>

    public open func addIfAbsent(key: K, value: V): std.core.Option<V>

    public open func remove(key: K): std.core.Option<V>

    @Deprecated[message: "Use member function `func entryView(key: K, fn: (MapEntryView<K, V>)->Unit): ?V` instead."]
    public open func remove(key: K, predicate: (V) -> Bool): std.core.Option<V>

    public open func replace(key: K, value: V): std.core.Option<V>

    @Deprecated[message: "Use member function `func entryView(key: K, fn: (MapEntryView<K, V>)->Unit): ?V` instead."]
    public open func replace(key: K, eval: (V) -> V): std.core.Option<V>

    @Deprecated[message: "Use member function `func entryView(key: K, fn: (MapEntryView<K, V>)->Unit): ?V` instead."]
    public open func replace(key: K, predicate: (V) -> Bool, eval: (V) -> V): std.core.Option<V>

    public open func entryView(key: K, fn: (std.collection.MapEntryView<K, V>) -> Unit): std.core.Option<V>

    public open operator func [](key: K): V

    public open operator func [](key: K, value!: V): Unit
}

@Deprecated[message: "Use ConcurrentLinkedQueue instead."]
public type NonBlockingQueue<E> = std.collection.concurrent.ConcurrentLinkedQueue<E>
