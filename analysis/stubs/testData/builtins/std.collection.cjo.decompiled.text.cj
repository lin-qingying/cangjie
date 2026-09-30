// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.collection

import std.core.*
import std.math.*

@Frozen
func collectionToString<C, T>(col: C): std.core.String { /* compiled code */ }

@Frozen
func max(a: Int64, b: Int64): Int64 { /* compiled code */ }

@Frozen
func reduce<T>(operation: (T, T) -> T): std.core.Option<T> { /* compiled code */ }

@Frozen
func filter<T>(predicate: (T) -> Bool): std.core.Iterator<T> { /* compiled code */ }

@Frozen
func map<T, R>(transform: (T) -> R): std.core.Iterator<R> { /* compiled code */ }

@Frozen
func concat<T>(other: std.core.Iterable<T>): std.core.Iterator<T> { /* compiled code */ }

@Frozen
func collectArray<T>(it: std.core.Iterable<T>): std.core.Array<T> { /* compiled code */ }

class ArrayList<T> <: std.core.Object & std.collection.List<T> {
    var myData: std.core.Array<T>

    var mySize: Int64

    @Annotation
    var myVersion: Int64

    @Annotation
    const DEFAULT_CAPACITY: Int64

    @Frozen
    @Annotation
    prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    init() { /* compiled code */ }

    @Frozen
    init(capacity: Int64) { /* compiled code */ }

    @Frozen
    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    @Frozen
    init(elements: std.core.Array<T>) { /* compiled code */ }

    @Frozen
    init(elements: std.core.Collection<T>) { /* compiled code */ }

    @Frozen
    static func of(elements: std.core.Array<T>): std.collection.ArrayList<T> { /* compiled code */ }

    @Frozen
    func getRawArray(): std.core.Array<T> { /* compiled code */ }

    @Frozen
    func toArray(): std.core.Array<T> { /* compiled code */ }

    @Frozen
    func get(index: Int64): std.core.Option<T> { /* compiled code */ }

    @Frozen
    @Annotation
    prop first: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    @Frozen
    @Annotation
    prop last: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    @Frozen
    func add(element: T): Unit { /* compiled code */ }

    @Frozen
    func add(all: std.core.Collection<T>): Unit { /* compiled code */ }

    @Frozen
    func add(element: T, at: Int64): Unit { /* compiled code */ }

    @Frozen
    func add(all: std.core.Collection<T>, at: Int64): Unit { /* compiled code */ }

    @Frozen
    func remove(at: Int64): T { /* compiled code */ }

    @Frozen
    func remove(range: std.core.Range<Int64>): Unit { /* compiled code */ }

    @Frozen
    func removeIf(predicate: (T) -> Bool): Unit { /* compiled code */ }

    @Frozen
    func clear(): Unit { /* compiled code */ }

    @Frozen
    func reserve(additional: Int64): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use global function `public func sort<T>(data: ArrayList<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit` in std.sort instead."]
    func sortBy(stable: Bool, comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use global function `public func sort<T>(data: ArrayList<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit` in std.sort instead."]
    func sortBy(comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

    @Frozen
    func reverse(): Unit { /* compiled code */ }

    @Frozen
    operator func [](range: std.core.Range<Int64>): std.collection.ArrayList<T> { /* compiled code */ }

    @Frozen
    func slice(range: std.core.Range<Int64>): std.collection.ArrayList<T> { /* compiled code */ }

    @Frozen
    func rangeSlice(arr: std.core.Array<T>, range: std.core.Range<Int64>, size: Int64): std.core.Array<T> { /* compiled code */ }

    @Frozen
    func clone(): std.collection.ArrayList<T> { /* compiled code */ }

    @Frozen
    operator func [](index: Int64): T { /* compiled code */ }

    @Frozen
    operator func [](index: Int64, value: T): Unit { /* compiled code */ }

    @Frozen
    func iterator(): std.core.Iterator<T> { /* compiled code */ }

    @Frozen
    func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    func checkRange(index: Int64): Unit { /* compiled code */ }

    @Frozen
    func checkRangeInsert(index: Int64): Unit { /* compiled code */ }

    @Frozen
    func grow(minCapacity: Int64, startIndex: Int64): Unit { /* compiled code */ }

    func updateVersion(): Unit { /* compiled code */ }
}

@Frozen
func collectArrayList<T>(it: std.core.Iterable<T>): std.collection.ArrayList<T> { /* compiled code */ }

@Frozen
func collectHashSet<T>(it: std.core.Iterable<T>): std.collection.HashSet<T> { /* compiled code */ }

class HashSet<T> <: std.core.Object & std.collection.Set<T> {
    var map: std.collection.HashMap<T, Unit>

    @Frozen
    init() { /* compiled code */ }

    @Frozen
    init(elements: std.core.Collection<T>) { /* compiled code */ }

    @Frozen
    init(elements: std.core.Array<T>) { /* compiled code */ }

    @Frozen
    init(capacity: Int64) { /* compiled code */ }

    @Frozen
    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    @Frozen
    func contains(element: T): Bool { /* compiled code */ }

    @Frozen
    func subsetOf(other: std.collection.ReadOnlySet<T>): Bool { /* compiled code */ }

    @Frozen
    func contains(all: std.core.Collection<T>): Bool { /* compiled code */ }

    @Frozen
    func add(element: T): Bool { /* compiled code */ }

    @Frozen
    func remove(element: T): Bool { /* compiled code */ }

    @Frozen
    func add(all: std.core.Collection<T>): Unit { /* compiled code */ }

    @Frozen
    func remove(all: std.core.Collection<T>): Unit { /* compiled code */ }

    @Frozen
    func removeIf(predicate: (T) -> Bool): Unit { /* compiled code */ }

    @Frozen
    func clear(): Unit { /* compiled code */ }

    @Frozen
    func retain(all: std.collection.Set<T>): Unit { /* compiled code */ }

    @Frozen
    func clone(): std.collection.HashSet<T> { /* compiled code */ }

    @Frozen
    func reserve(additional: Int64): Unit { /* compiled code */ }

    @Frozen
    @Annotation
    prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    func iterator(): std.core.Iterator<T> { /* compiled code */ }

    @Frozen
    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    func toArray(): std.core.Array<T> { /* compiled code */ }

    @Frozen
    operator func &(other: std.collection.ReadOnlySet<T>): std.collection.HashSet<T> { /* compiled code */ }

    @Frozen
    operator func |(other: std.collection.ReadOnlySet<T>): std.collection.HashSet<T> { /* compiled code */ }

    @Frozen
    operator func -(other: std.collection.ReadOnlySet<T>): std.collection.HashSet<T> { /* compiled code */ }
}

@Frozen
func collectHashMap<K, V>(it: std.core.Iterable<(K, V)>): std.collection.HashMap<K, V> { /* compiled code */ }

class HashMap<K, V> <: std.core.Object & std.collection.Map<K, V> {
    @Annotation
    const MAX_BUCKET_SIZE: Int64

    @Annotation
    const LOAD_FACTOR: Float32

    @Annotation
    const DEFAULT_CAPACITY: Int64

    @Annotation
    var appendIndex: Int64

    @Annotation
    var freeIndex: Int64

    @Annotation
    var freeSize: Int64

    @Annotation
    var modCount: Int64

    var buckets: std.core.Array<Int64>

    var entries: std.core.Array<std.collection.HashMapEntry<K, V>>

    @Frozen
    init() { /* compiled code */ }

    @Frozen
    init(elements: std.core.Collection<(K, V)>) { /* compiled code */ }

    @Frozen
    init(elements: std.core.Array<(K, V)>) { /* compiled code */ }

    @Frozen
    init(capacity: Int64) { /* compiled code */ }

    @Frozen
    init(size: Int64, initElement: (Int64) -> (K, V)) { /* compiled code */ }

    @Frozen
    init(other: std.collection.HashMap<K, V>) { /* compiled code */ }

    @Frozen
    func get(key: K): std.core.Option<V> { /* compiled code */ }

    @Frozen
    func add(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    @Frozen
    func putWithoutResize(key: K, value: V): Unit { /* compiled code */ }

    @Frozen
    func entryView(key: K): std.collection.MapEntryView<K, V> { /* compiled code */ }

    @Frozen
    func add(all: std.core.Collection<(K, V)>): Unit { /* compiled code */ }

    @Frozen
    func remove(key: K): std.core.Option<V> { /* compiled code */ }

    @Frozen
    func remove(all: std.core.Collection<K>): Unit { /* compiled code */ }

    @Frozen
    func removeIf(predicate: (K, V) -> Bool): Unit { /* compiled code */ }

    @Frozen
    func clear(): Unit { /* compiled code */ }

    @Frozen
    func reserve(additional: Int64): Unit { /* compiled code */ }

    @Frozen
    @Annotation
    prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    func contains(all: std.core.Collection<K>): Bool { /* compiled code */ }

    @Frozen
    func contains(key: K): Bool { /* compiled code */ }

    @Frozen
    func clone(): std.collection.HashMap<K, V> { /* compiled code */ }

    @Frozen
    func keys(): std.collection.EquatableCollection<K> { /* compiled code */ }

    @Frozen
    func values(): std.core.Collection<V> { /* compiled code */ }

    @Frozen
    operator func [](key: K): V { /* compiled code */ }

    @Frozen
    operator func [](key: K, value: V): Unit { /* compiled code */ }

    @Frozen
    func getIndex(hash: Int64, size: Int64): Int64 { /* compiled code */ }

    @Frozen
    func getHash(hashCode: Int64): Int64 { /* compiled code */ }

    @Frozen
    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    func iterator(): std.collection.HashMapIterator<K, V> { /* compiled code */ }

    @Frozen
    func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    func version(): Int64 { /* compiled code */ }

    @Frozen
    func max(l: Int64, r: Int64): Int64 { /* compiled code */ }

    @Frozen
    func toArray(): std.core.Array<(K, V)> { /* compiled code */ }

    @Frozen
    func resize(argCap: Int64): Bool { /* compiled code */ }

    @Frozen
    static func tableSizeFor(cap: Int64): Int64 { /* compiled code */ }

    @Frozen
    func removePosition(hash: Int64, pos: Int64): Unit { /* compiled code */ }
}

@Annotation
let DEGREE: Int64

@Annotation
let MIN_ENTRY_SIZE: Int64

@Annotation
let MAX_ENTRY_SIZE: Int64

@Annotation
let MIN_CHILD_SIZE: Int64

@Annotation
let MAX_CHILD_SIZE: Int64

class ArrayDequeIterator<T> <: std.core.Iterator<T> {
    var myData: std.collection.ArrayDeque<T>

    var myIndex: Int64

    let lockVersion: Int64

    init(data: std.collection.ArrayDeque<T>) { /* compiled code */ }

    @Frozen
    func next(): std.core.Option<T> { /* compiled code */ }
}

class ArrayDeque<T> <: std.core.Object & std.collection.Deque<T> {
    var _data: std.core.Array<T>

    @Annotation
    var _version: Int64

    @Annotation
    var _head: Int64

    @Annotation
    var _tail: Int64

    init() { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Annotation
    prop first: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    @Annotation
    prop last: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    @Annotation
    prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func addFirst(element: T): Unit { /* compiled code */ }

    func addLast(element: T): Unit { /* compiled code */ }

    func removeFirst(): std.core.Option<T> { /* compiled code */ }

    func removeLast(): std.core.Option<T> { /* compiled code */ }

    func toArray(): std.core.Array<T> { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    func reserve(additional: Int64): Unit { /* compiled code */ }

    @Frozen
    func iterator(): std.core.Iterator<T> { /* compiled code */ }
}

class ConcurrentModificationException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class ArrayListIterator<T> <: std.core.Iterator<T> {
    @Annotation
    var myPosition: Int64

    let myData: std.collection.ArrayList<T>

    let initVersion: Int64

    @Frozen
    init(data: std.collection.ArrayList<T>) { /* compiled code */ }

    @Frozen
    func next(): std.core.Option<T> { /* compiled code */ }
}

func stableSort<T>(data: std.core.Array<T>, comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

func unstableSort<T>(data: std.core.Array<T>, comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

class ArrayStackIterator<T> <: std.core.Iterator<T> {
    let _stack: std.collection.ArrayStack<T>

    var _position: Int64

    let _lockVersion: Int64

    init(data: std.collection.ArrayStack<T>) { /* compiled code */ }

    @Frozen
    func next(): std.core.Option<T> { /* compiled code */ }
}

class ArrayStack<T> <: std.core.Object & std.collection.Stack<T> {
    @Annotation
    var _data: std.core.Array<T>

    @Annotation
    var _size: Int64

    @Annotation
    var _version: Int64

    init(capacity: Int64) { /* compiled code */ }

    init() { /* compiled code */ }

    func peek(): std.core.Option<T> { /* compiled code */ }

    func remove(): std.core.Option<T> { /* compiled code */ }

    func add(element: T): Unit { /* compiled code */ }

    @Annotation
    prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func reserve(additional: Int64): Unit { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    func toArray(): std.core.Array<T> { /* compiled code */ }

    @Frozen
    func iterator(): std.core.Iterator<T> { /* compiled code */ }
}

struct HashMapEntry<K, V> {
    let hash: Int64

    let next: Int64

    let key: K

    let value: V

    @Frozen
    init(h: Int64, n: Int64, k: K, v: V) { /* compiled code */ }

    @Frozen
    init() { /* compiled code */ }
}

class HashMapKeys<K, V> <: std.core.Object & std.collection.EquatableCollection<K> {
    let map: std.collection.HashMap<K, V>

    @Frozen
    init(m: std.collection.HashMap<K, V>) { /* compiled code */ }

    @Frozen
    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    func contains(element: K): Bool { /* compiled code */ }

    @Frozen
    func contains(all: std.core.Collection<K>): Bool { /* compiled code */ }

    @Frozen
    func iterator(): std.core.Iterator<K> { /* compiled code */ }

    @Frozen
    func toArray(): std.core.Array<K> { /* compiled code */ }
}

class HashMapIterator<K, V> <: std.core.Iterator<(K, V)> {
    var lockVersion: Int64

    var index: Int64

    let data: std.collection.HashMap<K, V>

    var isRemoved: Bool

    @Frozen
    init(map: std.collection.HashMap<K, V>) { /* compiled code */ }

    @Frozen
    func next(): std.core.Option<(K, V)> { /* compiled code */ }

    @Frozen
    func remove(): std.core.Option<(K, V)> { /* compiled code */ }
}

class HashMapValues<K, V> <: std.core.Object & std.core.Collection<V> {
    let map: std.collection.HashMap<K, V>

    @Frozen
    init(m: std.collection.HashMap<K, V>) { /* compiled code */ }

    @Frozen
    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    func iterator(): std.core.Iterator<V> { /* compiled code */ }

    @Frozen
    func toArray(): std.core.Array<V> { /* compiled code */ }
}

class HashMapEntryView<K, V> <: std.core.Object & std.collection.MapEntryView<K, V> {
    let map: std.collection.HashMap<K, V>

    let _key: K

    let hash: Int64

    var index: Int64

    var _isAbsent: Bool

    var lockVersion: Int64

    @Frozen
    init(isAbsent: Bool, key: K, hash: Int64, index: Int64, map: std.collection.HashMap<K, V>) { /* compiled code */ }

    @Frozen
    @Annotation
    prop key: K {
        get() { /* compiled code */ }
    }

    @Frozen
    @Annotation
    mut prop value: std.core.Option<V> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Frozen
    func getKey(): K { /* compiled code */ }

    @Frozen
    func getValue(): std.core.Option<V> { /* compiled code */ }

    @Frozen
    func setValue(v: V): Unit { /* compiled code */ }
}

interface MapEntryView<K, V> {
    @Annotation
    abstract open prop key: K

    @Annotation
    abstract open mut prop value: std.core.Option<V>
}

interface EquatableCollection<T> <: std.core.Collection<T> {
    abstract open func contains(element: T): Bool

    abstract open func contains(all: std.core.Collection<T>): Bool
}

interface ReadOnlySet<T> <: std.core.Collection<T> {
    abstract open func contains(element: T): Bool

    abstract open func contains(all: std.core.Collection<T>): Bool

    abstract open func subsetOf(other: std.collection.ReadOnlySet<T>): Bool
}

interface Set<T> <: std.collection.ReadOnlySet<T> {
    abstract open func add(element: T): Bool

    abstract open func add(all: std.core.Collection<T>): Unit

    abstract open func remove(element: T): Bool

    abstract open func remove(all: std.core.Collection<T>): Unit

    abstract open func removeIf(predicate: (T) -> Bool): Unit

    abstract open func clear(): Unit

    abstract open func retain(all: std.collection.Set<T>): Unit
}

class LinkedListNode<T> {
    var _next: std.core.Option<std.collection.LinkedListNode<T>>

    var _prev: std.core.Option<std.collection.LinkedListNode<T>>

    var _item: T

    var _list: std.core.Option<std.collection.LinkedList<T>>

    init(element: T, prev: std.core.Option<std.collection.LinkedListNode<T>>, next: std.core.Option<std.collection.LinkedListNode<T>>, belongs: std.core.Option<std.collection.LinkedList<T>>) { /* compiled code */ }

    @Frozen
    @Annotation
    mut prop value: T {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Frozen
    @Annotation
    prop next: std.core.Option<std.collection.LinkedListNode<T>> {
        get() { /* compiled code */ }
    }

    @Frozen
    @Annotation
    prop prev: std.core.Option<std.collection.LinkedListNode<T>> {
        get() { /* compiled code */ }
    }

    @Frozen
    func insertBefore(element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    @Frozen
    func insertAfter(element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    @Frozen
    func remove(): T { /* compiled code */ }

    func reverse(): Unit { /* compiled code */ }

    func checkNodeState(): Unit { /* compiled code */ }
}

class LinkedList<T> <: std.core.Object & std.core.Collection<T> {
    @Annotation
    var _size: Int64

    @Annotation
    var _head: std.core.Option<std.collection.LinkedListNode<T>>

    @Annotation
    var _tail: std.core.Option<std.collection.LinkedListNode<T>>

    @Annotation
    var _version: Int64

    init() { /* compiled code */ }

    init(elements: std.core.Collection<T>) { /* compiled code */ }

    init(elements: std.core.Array<T>) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Annotation
    prop first: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    @Annotation
    prop last: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    @Annotation
    prop firstNode: std.core.Option<std.collection.LinkedListNode<T>> {
        get() { /* compiled code */ }
    }

    @Annotation
    prop lastNode: std.core.Option<std.collection.LinkedListNode<T>> {
        get() { /* compiled code */ }
    }

    @Annotation
    prop version: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    func addFirst(element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    @Frozen
    func addLast(element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    func toArray(): std.core.Array<T> { /* compiled code */ }

    func addBefore(node: std.collection.LinkedListNode<T>, element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    func addAfter(node: std.collection.LinkedListNode<T>, element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    @Frozen
    func remove(node: std.collection.LinkedListNode<T>): T { /* compiled code */ }

    func removeIf(predicate: (T) -> Bool): Unit { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    func reverse(): Unit { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    func iterator(): std.core.Iterator<T> { /* compiled code */ }

    func removeFirst(): std.core.Option<T> { /* compiled code */ }

    func removeLast(): std.core.Option<T> { /* compiled code */ }

    func nodeAt(index: Int64): std.core.Option<std.collection.LinkedListNode<T>> { /* compiled code */ }

    func splitOff(node: std.collection.LinkedListNode<T>): std.collection.LinkedList<T> { /* compiled code */ }

    func forward(mark: std.collection.LinkedListNode<T>): std.core.Iterator<T> { /* compiled code */ }

    func backward(mark: std.collection.LinkedListNode<T>): std.core.Iterator<T> { /* compiled code */ }

    func checkNode(node: std.collection.LinkedListNode<T>): Unit { /* compiled code */ }
}

class LinkedListIterator<T> <: std.core.Iterator<T> {
    let _lockVersion: Int64

    let _data: std.collection.LinkedList<T>

    var _currentNode: std.collection.LinkedListNode<T>

    let _backward: Bool

    init(data: std.collection.LinkedList<T>, node: std.core.Option<std.collection.LinkedListNode<T>>, backward: Bool) { /* compiled code */ }

    @Frozen
    func next(): std.core.Option<T> { /* compiled code */ }
}

interface Map<K, V> <: std.collection.ReadOnlyMap<K, V> {
    abstract open func add(key: K, value: V): std.core.Option<V>

    abstract open func add(all: std.core.Collection<(K, V)>): Unit

    abstract open func remove(key: K): std.core.Option<V>

    abstract open func remove(all: std.core.Collection<K>): Unit

    abstract open func removeIf(predicate: (K, V) -> Bool): Unit

    abstract open func clear(): Unit

    abstract open operator func [](key: K, value: V): Unit

    abstract open func entryView(k: K): std.collection.MapEntryView<K, V>

    @Frozen
    open func addIfAbsent(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    @Frozen
    open func replace(key: K, value: V): std.core.Option<V> { /* compiled code */ }
}

struct TreeMapEntryView<K, V> <: std.collection.MapEntryView<K, V> {
    let map: std.collection.TreeMap<K, V>

    var curNode: std.collection.SearchResult<K, V>

    var lockVersion: Int64

    var _key: std.core.Option<K>

    init(map: std.collection.TreeMap<K, V>, curNode: std.collection.SearchResult<K, V>, key: std.core.Option<K>) { /* compiled code */ }

    @Annotation
    prop key: K {
        get() { /* compiled code */ }
    }

    @Annotation
    mut prop value: std.core.Option<V> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func nextNode(): std.collection.TreeMapEntryView<K, V> { /* compiled code */ }

    func prevNode(): std.collection.TreeMapEntryView<K, V> { /* compiled code */ }

    func isSome(): Bool { /* compiled code */ }
}

class TreeMap<K, V> <: std.core.Object & std.collection.OrderedMap<K, V> {
    @Annotation
    var _size: Int64

    @Annotation
    var modCount: Int64

    var root: std.collection.Node<K, V>

    init() { /* compiled code */ }

    init(elements: std.core.Collection<(K, V)>) { /* compiled code */ }

    init(elements: std.core.Array<(K, V)>) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> (K, V)) { /* compiled code */ }

    func get(key: K): std.core.Option<V> { /* compiled code */ }

    func contains(key: K): Bool { /* compiled code */ }

    func contains(all: std.core.Collection<K>): Bool { /* compiled code */ }

    func add(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func add(all: std.core.Collection<(K, V)>): Unit { /* compiled code */ }

    func remove(key: K): std.core.Option<V> { /* compiled code */ }

    func remove(all: std.core.Collection<K>): Unit { /* compiled code */ }

    func removeIf(predicate: (K, V) -> Bool): Unit { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    func clone(): std.collection.TreeMap<K, V> { /* compiled code */ }

    operator func [](key: K): V { /* compiled code */ }

    operator func [](key: K, value: V): Unit { /* compiled code */ }

    func keys(): std.collection.EquatableCollection<K> { /* compiled code */ }

    func values(): std.core.Collection<V> { /* compiled code */ }

    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    func iterator(): std.core.Iterator<(K, V)> { /* compiled code */ }

    @Annotation
    prop first: std.core.Option<(K, V)> {
        get() { /* compiled code */ }
    }

    func removeFirst(): std.core.Option<(K, V)> { /* compiled code */ }

    @Annotation
    prop last: std.core.Option<(K, V)> {
        get() { /* compiled code */ }
    }

    func removeLast(): std.core.Option<(K, V)> { /* compiled code */ }

    func searchNearestEntryView(array: std.collection.ArrayList<K>, key: K, forward: Bool): std.collection.TreeMapEntryView<K, V> { /* compiled code */ }

    func backward(mark: K, inclusive: Bool): std.core.Iterator<(K, V)> { /* compiled code */ }

    func forward(mark: K, inclusive: Bool): std.core.Iterator<(K, V)> { /* compiled code */ }

    func version(): Int64 { /* compiled code */ }

    func getFirstEntry(): std.collection.SearchResult<K, V> { /* compiled code */ }

    func getLastEntry(): std.collection.SearchResult<K, V> { /* compiled code */ }

    func searchEntry(node: std.core.Option<std.collection.Node<K, V>>, key: K): std.collection.SearchResult<K, V> { /* compiled code */ }

    func searchEntryFromRoot(key: K): std.collection.SearchResult<K, V> { /* compiled code */ }

    func inserEntry(node: std.collection.Node<K, V>, entry: std.collection.TreeMapEntry<K, V>): std.core.Option<V> { /* compiled code */ }

    func splitNode(node: std.collection.Node<K, V>, splitNode: std.collection.Node<K, V>, index: Int64): Unit { /* compiled code */ }

    func deleteEntry(node: std.collection.Node<K, V>, key: K): std.core.Option<V> { /* compiled code */ }

    func predecessor(sr: std.collection.SearchResult<K, V>): std.collection.SearchResult<K, V> { /* compiled code */ }

    func successor(sr: std.collection.SearchResult<K, V>): std.collection.SearchResult<K, V> { /* compiled code */ }

    func parentOf(node: std.collection.Node<K, V>): std.collection.SearchResult<K, V> { /* compiled code */ }

    func entryView(k: K): std.collection.MapEntryView<K, V> { /* compiled code */ }
}

struct SearchResult<K, V> {
    var _index: Int64

    var _replaceNode: std.core.Option<std.collection.Node<K, V>>

    init(index: Int64, node: std.core.Option<std.collection.Node<K, V>>) { /* compiled code */ }

    @Annotation
    prop index: Int64 {
        get() { /* compiled code */ }
    }

    @Annotation
    prop replaceNode: std.core.Option<std.collection.Node<K, V>> {
        get() { /* compiled code */ }
    }
}

class TreeMapIterator<K, V> <: std.core.Iterator<(K, V)> {
    var curNode: std.collection.TreeMapEntryView<K, V>

    init(node: std.collection.TreeMapEntryView<K, V>) { /* compiled code */ }

    @Frozen
    func next(): std.core.Option<(K, V)> { /* compiled code */ }
}

func checkVersion<K, V>(node: std.collection.TreeMapEntryView<K, V>): Unit { /* compiled code */ }

class TreeSet<T> <: std.core.Object & std.collection.OrderedSet<T> {
    var map: std.collection.TreeMap<T, Unit>

    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Annotation
    prop first: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    @Annotation
    prop last: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }

    init(elements: std.core.Collection<T>) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    static func of(elements: std.core.Array<T>): std.collection.TreeSet<T> { /* compiled code */ }

    func contains(element: T): Bool { /* compiled code */ }

    func contains(all: std.core.Collection<T>): Bool { /* compiled code */ }

    func add(element: T): Bool { /* compiled code */ }

    func add(all: std.core.Collection<T>): Unit { /* compiled code */ }

    func remove(element: T): Bool { /* compiled code */ }

    func remove(all: std.core.Collection<T>): Unit { /* compiled code */ }

    func removeIf(predicate: (T) -> Bool): Unit { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    func clone(): std.collection.TreeSet<T> { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    func iterator(): std.core.Iterator<T> { /* compiled code */ }

    func removeFirst(): std.core.Option<T> { /* compiled code */ }

    func removeLast(): std.core.Option<T> { /* compiled code */ }

    func backward(mark: T, inclusive: Bool): std.core.Iterator<T> { /* compiled code */ }

    func forward(mark: T, inclusive: Bool): std.core.Iterator<T> { /* compiled code */ }

    func retain(all: std.collection.Set<T>): Unit { /* compiled code */ }

    func toArray(): std.core.Array<T> { /* compiled code */ }

    func subsetOf(other: std.collection.ReadOnlySet<T>): Bool { /* compiled code */ }

    operator func &(other: std.collection.ReadOnlySet<T>): std.collection.TreeSet<T> { /* compiled code */ }

    operator func |(other: std.collection.ReadOnlySet<T>): std.collection.TreeSet<T> { /* compiled code */ }

    operator func -(other: std.collection.ReadOnlySet<T>): std.collection.TreeSet<T> { /* compiled code */ }
}

interface Deque<T> <: std.core.Collection<T> {
    @Annotation
    abstract open prop first: std.core.Option<T>

    @Annotation
    abstract open prop last: std.core.Option<T>

    abstract open func addFirst(element: T): Unit

    abstract open func addLast(element: T): Unit

    abstract open func removeFirst(): std.core.Option<T>

    abstract open func removeLast(): std.core.Option<T>
}

extend std.collection.ArrayDeque<T> <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

interface List<T> <: std.collection.ReadOnlyList<T> {
    abstract open func add(element: T): Unit

    abstract open func add(all: std.core.Collection<T>): Unit

    abstract open func add(element: T, at: Int64): Unit

    abstract open func add(all: std.core.Collection<T>, at: Int64): Unit

    abstract open func remove(at: Int64): T

    abstract open func remove(range: std.core.Range<Int64>): Unit

    abstract open func removeIf(predicate: (T) -> Bool): Unit

    abstract open func clear(): Unit

    abstract open operator func [](index: Int64, value: T): Unit
}

extend std.collection.ArrayList<T> <: Equatable {
    @Frozen
    operator func ==(that: std.collection.ArrayList<T>): Bool { /* compiled code */ }

    @Frozen
    operator func !=(that: std.collection.ArrayList<T>): Bool { /* compiled code */ }

    @Frozen
    func contains(element: T): Bool { /* compiled code */ }
}

extend std.collection.ArrayList<T> {
    @Frozen
    @Deprecated[message: "Use global function `public func sort<T>(data: ArrayList<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` in std.sort instead."]
    func sort(stable: Bool): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use global function `public func sort<T>(data: ArrayList<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` in std.sort instead."]
    func sort(): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use global function `public func sort<T>(data: ArrayList<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` in std.sort instead."]
    func sortDescending(stable: Bool): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use global function `public func sort<T>(data: ArrayList<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` in std.sort instead."]
    func sortDescending(): Unit { /* compiled code */ }
}

extend std.collection.ArrayList<T> <: ToString {
    @Frozen
    func toString(): std.core.String { /* compiled code */ }
}

class TempArray<T> {
    var data: std.core.Array<T>

    let zero: T

    let max: Int64

    init(n: Int64, z: T) { /* compiled code */ }

    func getArray(n: Int64): std.core.Array<T> { /* compiled code */ }
}

class ArrayQueue<T> <: std.core.Object & std.collection.Queue<T> {
    let _data: std.collection.ArrayDeque<T>

    init() { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    func peek(): std.core.Option<T> { /* compiled code */ }

    func add(element: T): Unit { /* compiled code */ }

    func remove(): std.core.Option<T> { /* compiled code */ }

    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Annotation
    prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    @Frozen
    func iterator(): std.core.Iterator<T> { /* compiled code */ }

    func toArray(): std.core.Array<T> { /* compiled code */ }

    func reserve(additional: Int64): Unit { /* compiled code */ }
}

interface Queue<T> <: std.core.Collection<T> {
    abstract open func add(element: T): Unit

    abstract open func peek(): std.core.Option<T>

    abstract open func remove(): std.core.Option<T>
}

extend std.collection.ArrayQueue<T> <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

interface Stack<T> <: std.core.Collection<T> {
    abstract open func peek(): std.core.Option<T>

    abstract open func remove(): std.core.Option<T>

    abstract open func add(element: T): Unit
}

extend std.collection.ArrayStack<T> <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

func fold<T, R>(initial: R, operation: (R, T) -> R): R { /* compiled code */ }

func isEmpty<T>(it: std.core.Iterable<T>): Bool { /* compiled code */ }

func count<T>(it: std.core.Iterable<T>): Int64 { /* compiled code */ }

func contains<T>(element: T): Bool { /* compiled code */ }

func max<T>(it: std.core.Iterable<T>): std.core.Option<T> { /* compiled code */ }

func min<T>(it: std.core.Iterable<T>): std.core.Option<T> { /* compiled code */ }

func all<T>(predicate: (T) -> Bool): Bool { /* compiled code */ }

func any<T>(predicate: (T) -> Bool): Bool { /* compiled code */ }

func none<T>(predicate: (T) -> Bool): Bool { /* compiled code */ }

func first<T>(it: std.core.Iterable<T>): std.core.Option<T> { /* compiled code */ }

func last<T>(it: std.core.Iterable<T>): std.core.Option<T> { /* compiled code */ }

func at<T>(n: Int64): std.core.Option<T> { /* compiled code */ }

func forEach<T>(action: (T) -> Unit): Unit { /* compiled code */ }

extend std.collection.HashMap<K, V> <: ToString {
    @Frozen
    func toString(): std.core.String { /* compiled code */ }
}

extend std.collection.HashMap<K, V> <: Equatable {
    @Frozen
    operator func ==(right: std.collection.HashMap<K, V>): Bool { /* compiled code */ }

    @Frozen
    operator func !=(right: std.collection.HashMap<K, V>): Bool { /* compiled code */ }
}

extend std.collection.HashSet<T> <: Equatable {
    @Frozen
    operator func ==(that: std.collection.HashSet<T>): Bool { /* compiled code */ }

    @Frozen
    operator func !=(that: std.collection.HashSet<T>): Bool { /* compiled code */ }
}

extend std.collection.HashSet<T> <: ToString {
    @Frozen
    func toString(): std.core.String { /* compiled code */ }
}

extend std.collection.LinkedList<T> <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend std.collection.LinkedList<T> <: Equatable {
    operator func ==(right: std.collection.LinkedList<T>): Bool { /* compiled code */ }

    operator func !=(right: std.collection.LinkedList<T>): Bool { /* compiled code */ }
}

interface ReadOnlyList<T> <: std.core.Collection<T> {
    @Annotation
    abstract open prop first: std.core.Option<T>

    @Annotation
    abstract open prop last: std.core.Option<T>

    abstract open func get(index: Int64): std.core.Option<T>

    abstract open operator func [](index: Int64): T
}

interface ReadOnlyMap<K, V> <: std.core.Collection<(K, V)> {
    abstract open func get(key: K): std.core.Option<V>

    abstract open func contains(key: K): Bool

    abstract open func contains(all: std.core.Collection<K>): Bool

    abstract open func keys(): std.collection.EquatableCollection<K>

    abstract open func values(): std.core.Collection<V>

    abstract open operator func [](key: K): V
}

interface OrderedMap<K, V> <: std.collection.Map<K, V> {
    @Annotation
    abstract open prop first: std.core.Option<(K, V)>

    @Annotation
    abstract open prop last: std.core.Option<(K, V)>

    abstract open func removeFirst(): std.core.Option<(K, V)>

    abstract open func removeLast(): std.core.Option<(K, V)>

    abstract open func backward(mark: K, inclusive: Bool): std.core.Iterator<(K, V)>

    abstract open func forward(mark: K, inclusive: Bool): std.core.Iterator<(K, V)>
}

func enumerate<T>(it: std.core.Iterable<T>): std.core.Iterator<(Int64, T)> { /* compiled code */ }

func filterMap<T, R>(transform: (T) -> std.core.Option<R>): std.core.Iterator<R> { /* compiled code */ }

func flatten<T, R>(it: std.core.Iterable<T>): std.core.Iterator<R> { /* compiled code */ }

func flatMap<T, R>(transform: (T) -> std.core.Iterable<R>): std.core.Iterator<R> { /* compiled code */ }

func zip<T, R>(other: std.core.Iterable<R>): std.core.Iterator<(T, R)> { /* compiled code */ }

func skip<T>(count: Int64): std.core.Iterator<T> { /* compiled code */ }

func take<T>(count: Int64): std.core.Iterator<T> { /* compiled code */ }

func step<T>(count: Int64): std.core.Iterator<T> { /* compiled code */ }

func inspect<T>(action: (T) -> Unit): std.core.Iterator<T> { /* compiled code */ }

class FlattenIterator<T, R> <: std.core.Iterator<R> {
    @Annotation
    var subIt: std.core.Option<std.core.Iterator<R>>

    let it: std.core.Iterator<T>

    init(it: std.core.Iterable<T>) { /* compiled code */ }

    @Frozen
    func next(): std.core.Option<R> { /* compiled code */ }
}

class FlatMapIterator<T, R> <: std.core.Iterator<R> {
    @Annotation
    var subIt: std.core.Option<std.core.Iterator<R>>

    let transform: (T) -> std.core.Iterable<R>

    let it: std.core.Iterator<T>

    init(it: std.core.Iterable<T>, transform: (T) -> std.core.Iterable<R>) { /* compiled code */ }

    @Frozen
    func next(): std.core.Option<R> { /* compiled code */ }
}

interface OrderedSet<T> <: std.collection.Set<T> {
    @Annotation
    abstract open prop first: std.core.Option<T>

    @Annotation
    abstract open prop last: std.core.Option<T>

    abstract open func removeFirst(): std.core.Option<T>

    abstract open func removeLast(): std.core.Option<T>

    abstract open func backward(mark: T, inclusive: Bool): std.core.Iterator<T>

    abstract open func forward(mark: T, inclusive: Bool): std.core.Iterator<T>
}

func collectString<T>(delimiter: std.core.String): std.core.String { /* compiled code */ }

class Node<K, V> {
    var entries: std.collection.ArrayList<std.collection.TreeMapEntry<K, V>>

    var _parent: std.core.Option<std.collection.Node<K, V>>

    var children: std.collection.ArrayList<std.collection.Node<K, V>>

    static func nullNode(): std.core.Option<std.collection.Node<K, V>> { /* compiled code */ }

    init(parent: std.core.Option<std.collection.Node<K, V>>) { /* compiled code */ }

    func isLeaf(): Bool { /* compiled code */ }

    @Annotation
    prop entrySize: Int64 {
        get() { /* compiled code */ }
    }

    @Annotation
    prop childrenSize: Int64 {
        get() { /* compiled code */ }
    }

    func quickEquals(that: std.collection.Node<K, V>): Bool { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    func searchEntry(key: K): std.collection.SearchResult<K, V> { /* compiled code */ }

    func getEntry(index: Int64): std.core.Option<std.collection.TreeMapEntry<K, V>> { /* compiled code */ }

    func insertEntry(entry: std.collection.TreeMapEntry<K, V>): std.core.Option<V> { /* compiled code */ }

    func insertEntry(index: Int64, entry: std.collection.TreeMapEntry<K, V>): Unit { /* compiled code */ }

    func deleteEntry(index: Int64): Unit { /* compiled code */ }

    func getChild(index: Int64): std.core.Option<std.collection.Node<K, V>> { /* compiled code */ }

    func deleteChild(index: Int64): Unit { /* compiled code */ }

    func insertChild(index: Int64, node: std.collection.Node<K, V>): Unit { /* compiled code */ }
}

class TreeMapEntry<K, V> {
    var _key: K

    var _value: V

    init(key: K, value: V) { /* compiled code */ }

    @Annotation
    prop key: K {
        get() { /* compiled code */ }
    }

    @Annotation
    mut prop value: V {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func reset(key: K, value: V): Unit { /* compiled code */ }
}

extend std.collection.TreeMap<K, V> <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend std.collection.TreeMap<K, V> <: Equatable {
    operator func ==(right: std.collection.TreeMap<K, V>): Bool { /* compiled code */ }

    operator func !=(right: std.collection.TreeMap<K, V>): Bool { /* compiled code */ }
}

class ForwardIterator<K, V> <: std.core.Iterator<(K, V)> {
    var curNode: std.collection.TreeMapEntryView<K, V>

    var key: K

    var inclusive: Bool

    init(node: std.collection.TreeMapEntryView<K, V>, key: K, inclusive: Bool) { /* compiled code */ }

    @Frozen
    func next(): std.core.Option<(K, V)> { /* compiled code */ }
}

class BackwardIterator<K, V> <: std.core.Iterator<(K, V)> {
    var curNode: std.collection.TreeMapEntryView<K, V>

    var key: K

    var inclusive: Bool

    init(node: std.collection.TreeMapEntryView<K, V>, key: K, inclusive: Bool) { /* compiled code */ }

    @Frozen
    func next(): std.core.Option<(K, V)> { /* compiled code */ }
}

class TreeMapKeys<K, V> <: std.core.Object & std.collection.EquatableCollection<K> {
    let map: std.collection.TreeMap<K, V>

    init(m: std.collection.TreeMap<K, V>) { /* compiled code */ }

    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func contains(element: K): Bool { /* compiled code */ }

    func contains(all: std.core.Collection<K>): Bool { /* compiled code */ }

    @Frozen
    func iterator(): std.core.Iterator<K> { /* compiled code */ }
}

class TreeMapValues<K, V> <: std.core.Object & std.core.Collection<V> {
    let map: std.collection.TreeMap<K, V>

    init(m: std.collection.TreeMap<K, V>) { /* compiled code */ }

    @Annotation
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    func iterator(): std.core.Iterator<V> { /* compiled code */ }
}

class IteratorForSet<T> <: std.core.Iterator<T> {
    let it: std.core.Iterator<(T, Unit)>

    init(it: std.core.Iterator<(T, Unit)>) { /* compiled code */ }

    @Frozen
    func next(): std.core.Option<T> { /* compiled code */ }
}

extend std.collection.TreeSet<T> <: Equatable {
    operator func ==(that: std.collection.TreeSet<T>): Bool { /* compiled code */ }

    operator func !=(that: std.collection.TreeSet<T>): Bool { /* compiled code */ }
}

extend std.collection.TreeSet<T> <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}
