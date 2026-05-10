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

    func find(hash: Int64, key: K): std.core.Option<V>

    operator func [](index: Int64): (Int64, K, V)
}

enum FindKey {
    | FIND(V)

    | NotFind

    | NotMigrate
}

struct Bucket {
    let atomic_ref_entries: std.sync.AtomicOptionReference<std.collection.concurrent.KVList<K, V>>

    func find(hash: Int64, key: K): std.collection.concurrent.FindKey<V>

    func copyAddIfAbsent(hash: Int64, key: K, value: V): std.core.Option<V>

    func refList: std.core.Option<std.collection.concurrent.KVList<K, V>>

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

    func getValue(hash: Int64, key: K): std.core.Option<V>

    func tryGetBucket(index: Int64): Bool
}

class MutexLockGroup {
    let mutexes: std.core.Array<std.sync.Mutex>

    let mod: Int64

    func unlock(index: Int64): Unit
}

class ConcurrentHashMapIterator <: std.core.Iterator<(K, V)> {
    let curHT: std.collection.concurrent.HNode<K, V>

    var curBucketIndex: Int64

    var curEntries: std.core.Option<std.collection.concurrent.KVList<K, V>>

    var entriesIndex: Int64

    init(cmap: std.collection.concurrent.ConcurrentHashMap<K, V>) { /* compiled code */ }

    func next: std.core.Option<(K, V)>

    func nextElement: Unit
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

    func `get`(key: K): std.core.Option<V>

    func contains(key: K): Bool

    func put(key: K, value: V): std.core.Option<V>

    func add(key: K, value: V): std.core.Option<V>

    func putIfAbsent(key: K, value: V): std.core.Option<V>

    func addIfAbsent(key: K, value: V): std.core.Option<V>

    func remove(key: K): std.core.Option<V>

    func remove(key: K, predicate: (V) -> Bool): std.core.Option<V>

    func replace(key: K, value: V): std.core.Option<V>

    func replace(key: K, eval: (V) -> V): std.core.Option<V>

    func replace(key: K, predicate: (V) -> Bool, eval: (V) -> V): std.core.Option<V>

    func entryView(key: K, fn: (std.collection.MapEntryView<K, V>) -> Unit): std.core.Option<V>

    operator func [](key: K): V

    operator func [](key: K, value: V): Unit

    prop size: Int64

    func isEmpty: Bool

    func iterator: std.collection.concurrent.ConcurrentHashMapIterator<K, V>

    func addInternal(key: K, value: V): std.core.Option<V>

    func replaceInternel(key: K, predicate: (V) -> Bool, eval: (V) -> V): std.core.Option<V>

    func tryResize(hash: Int64): Unit

    func countInc(ret: std.core.Option<V>, hash: Int64): Unit
}

class Block {
    let version: UInt64

    let isExtended: std.sync.AtomicBool

    let next: std.sync.AtomicOptionReference<std.collection.concurrent.Block<E>>

    let allocated: std.sync.AtomicInt64

    let committed: std.sync.AtomicInt64

    let reserved: std.sync.AtomicInt64

    let buffer: std.core.Array<E>

    func getAllocated: (UInt64, Int64)

    func getReserved: (UInt64, Int64)
}

class ConcurrentLinkedQueueIterator <: std.core.Iterator<E> {
    let queue: std.collection.concurrent.ConcurrentLinkedQueue<E>

    var curBlock: std.collection.concurrent.Block<E>

    var curIndex: Int64

    var hasNext: Bool

    init(queue: std.collection.concurrent.ConcurrentLinkedQueue<E>, front: std.collection.concurrent.Block<E>) { /* compiled code */ }

    func next: std.core.Option<E>

    func getNextBlock: Bool
}

class ConcurrentLinkedQueue <: std.core.Object & std.core.Collection<E> {
    let front: std.sync.AtomicReference<std.collection.concurrent.Block<E>>

    let rear: std.sync.AtomicReference<std.collection.concurrent.Block<E>>

    init() { /* compiled code */ }

    init(elements: std.core.Collection<E>) { /* compiled code */ }

    prop size: Int64

    func enqueue(element: E): Bool

    func add(element: E): Bool

    func dequeue: std.core.Option<E>

    func remove: std.core.Option<E>

    func head: std.core.Option<E>

    func peek: std.core.Option<E>

    func isEmpty: Bool

    func toArray: std.core.Array<E>

    func iterator: std.core.Iterator<E>
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

    ~init()

    prop size: Int64

    func enqueue(element: E): Unit

    func add(element: E): Unit

    func enqueue(element: E, timeout: std.core.Duration): Bool

    func add(element: E, timeout: std.core.Duration): Bool

    func dequeue: E

    func remove: E

    func dequeue(timeout: std.core.Duration): std.core.Option<E>

    func remove(timeout: std.core.Duration): std.core.Option<E>

    func head: std.core.Option<E>

    func peek: std.core.Option<E>

    func tryEnqueue(element: E): Bool

    func tryAdd(element: E): Bool

    func tryDequeue: std.core.Option<E>

    func tryRemove: std.core.Option<E>
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

    prop size: Int64

    func enqueue(element: E): Unit

    func add(element: E): Unit

    func enqueue(element: E, timeout: std.core.Duration): Bool

    func add(element: E, timeout: std.core.Duration): Bool

    func dequeue: E

    func remove: E

    func dequeue(timeout: std.core.Duration): std.core.Option<E>

    func remove(timeout: std.core.Duration): std.core.Option<E>

    func head: std.core.Option<E>

    func peek: std.core.Option<E>

    func tryEnqueue(element: E): Bool

    func tryAdd(element: E): Bool

    func tryDequeue: std.core.Option<E>

    func tryRemove: std.core.Option<E>
}

type KeyValue = (Int64, K, V)

interface ConcurrentMap {
    func `get`(key: K): std.core.Option<V>

    func contains(key: K): Bool

    func put(key: K, value: V): std.core.Option<V>

    func add(key: K, value: V): std.core.Option<V>

    func putIfAbsent(key: K, value: V): std.core.Option<V>

    func addIfAbsent(key: K, value: V): std.core.Option<V>

    func remove(key: K): std.core.Option<V>

    func remove(key: K, predicate: (V) -> Bool): std.core.Option<V>

    func replace(key: K, value: V): std.core.Option<V>

    func replace(key: K, eval: (V) -> V): std.core.Option<V>

    func replace(key: K, predicate: (V) -> Bool, eval: (V) -> V): std.core.Option<V>

    func entryView(key: K, fn: (std.collection.MapEntryView<K, V>) -> Unit): std.core.Option<V>

    operator func [](key: K): V

    operator func [](key: K, value: V): Unit
}

type NonBlockingQueue = std.collection.concurrent.ConcurrentLinkedQueue<E>
