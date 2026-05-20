// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.collection

import std.core.*
import std.math.*

func collectionToString(col: C): std.core.String { /* compiled code */ }

func max(a: Int64, b: Int64): Int64 { /* compiled code */ }

func reduce(operation: (T, T) -> T): (std.core.Iterable<T>) -> std.core.Option<T> { /* compiled code */ }

func filter(predicate: (T) -> Bool): (std.core.Iterable<T>) -> std.core.Iterator<T> { /* compiled code */ }

func map(transform: (T) -> R): (std.core.Iterable<T>) -> std.core.Iterator<R> { /* compiled code */ }

func concat(other: std.core.Iterable<T>): (std.core.Iterable<T>) -> std.core.Iterator<T> { /* compiled code */ }

func collectArray(it: std.core.Iterable<T>): std.core.Array<T> { /* compiled code */ }

class ArrayList <: std.core.Object & std.collection.List<T> {
    var myData: std.core.Array<T>

    var mySize: Int64

    var myVersion: Int64

    let DEFAULT_CAPACITY: Int64

    prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    init(elements: std.core.Array<T>) { /* compiled code */ }

    init(elements: std.core.Collection<T>) { /* compiled code */ }

    static func of(elements: std.core.Array<T>): std.collection.ArrayList<T> { /* compiled code */ }

    func getRawArray(): std.core.Array<T> { /* compiled code */ }

    func toArray(): std.core.Array<T> { /* compiled code */ }

    func `get`(index: Int64): std.core.Option<T> { /* compiled code */ }

    prop first: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    prop last: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    func add(element: T): Unit { /* compiled code */ }

    func add(all: std.core.Collection<T>): Unit { /* compiled code */ }

    func add(element: T, at: Int64): Unit { /* compiled code */ }

    func add(all: std.core.Collection<T>, at: Int64): Unit { /* compiled code */ }

    func remove(at: Int64): T { /* compiled code */ }

    func remove(range: std.core.Range<Int64>): Unit { /* compiled code */ }

    func removeIf(predicate: (T) -> Bool): Unit { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    func reserve(additional: Int64): Unit { /* compiled code */ }

    func sortBy(stable: Bool, comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

    func sortBy(comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

    func reverse(): Unit { /* compiled code */ }

    operator func [](range: std.core.Range<Int64>): std.collection.ArrayList<T> { /* compiled code */ }

    func slice(range: std.core.Range<Int64>): std.collection.ArrayList<T> { /* compiled code */ }

    func rangeSlice(arr: std.core.Array<T>, range: std.core.Range<Int64>, size: Int64): std.core.Array<T> { /* compiled code */ }

    func clone(): std.collection.ArrayList<T> { /* compiled code */ }

    operator func [](index: Int64): T { /* compiled code */ }

    operator func [](index: Int64, value: T): Unit { /* compiled code */ }

    func iterator(): std.core.Iterator<T> { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func checkRange(index: Int64): Unit { /* compiled code */ }

    func checkRangeInsert(index: Int64): Unit { /* compiled code */ }

    func grow(minCapacity: Int64, startIndex: Int64): Unit { /* compiled code */ }

    func updateVersion(): Unit { /* compiled code */ }
}

func collectArrayList(it: std.core.Iterable<T>): std.collection.ArrayList<T> { /* compiled code */ }

func collectHashSet(it: std.core.Iterable<T>): std.collection.HashSet<T> { /* compiled code */ }

class HashSet <: std.core.Object & std.collection.Set<T> {
    var map: std.collection.HashMap<T, Unit>

    init() { /* compiled code */ }

    init(elements: std.core.Collection<T>) { /* compiled code */ }

    init(elements: std.core.Array<T>) { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    func contains(element: T): Bool { /* compiled code */ }

    func subsetOf(other: std.collection.ReadOnlySet<T>): Bool { /* compiled code */ }

    func contains(all: std.core.Collection<T>): Bool { /* compiled code */ }

    func add(element: T): Bool { /* compiled code */ }

    func remove(element: T): Bool { /* compiled code */ }

    func add(all: std.core.Collection<T>): Unit { /* compiled code */ }

    func remove(all: std.core.Collection<T>): Unit { /* compiled code */ }

    func removeIf(predicate: (T) -> Bool): Unit { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    func retain(all: std.collection.Set<T>): Unit { /* compiled code */ }

    func clone(): std.collection.HashSet<T> { /* compiled code */ }

    func reserve(additional: Int64): Unit { /* compiled code */ }

    prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    func iterator(): std.core.Iterator<T> { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func toArray(): std.core.Array<T> { /* compiled code */ }

    operator func &(other: std.collection.ReadOnlySet<T>): std.collection.HashSet<T> { /* compiled code */ }

    operator func |(other: std.collection.ReadOnlySet<T>): std.collection.HashSet<T> { /* compiled code */ }

    operator func -(other: std.collection.ReadOnlySet<T>): std.collection.HashSet<T> { /* compiled code */ }
}

func collectHashMap(it: std.core.Iterable<(K, V)>): std.collection.HashMap<K, V> { /* compiled code */ }

class HashMap <: std.core.Object & std.collection.Map<K, V> {
    let MAX_BUCKET_SIZE: Int64

    let LOAD_FACTOR: Float32

    let DEFAULT_CAPACITY: Int64

    var appendIndex: Int64

    var freeIndex: Int64

    var freeSize: Int64

    var modCount: Int64

    var buckets: std.core.Array<Int64>

    var entries: std.core.Array<std.collection.HashMapEntry<K, V>>

    init() { /* compiled code */ }

    init(elements: std.core.Collection<(K, V)>) { /* compiled code */ }

    init(elements: std.core.Array<(K, V)>) { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> (K, V)) { /* compiled code */ }

    init(other: std.collection.HashMap<K, V>) { /* compiled code */ }

    func `get`(key: K): std.core.Option<V> { /* compiled code */ }

    func add(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func putWithoutResize(key: K, value: V): Unit { /* compiled code */ }

    func entryView(key: K): std.collection.MapEntryView<K, V> { /* compiled code */ }

    func add(all: std.core.Collection<(K, V)>): Unit { /* compiled code */ }

    func remove(key: K): std.core.Option<V> { /* compiled code */ }

    func remove(all: std.core.Collection<K>): Unit { /* compiled code */ }

    func removeIf(predicate: (K, V) -> Bool): Unit { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    func reserve(additional: Int64): Unit { /* compiled code */ }

    prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    func contains(all: std.core.Collection<K>): Bool { /* compiled code */ }

    func contains(key: K): Bool { /* compiled code */ }

    func clone(): std.collection.HashMap<K, V> { /* compiled code */ }

    func keys(): std.collection.EquatableCollection<K> { /* compiled code */ }

    func values(): std.core.Collection<V> { /* compiled code */ }

    operator func [](key: K): V { /* compiled code */ }

    operator func [](key: K, value: V): Unit { /* compiled code */ }

    func getIndex(hash: Int64, size: Int64): Int64 { /* compiled code */ }

    func getHash(hashCode: Int64): Int64 { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func iterator(): std.collection.HashMapIterator<K, V> { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    func version(): Int64 { /* compiled code */ }

    func max(l: Int64, r: Int64): Int64 { /* compiled code */ }

    func toArray(): std.core.Array<(K, V)> { /* compiled code */ }

    func resize(argCap: Int64): Bool { /* compiled code */ }

    static func tableSizeFor(cap: Int64): Int64 { /* compiled code */ }

    func removePosition(hash: Int64, pos: Int64): Unit { /* compiled code */ }
}

let DEGREE: Int64

let MIN_ENTRY_SIZE: Int64

let MAX_ENTRY_SIZE: Int64

let MIN_CHILD_SIZE: Int64

let MAX_CHILD_SIZE: Int64

class ArrayDequeIterator <: std.core.Iterator<T> {
    var myData: std.collection.ArrayDeque<T>

    var myIndex: Int64

    let lockVersion: Int64

    init(data: std.collection.ArrayDeque<T>) { /* compiled code */ }

    func next(): std.core.Option<T> { /* compiled code */ }
}

class ArrayDeque <: std.core.Object & std.collection.Deque<T> {
    var _data: std.core.Array<T>

    var _version: Int64

    var _head: Int64

    var _tail: Int64

    init() { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    prop first: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    prop last: std.core.Option<T> {
        get() { /* compiled code */ }
    }

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

    func iterator(): std.core.Iterator<T> { /* compiled code */ }
}

class ConcurrentModificationException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class ArrayListIterator <: std.core.Iterator<T> {
    var myPosition: Int64

    let myData: std.collection.ArrayList<T>

    let initVersion: Int64

    init(data: std.collection.ArrayList<T>) { /* compiled code */ }

    func next(): std.core.Option<T> { /* compiled code */ }
}

func stableSort(data: std.core.Array<T>, comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

func unstableSort(data: std.core.Array<T>, comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

class ArrayStackIterator <: std.core.Iterator<T> {
    let _stack: std.collection.ArrayStack<T>

    var _position: Int64

    let _lockVersion: Int64

    init(data: std.collection.ArrayStack<T>) { /* compiled code */ }

    func next(): std.core.Option<T> { /* compiled code */ }
}

class ArrayStack <: std.core.Object & std.collection.Stack<T> {
    var _data: std.core.Array<T>

    var _size: Int64

    var _version: Int64

    init(capacity: Int64) { /* compiled code */ }

    init() { /* compiled code */ }

    func peek(): std.core.Option<T> { /* compiled code */ }

    func remove(): std.core.Option<T> { /* compiled code */ }

    func add(element: T): Unit { /* compiled code */ }

    prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func reserve(additional: Int64): Unit { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    func toArray(): std.core.Array<T> { /* compiled code */ }

    func iterator(): std.core.Iterator<T> { /* compiled code */ }
}

struct HashMapEntry {
    let hash: Int64

    let next: Int64

    let key: K

    let value: V

    init(h: Int64, n: Int64, k: K, v: V) { /* compiled code */ }

    init() { /* compiled code */ }
}

class HashMapKeys <: std.core.Object & std.collection.EquatableCollection<K> {
    let map: std.collection.HashMap<K, V>

    init(m: std.collection.HashMap<K, V>) { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func contains(element: K): Bool { /* compiled code */ }

    func contains(all: std.core.Collection<K>): Bool { /* compiled code */ }

    func iterator(): std.core.Iterator<K> { /* compiled code */ }

    func toArray(): std.core.Array<K> { /* compiled code */ }
}

class HashMapIterator <: std.core.Iterator<(K, V)> {
    var lockVersion: Int64

    var index: Int64

    let data: std.collection.HashMap<K, V>

    var isRemoved: Bool

    init(map: std.collection.HashMap<K, V>) { /* compiled code */ }

    func next(): std.core.Option<(K, V)> { /* compiled code */ }

    func remove(): std.core.Option<(K, V)> { /* compiled code */ }
}

class HashMapValues <: std.core.Object & std.core.Collection<V> {
    let map: std.collection.HashMap<K, V>

    init(m: std.collection.HashMap<K, V>) { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func iterator(): std.core.Iterator<V> { /* compiled code */ }

    func toArray(): std.core.Array<V> { /* compiled code */ }
}

class HashMapEntryView <: std.core.Object & std.collection.MapEntryView<K, V> {
    let map: std.collection.HashMap<K, V>

    let _key: K

    let hash: Int64

    var index: Int64

    var _isAbsent: Bool

    var lockVersion: Int64

    init(isAbsent: Bool, key: K, hash: Int64, index: Int64, map: std.collection.HashMap<K, V>) { /* compiled code */ }

    prop key: K {
        get() { /* compiled code */ }
    }

    mut prop value: std.core.Option<V> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func getKey(): K { /* compiled code */ }

    func getValue(): std.core.Option<V> { /* compiled code */ }

    func setValue(v: V): Unit { /* compiled code */ }
}

interface MapEntryView {
    abstract prop key: K

    abstract mut prop value: std.core.Option<V>
}

interface EquatableCollection <: std.core.Collection<T> {
    abstract func contains(element: T): Bool

    abstract func contains(all: std.core.Collection<T>): Bool
}

interface ReadOnlySet <: std.core.Collection<T> {
    abstract func contains(element: T): Bool

    abstract func contains(all: std.core.Collection<T>): Bool

    abstract func subsetOf(other: std.collection.ReadOnlySet<T>): Bool
}

interface Set <: std.collection.ReadOnlySet<T> {
    abstract func add(element: T): Bool

    abstract func add(all: std.core.Collection<T>): Unit

    abstract func remove(element: T): Bool

    abstract func remove(all: std.core.Collection<T>): Unit

    abstract func removeIf(predicate: (T) -> Bool): Unit

    abstract func clear(): Unit

    abstract func retain(all: std.collection.Set<T>): Unit
}

class LinkedListNode {
    var _next: std.core.Option<std.collection.LinkedListNode<T>>

    var _prev: std.core.Option<std.collection.LinkedListNode<T>>

    var _item: T

    var _list: std.core.Option<std.collection.LinkedList<T>>

    init(element: T, prev: std.core.Option<std.collection.LinkedListNode<T>>, next: std.core.Option<std.collection.LinkedListNode<T>>, belongs: std.core.Option<std.collection.LinkedList<T>>) { /* compiled code */ }

    mut prop value: T {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    prop next: std.core.Option<std.collection.LinkedListNode<T>> {
        get() { /* compiled code */ }
    }

    prop prev: std.core.Option<std.collection.LinkedListNode<T>> {
        get() { /* compiled code */ }
    }

    func insertBefore(element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    func insertAfter(element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    func remove(): T { /* compiled code */ }

    func reverse(): Unit { /* compiled code */ }

    func checkNodeState(): Unit { /* compiled code */ }
}

class LinkedList <: std.core.Object & std.core.Collection<T> {
    var _size: Int64

    var _head: std.core.Option<std.collection.LinkedListNode<T>>

    var _tail: std.core.Option<std.collection.LinkedListNode<T>>

    var _version: Int64

    init() { /* compiled code */ }

    init(elements: std.core.Collection<T>) { /* compiled code */ }

    init(elements: std.core.Array<T>) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    prop first: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    prop last: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    prop firstNode: std.core.Option<std.collection.LinkedListNode<T>> {
        get() { /* compiled code */ }
    }

    prop lastNode: std.core.Option<std.collection.LinkedListNode<T>> {
        get() { /* compiled code */ }
    }

    prop version: Int64 {
        get() { /* compiled code */ }
    }

    func addFirst(element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    func addLast(element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    func toArray(): std.core.Array<T> { /* compiled code */ }

    func addBefore(node: std.collection.LinkedListNode<T>, element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    func addAfter(node: std.collection.LinkedListNode<T>, element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    func remove(node: std.collection.LinkedListNode<T>): T { /* compiled code */ }

    func removeIf(predicate: (T) -> Bool): Unit { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    func reverse(): Unit { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    func iterator(): std.core.Iterator<T> { /* compiled code */ }

    func removeFirst(): std.core.Option<T> { /* compiled code */ }

    func removeLast(): std.core.Option<T> { /* compiled code */ }

    func nodeAt(index: Int64): std.core.Option<std.collection.LinkedListNode<T>> { /* compiled code */ }

    func splitOff(node: std.collection.LinkedListNode<T>): std.collection.LinkedList<T> { /* compiled code */ }

    func forward(mark: std.collection.LinkedListNode<T>): std.core.Iterator<T> { /* compiled code */ }

    func backward(mark: std.collection.LinkedListNode<T>): std.core.Iterator<T> { /* compiled code */ }

    func checkNode(node: std.collection.LinkedListNode<T>): Unit { /* compiled code */ }
}

class LinkedListIterator <: std.core.Iterator<T> {
    let _lockVersion: Int64

    let _data: std.collection.LinkedList<T>

    var _currentNode: std.collection.LinkedListNode<T>

    let _backward: Bool

    init(data: std.collection.LinkedList<T>, node: std.core.Option<std.collection.LinkedListNode<T>>, backward: Bool) { /* compiled code */ }

    func next(): std.core.Option<T> { /* compiled code */ }
}

interface Map <: std.collection.ReadOnlyMap<K, V> {
    abstract func add(key: K, value: V): std.core.Option<V>

    abstract func add(all: std.core.Collection<(K, V)>): Unit

    abstract func remove(key: K): std.core.Option<V>

    abstract func remove(all: std.core.Collection<K>): Unit

    abstract func removeIf(predicate: (K, V) -> Bool): Unit

    abstract func clear(): Unit

    abstract operator func [](key: K, value: V): Unit

    abstract func entryView(k: K): std.collection.MapEntryView<K, V>

    func addIfAbsent(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    func replace(key: K, value: V): std.core.Option<V> { /* compiled code */ }
}

struct TreeMapEntryView <: std.collection.MapEntryView<K, V> {
    let map: std.collection.TreeMap<K, V>

    var curNode: std.collection.SearchResult<K, V>

    var lockVersion: Int64

    var _key: std.core.Option<K>

    init(map: std.collection.TreeMap<K, V>, curNode: std.collection.SearchResult<K, V>, key: std.core.Option<K>) { /* compiled code */ }

    prop key: K {
        get() { /* compiled code */ }
    }

    mut prop value: std.core.Option<V> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func nextNode(): std.collection.TreeMapEntryView<K, V> { /* compiled code */ }

    func prevNode(): std.collection.TreeMapEntryView<K, V> { /* compiled code */ }

    func isSome(): Bool { /* compiled code */ }
}

class TreeMap <: std.core.Object & std.collection.OrderedMap<K, V> {
    var _size: Int64

    var modCount: Int64

    var root: std.collection.Node<K, V>

    init() { /* compiled code */ }

    init(elements: std.core.Collection<(K, V)>) { /* compiled code */ }

    init(elements: std.core.Array<(K, V)>) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> (K, V)) { /* compiled code */ }

    func `get`(key: K): std.core.Option<V> { /* compiled code */ }

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

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func iterator(): std.core.Iterator<(K, V)> { /* compiled code */ }

    prop first: std.core.Option<(K, V)> {
        get() { /* compiled code */ }
    }

    func removeFirst(): std.core.Option<(K, V)> { /* compiled code */ }

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

struct SearchResult {
    var _index: Int64

    var _replaceNode: std.core.Option<std.collection.Node<K, V>>

    init(index: Int64, node: std.core.Option<std.collection.Node<K, V>>) { /* compiled code */ }

    prop index: Int64 {
        get() { /* compiled code */ }
    }

    prop replaceNode: std.core.Option<std.collection.Node<K, V>> {
        get() { /* compiled code */ }
    }
}

class TreeMapIterator <: std.core.Iterator<(K, V)> {
    var curNode: std.collection.TreeMapEntryView<K, V>

    init(node: std.collection.TreeMapEntryView<K, V>) { /* compiled code */ }

    func next(): std.core.Option<(K, V)> { /* compiled code */ }
}

func checkVersion(node: std.collection.TreeMapEntryView<K, V>): Unit { /* compiled code */ }

class TreeSet <: std.core.Object & std.collection.OrderedSet<T> {
    var map: std.collection.TreeMap<T, Unit>

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    prop first: std.core.Option<T> {
        get() { /* compiled code */ }
    }

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

interface Deque <: std.core.Collection<T> {
    abstract prop first: std.core.Option<T>

    abstract prop last: std.core.Option<T>

    abstract func addFirst(element: T): Unit

    abstract func addLast(element: T): Unit

    abstract func removeFirst(): std.core.Option<T>

    abstract func removeLast(): std.core.Option<T>
}

extend ArrayDeque <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

interface List <: std.collection.ReadOnlyList<T> {
    abstract func add(element: T): Unit

    abstract func add(all: std.core.Collection<T>): Unit

    abstract func add(element: T, at: Int64): Unit

    abstract func add(all: std.core.Collection<T>, at: Int64): Unit

    abstract func remove(at: Int64): T

    abstract func remove(range: std.core.Range<Int64>): Unit

    abstract func removeIf(predicate: (T) -> Bool): Unit

    abstract func clear(): Unit

    abstract operator func [](index: Int64, value: T): Unit
}

extend ArrayList <: Equatable {
    operator func ==(that: std.collection.ArrayList<T>): Bool { /* compiled code */ }

    operator func !=(that: std.collection.ArrayList<T>): Bool { /* compiled code */ }

    func contains(element: T): Bool { /* compiled code */ }
}

extend ArrayList {
    func sort(stable: Bool): Unit { /* compiled code */ }

    func sort(): Unit { /* compiled code */ }

    func sortDescending(stable: Bool): Unit { /* compiled code */ }

    func sortDescending(): Unit { /* compiled code */ }
}

extend ArrayList <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

class TempArray {
    var data: std.core.Array<T>

    let zero: T

    let max: Int64

    init(n: Int64, z: T) { /* compiled code */ }

    func getArray(n: Int64): std.core.Array<T> { /* compiled code */ }
}

class ArrayQueue <: std.core.Object & std.collection.Queue<T> {
    let _data: std.collection.ArrayDeque<T>

    init() { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    func peek(): std.core.Option<T> { /* compiled code */ }

    func add(element: T): Unit { /* compiled code */ }

    func remove(): std.core.Option<T> { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    func iterator(): std.core.Iterator<T> { /* compiled code */ }

    func toArray(): std.core.Array<T> { /* compiled code */ }

    func reserve(additional: Int64): Unit { /* compiled code */ }
}

interface Queue <: std.core.Collection<T> {
    abstract func add(element: T): Unit

    abstract func peek(): std.core.Option<T>

    abstract func remove(): std.core.Option<T>
}

extend ArrayQueue <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

interface Stack <: std.core.Collection<T> {
    abstract func peek(): std.core.Option<T>

    abstract func remove(): std.core.Option<T>

    abstract func add(element: T): Unit
}

extend ArrayStack <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

func fold(initial: R, operation: (R, T) -> R): (std.core.Iterable<T>) -> R { /* compiled code */ }

func isEmpty(it: std.core.Iterable<T>): Bool { /* compiled code */ }

func count(it: std.core.Iterable<T>): Int64 { /* compiled code */ }

func contains(element: T): (std.core.Iterable<T>) -> Bool { /* compiled code */ }

func max(it: std.core.Iterable<T>): std.core.Option<T> { /* compiled code */ }

func min(it: std.core.Iterable<T>): std.core.Option<T> { /* compiled code */ }

func all(predicate: (T) -> Bool): (std.core.Iterable<T>) -> Bool { /* compiled code */ }

func any(predicate: (T) -> Bool): (std.core.Iterable<T>) -> Bool { /* compiled code */ }

func none(predicate: (T) -> Bool): (std.core.Iterable<T>) -> Bool { /* compiled code */ }

func first(it: std.core.Iterable<T>): std.core.Option<T> { /* compiled code */ }

func last(it: std.core.Iterable<T>): std.core.Option<T> { /* compiled code */ }

func at(n: Int64): (std.core.Iterable<T>) -> std.core.Option<T> { /* compiled code */ }

func forEach(action: (T) -> Unit): (std.core.Iterable<T>) -> Unit { /* compiled code */ }

extend HashMap <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend HashMap <: Equatable {
    operator func ==(right: std.collection.HashMap<K, V>): Bool { /* compiled code */ }

    operator func !=(right: std.collection.HashMap<K, V>): Bool { /* compiled code */ }
}

extend HashSet <: Equatable {
    operator func ==(that: std.collection.HashSet<T>): Bool { /* compiled code */ }

    operator func !=(that: std.collection.HashSet<T>): Bool { /* compiled code */ }
}

extend HashSet <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend LinkedList <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend LinkedList <: Equatable {
    operator func ==(right: std.collection.LinkedList<T>): Bool { /* compiled code */ }

    operator func !=(right: std.collection.LinkedList<T>): Bool { /* compiled code */ }
}

interface ReadOnlyList <: std.core.Collection<T> {
    abstract prop first: std.core.Option<T>

    abstract prop last: std.core.Option<T>

    abstract func `get`(index: Int64): std.core.Option<T>

    abstract operator func [](index: Int64): T
}

interface ReadOnlyMap <: std.core.Collection<(K, V)> {
    abstract func `get`(key: K): std.core.Option<V>

    abstract func contains(key: K): Bool

    abstract func contains(all: std.core.Collection<K>): Bool

    abstract func keys(): std.collection.EquatableCollection<K>

    abstract func values(): std.core.Collection<V>

    abstract operator func [](key: K): V
}

interface OrderedMap <: std.collection.Map<K, V> {
    abstract prop first: std.core.Option<(K, V)>

    abstract prop last: std.core.Option<(K, V)>

    abstract func removeFirst(): std.core.Option<(K, V)>

    abstract func removeLast(): std.core.Option<(K, V)>

    abstract func backward(mark: K, inclusive: Bool): std.core.Iterator<(K, V)>

    abstract func forward(mark: K, inclusive: Bool): std.core.Iterator<(K, V)>
}

func enumerate(it: std.core.Iterable<T>): std.core.Iterator<(Int64, T)> { /* compiled code */ }

func filterMap(transform: (T) -> std.core.Option<R>): (std.core.Iterable<T>) -> std.core.Iterator<R> { /* compiled code */ }

func flatten(it: std.core.Iterable<T>): std.core.Iterator<R> { /* compiled code */ }

func flatMap(transform: (T) -> std.core.Iterable<R>): (std.core.Iterable<T>) -> std.core.Iterator<R> { /* compiled code */ }

func zip(other: std.core.Iterable<R>): (std.core.Iterable<T>) -> std.core.Iterator<(T, R)> { /* compiled code */ }

func skip(count: Int64): (std.core.Iterable<T>) -> std.core.Iterator<T> { /* compiled code */ }

func take(count: Int64): (std.core.Iterable<T>) -> std.core.Iterator<T> { /* compiled code */ }

func step(count: Int64): (std.core.Iterable<T>) -> std.core.Iterator<T> { /* compiled code */ }

func inspect(action: (T) -> Unit): (std.core.Iterable<T>) -> std.core.Iterator<T> { /* compiled code */ }

class FlattenIterator <: std.core.Iterator<R> {
    var subIt: std.core.Option<std.core.Iterator<R>>

    let it: std.core.Iterator<T>

    init(it: std.core.Iterable<T>) { /* compiled code */ }

    func next(): std.core.Option<R> { /* compiled code */ }
}

class FlatMapIterator <: std.core.Iterator<R> {
    var subIt: std.core.Option<std.core.Iterator<R>>

    let transform: (T) -> std.core.Iterable<R>

    let it: std.core.Iterator<T>

    init(it: std.core.Iterable<T>, transform: (T) -> std.core.Iterable<R>) { /* compiled code */ }

    func next(): std.core.Option<R> { /* compiled code */ }
}

interface OrderedSet <: std.collection.Set<T> {
    abstract prop first: std.core.Option<T>

    abstract prop last: std.core.Option<T>

    abstract func removeFirst(): std.core.Option<T>

    abstract func removeLast(): std.core.Option<T>

    abstract func backward(mark: T, inclusive: Bool): std.core.Iterator<T>

    abstract func forward(mark: T, inclusive: Bool): std.core.Iterator<T>
}

func collectString(delimiter: std.core.String): (std.core.Iterable<T>) -> std.core.String { /* compiled code */ }

class Node {
    var entries: std.collection.ArrayList<std.collection.TreeMapEntry<K, V>>

    var _parent: std.core.Option<std.collection.Node<K, V>>

    var children: std.collection.ArrayList<std.collection.Node<K, V>>

    static func nullNode(): std.core.Option<std.collection.Node<K, V>> { /* compiled code */ }

    init(parent: std.core.Option<std.collection.Node<K, V>>) { /* compiled code */ }

    func isLeaf(): Bool { /* compiled code */ }

    prop entrySize: Int64 {
        get() { /* compiled code */ }
    }

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

class TreeMapEntry {
    var _key: K

    var _value: V

    init(key: K, value: V) { /* compiled code */ }

    prop key: K {
        get() { /* compiled code */ }
    }

    mut prop value: V {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func reset(key: K, value: V): Unit { /* compiled code */ }
}

extend TreeMap <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend TreeMap <: Equatable {
    operator func ==(right: std.collection.TreeMap<K, V>): Bool { /* compiled code */ }

    operator func !=(right: std.collection.TreeMap<K, V>): Bool { /* compiled code */ }
}

class ForwardIterator <: std.core.Iterator<(K, V)> {
    var curNode: std.collection.TreeMapEntryView<K, V>

    var key: K

    var inclusive: Bool

    init(node: std.collection.TreeMapEntryView<K, V>, key: K, inclusive: Bool) { /* compiled code */ }

    func next(): std.core.Option<(K, V)> { /* compiled code */ }
}

class BackwardIterator <: std.core.Iterator<(K, V)> {
    var curNode: std.collection.TreeMapEntryView<K, V>

    var key: K

    var inclusive: Bool

    init(node: std.collection.TreeMapEntryView<K, V>, key: K, inclusive: Bool) { /* compiled code */ }

    func next(): std.core.Option<(K, V)> { /* compiled code */ }
}

class TreeMapKeys <: std.core.Object & std.collection.EquatableCollection<K> {
    let map: std.collection.TreeMap<K, V>

    init(m: std.collection.TreeMap<K, V>) { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func contains(element: K): Bool { /* compiled code */ }

    func contains(all: std.core.Collection<K>): Bool { /* compiled code */ }

    func iterator(): std.core.Iterator<K> { /* compiled code */ }
}

class TreeMapValues <: std.core.Object & std.core.Collection<V> {
    let map: std.collection.TreeMap<K, V>

    init(m: std.collection.TreeMap<K, V>) { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func isEmpty(): Bool { /* compiled code */ }

    func iterator(): std.core.Iterator<V> { /* compiled code */ }
}

class IteratorForSet <: std.core.Iterator<T> {
    let it: std.core.Iterator<(T, Unit)>

    init(it: std.core.Iterator<(T, Unit)>) { /* compiled code */ }

    func next(): std.core.Option<T> { /* compiled code */ }
}

extend TreeSet <: Equatable {
    operator func ==(that: std.collection.TreeSet<T>): Bool { /* compiled code */ }

    operator func !=(that: std.collection.TreeSet<T>): Bool { /* compiled code */ }
}

extend TreeSet <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}
