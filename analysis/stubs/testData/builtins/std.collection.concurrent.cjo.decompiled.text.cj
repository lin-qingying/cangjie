// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.collection.concurrent

import std.sync.*
import std.time.*
import std.core.*
import std.collection.MapEntryView
import std.collection.ArrayList

let BUF_SIZE_BITS: Int64

let BUF_SIZE: Int64

class KVList {
    var myData: std.core.Array<(Int64, K, V)>

    var mySize: Int64

    func find(hash: Int64, key: K): std.core.Option<V> { /* compiled code */ }

    operator func [](index: Int64): (Int64, K, V) { /* compiled code */ }
}

enum FindKey {
    | FIND(V)

    | NotFind

    | NotMigrate
}

struct Bucket {
    let atomic_ref_entries: std.sync.AtomicOptionReference<std.collection.concurrent.KVList<K, V>>

    func find(hash: Int64, key: K): std.collection.concurrent.FindKey<V> { /* compiled code */ }

    func copyAddIfAbsent(hash: Int64, key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func refList(): std.core.Option<std.collection.concurrent.KVList<K, V>> { /* compiled code */ }

    init() { /* compiled code */ }
}

class HNode {
    let buckets: std.core.Array<std.collection.concurrent.Bucket<K, V>>

    var freezeFlags: std.core.Array<Bool>

    let lockG: std.collection.concurrent.MutexLockGroup

    let version: Int64

    let migratingIndex: std.sync.AtomicInt64

    let migratedIndex: std.sync.AtomicInt64

    let pre: std.sync.AtomicOptionReference<std.collection.concurrent.HNode<K, V>>

    func getValue(hash: Int64, key: K): std.core.Option<V> { /* compiled code */ }

    func tryGetBucket(index: Int64): Bool { /* compiled code */ }
}

class MutexLockGroup {
    let mutexes: std.core.Array<std.sync.Mutex>

    let mod: Int64

    func unlock(index: Int64): Unit { /* compiled code */ }
}

class ConcurrentHashMapIterator <: std.core.Iterator<(K, V)> {
    let curHT: std.collection.concurrent.HNode<K, V>

    var curBucketIndex: Int64

    var curEntries: std.core.Option<std.collection.concurrent.KVList<K, V>>

    var entriesIndex: Int64

    init(cmap: std.collection.concurrent.ConcurrentHashMap<K, V>) { /* compiled code */ }

    func next(): std.core.Option<(K, V)> { /* compiled code */ }

    func nextElement(): Unit { /* compiled code */ }
}

class ConcurrentHashMap <: std.core.Object & std.collection.concurrent.ConcurrentMap<K, V> & std.core.Collection<(K, V)> {
    let counts: std.core.Array<std.sync.AtomicInt64>

    let COUNT_SIZE: Int64

    let curHNodeVersion: std.sync.AtomicInt64

    let head: std.sync.AtomicReference<std.collection.concurrent.HNode<K, V>>

    let lockG: std.core.Array<std.collection.concurrent.MutexLockGroup>

    let concurrencyLevel: Int64

    init(concurrencyLevel: Int64) { /* compiled code */ }

    init(capacity: Int64, concurrencyLevel: Int64) { /* compiled code */ }

    init(elements: std.core.Collection<(K, V)>, concurrencyLevel: Int64) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> (K, V), concurrencyLevel: Int64) { /* compiled code */ }

    func `get`(key: K): std.core.Option<V> { /* compiled code */ }

    func contains(key: K): Bool { /* compiled code */ }

    func put(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func add(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func putIfAbsent(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func addIfAbsent(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func remove(key: K): std.core.Option<V> { /* compiled code */ }

    func remove(key: K, predicate: (V) -> Bool): std.core.Option<V> { /* compiled code */ }

    func replace(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func replace(key: K, eval: (V) -> V): std.core.Option<V> { /* compiled code */ }

    func replace(key: K, predicate: (V) -> Bool, eval: (V) -> V): std.core.Option<V> { /* compiled code */ }

    func entryView(key: K, fn: (std.collection.MapEntryView<K, V>) -> Unit): std.core.Option<V> { /* compiled code */ }

    operator func [](key: K): V { /* compiled code */ }

    operator func [](key: K, value: V): Unit { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func iterator(): std.collection.concurrent.ConcurrentHashMapIterator<K, V> { /* compiled code */ }

    func addInternal(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func replaceInternel(key: K, predicate: (V) -> Bool, eval: (V) -> V): std.core.Option<V> { /* compiled code */ }

    func tryResize(hash: Int64): Unit { /* compiled code */ }

    func countInc(ret: std.core.Option<V>, hash: Int64): Unit { /* compiled code */ }
}

class Block {
    let version: UInt64

    let isExtended: std.sync.AtomicBool

    let next: std.sync.AtomicOptionReference<std.collection.concurrent.Block<E>>

    let allocated: std.sync.AtomicInt64

    let committed: std.sync.AtomicInt64

    let reserved: std.sync.AtomicInt64

    let buffer: std.core.Array<E>

    func getAllocated(): (UInt64, Int64) { /* compiled code */ }

    func getReserved(): (UInt64, Int64) { /* compiled code */ }
}

class ConcurrentLinkedQueueIterator <: std.core.Iterator<E> {
    let queue: std.collection.concurrent.ConcurrentLinkedQueue<E>

    var curBlock: std.collection.concurrent.Block<E>

    var curIndex: Int64

    var hasNext: Bool

    init(queue: std.collection.concurrent.ConcurrentLinkedQueue<E>, front: std.collection.concurrent.Block<E>) { /* compiled code */ }

    func next(): std.core.Option<E> { /* compiled code */ }

    func getNextBlock(): Bool { /* compiled code */ }
}

class ConcurrentLinkedQueue <: std.core.Object & std.core.Collection<E> {
    let front: std.sync.AtomicReference<std.collection.concurrent.Block<E>>

    let rear: std.sync.AtomicReference<std.collection.concurrent.Block<E>>

    init() { /* compiled code */ }

    init(elements: std.core.Collection<E>) { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func enqueue(element: E): Bool { /* compiled code */ }

    func add(element: E): Bool { /* compiled code */ }

    func dequeue(): std.core.Option<E> { /* compiled code */ }

    func remove(): std.core.Option<E> { /* compiled code */ }

    func head(): std.core.Option<E> { /* compiled code */ }

    func peek(): std.core.Option<E> { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    func toArray(): std.core.Array<E> { /* compiled code */ }

    func iterator(): std.core.Iterator<E> { /* compiled code */ }
}

class ArrayBlockingQueue {
    let capacity: Int64

    let buffer: std.core.Array<E>

    let state: CPointer<UInt8>

    let allocated: std.sync.AtomicUInt64

    let reserved: std.sync.AtomicUInt64

    let sendQSize: std.sync.AtomicUInt64

    let receiveQSize: std.sync.AtomicUInt64

    let sendMonitor: std.sync.Monitor

    let receiveMonitor: std.sync.Monitor

    let unsignedCapacity: UInt64

    init(capacity: Int64) { /* compiled code */ }

    init(capacity: Int64, elements: std.core.Collection<E>) { /* compiled code */ }

    ~init() { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func enqueue(element: E): Unit { /* compiled code */ }

    func add(element: E): Unit { /* compiled code */ }

    func enqueue(element: E, timeout: std.core.Duration): Bool { /* compiled code */ }

    func add(element: E, timeout: std.core.Duration): Bool { /* compiled code */ }

    func dequeue(): E { /* compiled code */ }

    func remove(): E { /* compiled code */ }

    func dequeue(timeout: std.core.Duration): std.core.Option<E> { /* compiled code */ }

    func remove(timeout: std.core.Duration): std.core.Option<E> { /* compiled code */ }

    func head(): std.core.Option<E> { /* compiled code */ }

    func peek(): std.core.Option<E> { /* compiled code */ }

    func tryEnqueue(element: E): Bool { /* compiled code */ }

    func tryAdd(element: E): Bool { /* compiled code */ }

    func tryDequeue(): std.core.Option<E> { /* compiled code */ }

    func tryRemove(): std.core.Option<E> { /* compiled code */ }
}

type BlockingQueue = std.collection.concurrent.LinkedBlockingQueue<E>

class LinkedBlockingQueue {
    let capacity: Int64

    let sendMonitor: std.sync.Monitor

    let receiveMonitor: std.sync.Monitor

    let sendQSize: std.sync.AtomicInt64

    let receiveQSize: std.sync.AtomicInt64

    let front: std.sync.AtomicReference<std.collection.concurrent.Block<E>>

    let rear: std.sync.AtomicReference<std.collection.concurrent.Block<E>>

    init(capacity: Int64) { /* compiled code */ }

    init() { /* compiled code */ }

    init(capacity: Int64, elements: std.core.Array<E>) { /* compiled code */ }

    init(capacity: Int64, elements: std.core.Collection<E>) { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func enqueue(element: E): Unit { /* compiled code */ }

    func add(element: E): Unit { /* compiled code */ }

    func enqueue(element: E, timeout: std.core.Duration): Bool { /* compiled code */ }

    func add(element: E, timeout: std.core.Duration): Bool { /* compiled code */ }

    func dequeue(): E { /* compiled code */ }

    func remove(): E { /* compiled code */ }

    func dequeue(timeout: std.core.Duration): std.core.Option<E> { /* compiled code */ }

    func remove(timeout: std.core.Duration): std.core.Option<E> { /* compiled code */ }

    func head(): std.core.Option<E> { /* compiled code */ }

    func peek(): std.core.Option<E> { /* compiled code */ }

    func tryEnqueue(element: E): Bool { /* compiled code */ }

    func tryAdd(element: E): Bool { /* compiled code */ }

    func tryDequeue(): std.core.Option<E> { /* compiled code */ }

    func tryRemove(): std.core.Option<E> { /* compiled code */ }
}

type KeyValue = (Int64, K, V)

interface ConcurrentMap {
    func `get`(key: K): std.core.Option<V> { /* compiled code */ }

    func contains(key: K): Bool { /* compiled code */ }

    func put(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func add(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func putIfAbsent(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func addIfAbsent(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func remove(key: K): std.core.Option<V> { /* compiled code */ }

    func remove(key: K, predicate: (V) -> Bool): std.core.Option<V> { /* compiled code */ }

    func replace(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func replace(key: K, eval: (V) -> V): std.core.Option<V> { /* compiled code */ }

    func replace(key: K, predicate: (V) -> Bool, eval: (V) -> V): std.core.Option<V> { /* compiled code */ }

    func entryView(key: K, fn: (std.collection.MapEntryView<K, V>) -> Unit): std.core.Option<V> { /* compiled code */ }

    operator func [](key: K): V { /* compiled code */ }

    operator func [](key: K, value: V): Unit { /* compiled code */ }
}

type NonBlockingQueue = std.collection.concurrent.ConcurrentLinkedQueue<E>
