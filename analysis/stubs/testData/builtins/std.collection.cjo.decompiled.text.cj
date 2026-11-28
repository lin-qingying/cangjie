// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.collection

import std.core.*
import std.math.*

@Frozen
internal func collectionToString<C, T>(col: C): std.core.String { /* compiled code */ }

@Frozen
internal func max(a: Int64, b: Int64): Int64 { /* compiled code */ }

@Frozen
public func reduce<T>(operation: (T, T) -> T): std.core.Option<T> { /* compiled code */ }

@Frozen
public func filter<T>(predicate: (T) -> Bool): std.core.Iterator<T> { /* compiled code */ }

@Frozen
public func map<T, R>(transform: (T) -> R): std.core.Iterator<R> { /* compiled code */ }

@Frozen
public func concat<T>(other: std.core.Iterable<T>): std.core.Iterator<T> { /* compiled code */ }

@Frozen
public func collectArray<T>(it: std.core.Iterable<T>): std.core.Array<T> { /* compiled code */ }

public class ArrayList<T> <: std.core.Object & std.collection.List<T> {
    internal var myData: std.core.Array<T>

    internal var mySize: Int64

    internal var myVersion: Int64 = COMPILED_CODE /* compiled code */

    private static const DEFAULT_CAPACITY: Int64 = COMPILED_CODE /* compiled code */

    @Frozen
    public prop capacity: Int64 {
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
    public static func of(elements: std.core.Array<T>): std.collection.ArrayList<T> { /* compiled code */ }

    @Frozen
    public func getRawArray(): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public func toArray(): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public func get(index: Int64): std.core.Option<T> { /* compiled code */ }

    @Frozen
    public prop first: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    @Frozen
    public prop last: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    @Frozen
    public func add(element: T): Unit { /* compiled code */ }

    @Frozen
    public func add(all!: std.core.Collection<T>): Unit { /* compiled code */ }

    @Frozen
    public func add(element: T, at!: Int64): Unit { /* compiled code */ }

    @Frozen
    public func add(all!: std.core.Collection<T>, at!: Int64): Unit { /* compiled code */ }

    @Frozen
    public func remove(at!: Int64): T { /* compiled code */ }

    @Frozen
    public func remove(range: std.core.Range<Int64>): Unit { /* compiled code */ }

    @Frozen
    public func removeIf(predicate: (T) -> Bool): Unit { /* compiled code */ }

    @Frozen
    public func clear(): Unit { /* compiled code */ }

    @Frozen
    public func reserve(additional: Int64): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use global function `public func sort<T>(data: ArrayList<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit` in std.sort instead."]
    public func sortBy(stable!: Bool, comparator!: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use global function `public func sort<T>(data: ArrayList<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit` in std.sort instead."]
    public func sortBy(comparator!: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

    @Frozen
    public func reverse(): Unit { /* compiled code */ }

    @Frozen
    public operator func [](range: std.core.Range<Int64>): std.collection.ArrayList<T> { /* compiled code */ }

    @Frozen
    public func slice(range: std.core.Range<Int64>): std.collection.ArrayList<T> { /* compiled code */ }

    @Frozen
    private func rangeSlice(arr: std.core.Array<T>, range: std.core.Range<Int64>, size: Int64): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public func clone(): std.collection.ArrayList<T> { /* compiled code */ }

    @Frozen
    public operator func [](index: Int64): T { /* compiled code */ }

    @Frozen
    public operator func [](index: Int64, value!: T): Unit { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<T> { /* compiled code */ }

    @Frozen
    public func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    private func checkRange(index: Int64): Unit { /* compiled code */ }

    @Frozen
    private func checkRangeInsert(index: Int64): Unit { /* compiled code */ }

    @Frozen
    private func grow(minCapacity: Int64, startIndex!: Int64 = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    protected func updateVersion(): Unit { /* compiled code */ }
}

@Frozen
public func collectArrayList<T>(it: std.core.Iterable<T>): std.collection.ArrayList<T> { /* compiled code */ }

@Frozen
public func collectHashSet<T>(it: std.core.Iterable<T>): std.collection.HashSet<T> { /* compiled code */ }

public class HashSet<T> <: std.core.Object & std.collection.Set<T> {
    private var map: std.collection.HashMap<T, Unit>

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
    public func contains(element: T): Bool { /* compiled code */ }

    @Frozen
    public func subsetOf(other: std.collection.ReadOnlySet<T>): Bool { /* compiled code */ }

    @Frozen
    public func contains(all!: std.core.Collection<T>): Bool { /* compiled code */ }

    @Frozen
    public func add(element: T): Bool { /* compiled code */ }

    @Frozen
    public func remove(element: T): Bool { /* compiled code */ }

    @Frozen
    public func add(all!: std.core.Collection<T>): Unit { /* compiled code */ }

    @Frozen
    public func remove(all!: std.core.Collection<T>): Unit { /* compiled code */ }

    @Frozen
    public func removeIf(predicate: (T) -> Bool): Unit { /* compiled code */ }

    @Frozen
    public func clear(): Unit { /* compiled code */ }

    @Frozen
    public func retain(all!: std.collection.Set<T>): Unit { /* compiled code */ }

    @Frozen
    public func clone(): std.collection.HashSet<T> { /* compiled code */ }

    @Frozen
    public func reserve(additional: Int64): Unit { /* compiled code */ }

    @Frozen
    public prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    public func iterator(): std.core.Iterator<T> { /* compiled code */ }

    @Frozen
    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    public func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    public func toArray(): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public operator func &(other: std.collection.ReadOnlySet<T>): std.collection.HashSet<T> { /* compiled code */ }

    @Frozen
    public operator func |(other: std.collection.ReadOnlySet<T>): std.collection.HashSet<T> { /* compiled code */ }

    @Frozen
    public operator func -(other: std.collection.ReadOnlySet<T>): std.collection.HashSet<T> { /* compiled code */ }
}

@Frozen
public func collectHashMap<K, V>(it: std.core.Iterable<(K, V)>): std.collection.HashMap<K, V> { /* compiled code */ }

public class HashMap<K, V> <: std.core.Object & std.collection.Map<K, V> {
    private static const MAX_BUCKET_SIZE: Int64 = COMPILED_CODE /* compiled code */

    private static const LOAD_FACTOR: Float32 = COMPILED_CODE /* compiled code */

    private static const DEFAULT_CAPACITY: Int64 = COMPILED_CODE /* compiled code */

    internal var appendIndex: Int64 = COMPILED_CODE /* compiled code */

    internal var freeIndex: Int64 = COMPILED_CODE /* compiled code */

    internal var freeSize: Int64 = COMPILED_CODE /* compiled code */

    internal var modCount: Int64 = COMPILED_CODE /* compiled code */

    internal var buckets: std.core.Array<Int64>

    internal var entries: std.core.Array<std.collection.HashMapEntry<K, V>>

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
    public func get(key: K): std.core.Option<V> { /* compiled code */ }

    @Frozen
    public func add(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    @Frozen
    protected func putWithoutResize(key: K, value: V): Unit { /* compiled code */ }

    @Frozen
    public func entryView(key: K): std.collection.MapEntryView<K, V> { /* compiled code */ }

    @Frozen
    public func add(all!: std.core.Collection<(K, V)>): Unit { /* compiled code */ }

    @Frozen
    public func remove(key: K): std.core.Option<V> { /* compiled code */ }

    @Frozen
    public func remove(all!: std.core.Collection<K>): Unit { /* compiled code */ }

    @Frozen
    public func removeIf(predicate: (K, V) -> Bool): Unit { /* compiled code */ }

    @Frozen
    public func clear(): Unit { /* compiled code */ }

    @Frozen
    public func reserve(additional: Int64): Unit { /* compiled code */ }

    @Frozen
    public prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    public func contains(all!: std.core.Collection<K>): Bool { /* compiled code */ }

    @Frozen
    public func contains(key: K): Bool { /* compiled code */ }

    @Frozen
    public func clone(): std.collection.HashMap<K, V> { /* compiled code */ }

    @Frozen
    public func keys(): std.collection.EquatableCollection<K> { /* compiled code */ }

    @Frozen
    public func values(): std.core.Collection<V> { /* compiled code */ }

    @Frozen
    public operator func [](key: K): V { /* compiled code */ }

    @Frozen
    public operator func [](key: K, value!: V): Unit { /* compiled code */ }

    @Frozen
    internal func getIndex(hash: Int64, size: Int64): Int64 { /* compiled code */ }

    @Frozen
    private func getHash(hashCode: Int64): Int64 { /* compiled code */ }

    @Frozen
    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    public func iterator(): std.collection.HashMapIterator<K, V> { /* compiled code */ }

    @Frozen
    public func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    internal func version(): Int64 { /* compiled code */ }

    @Frozen
    private func max(l: Int64, r: Int64): Int64 { /* compiled code */ }

    @Frozen
    public func toArray(): std.core.Array<(K, V)> { /* compiled code */ }

    @Frozen
    internal func resize(argCap: Int64): Bool { /* compiled code */ }

    @Frozen
    private static func tableSizeFor(cap: Int64): Int64 { /* compiled code */ }

    @Frozen
    internal func removePosition(hash: Int64, pos: Int64): Unit { /* compiled code */ }
}

let DEGREE: Int64 = COMPILED_CODE /* compiled code */

let MIN_ENTRY_SIZE: Int64 = COMPILED_CODE /* compiled code */

let MAX_ENTRY_SIZE: Int64 = COMPILED_CODE /* compiled code */

let MIN_CHILD_SIZE: Int64 = COMPILED_CODE /* compiled code */

let MAX_CHILD_SIZE: Int64 = COMPILED_CODE /* compiled code */

private class ArrayDequeIterator<T> <: std.core.Iterator<T> {
    private var myData: std.collection.ArrayDeque<T>

    private var myIndex: Int64

    private let lockVersion: Int64

    init(data: std.collection.ArrayDeque<T>) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<T> { /* compiled code */ }
}

public class ArrayDeque<T> <: std.core.Object & std.collection.Deque<T> {
    internal var _data: std.core.Array<T>

    protected var _version: Int64 = COMPILED_CODE /* compiled code */

    internal var _head: Int64 = COMPILED_CODE /* compiled code */

    internal var _tail: Int64 = COMPILED_CODE /* compiled code */

    init() { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    public prop first: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    public prop last: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    public prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    public func isEmpty(): Bool { /* compiled code */ }

    public func addFirst(element: T): Unit { /* compiled code */ }

    public func addLast(element: T): Unit { /* compiled code */ }

    public func removeFirst(): std.core.Option<T> { /* compiled code */ }

    public func removeLast(): std.core.Option<T> { /* compiled code */ }

    public func toArray(): std.core.Array<T> { /* compiled code */ }

    public func clear(): Unit { /* compiled code */ }

    public func reserve(additional: Int64): Unit { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<T> { /* compiled code */ }
}

public class ConcurrentModificationException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

internal class ArrayListIterator<T> <: std.core.Iterator<T> {
    private var myPosition: Int64 = COMPILED_CODE /* compiled code */

    private let myData: std.collection.ArrayList<T>

    private let initVersion: Int64

    @Frozen
    init(data: std.collection.ArrayList<T>) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<T> { /* compiled code */ }
}

internal func stableSort<T>(data: std.core.Array<T>, comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

internal func unstableSort<T>(data: std.core.Array<T>, comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

internal class ArrayStackIterator<T> <: std.core.Iterator<T> {
    private let _stack: std.collection.ArrayStack<T>

    private var _position: Int64

    private let _lockVersion: Int64

    init(data: std.collection.ArrayStack<T>) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<T> { /* compiled code */ }
}

public class ArrayStack<T> <: std.core.Object & std.collection.Stack<T> {
    internal var _data: std.core.Array<T> = COMPILED_CODE /* compiled code */

    internal var _size: Int64 = COMPILED_CODE /* compiled code */

    internal var _version: Int64 = COMPILED_CODE /* compiled code */

    init(capacity: Int64) { /* compiled code */ }

    init() { /* compiled code */ }

    public func peek(): std.core.Option<T> { /* compiled code */ }

    public func remove(): std.core.Option<T> { /* compiled code */ }

    public func add(element: T): Unit { /* compiled code */ }

    public prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    public func isEmpty(): Bool { /* compiled code */ }

    public func reserve(additional: Int64): Unit { /* compiled code */ }

    public func clear(): Unit { /* compiled code */ }

    public func toArray(): std.core.Array<T> { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<T> { /* compiled code */ }
}

internal struct HashMapEntry<K, V> {
    public let hash: Int64

    public let next: Int64

    public let key: K

    public let value: V

    @Frozen
    init(h: Int64, n: Int64, k: K, v: V) { /* compiled code */ }

    @Frozen
    init() { /* compiled code */ }
}

internal class HashMapKeys<K, V> <: std.core.Object & std.collection.EquatableCollection<K> {
    private let map: std.collection.HashMap<K, V>

    @Frozen
    init(m: std.collection.HashMap<K, V>) { /* compiled code */ }

    @Frozen
    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    public func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    public func contains(element: K): Bool { /* compiled code */ }

    @Frozen
    public func contains(all!: std.core.Collection<K>): Bool { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<K> { /* compiled code */ }

    @Frozen
    public func toArray(): std.core.Array<K> { /* compiled code */ }
}

public class HashMapIterator<K, V> <: std.core.Iterator<(K, V)> {
    private var lockVersion: Int64

    private var index: Int64

    private let data: std.collection.HashMap<K, V>

    private var isRemoved: Bool

    @Frozen
    init(map: std.collection.HashMap<K, V>) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<(K, V)> { /* compiled code */ }

    @Frozen
    public func remove(): std.core.Option<(K, V)> { /* compiled code */ }
}

internal class HashMapValues<K, V> <: std.core.Object & std.core.Collection<V> {
    private let map: std.collection.HashMap<K, V>

    @Frozen
    init(m: std.collection.HashMap<K, V>) { /* compiled code */ }

    @Frozen
    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    public func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<V> { /* compiled code */ }

    @Frozen
    public func toArray(): std.core.Array<V> { /* compiled code */ }
}

internal class HashMapEntryView<K, V> <: std.core.Object & std.collection.MapEntryView<K, V> {
    private let map: std.collection.HashMap<K, V>

    private let _key: K

    private let hash: Int64

    private var index: Int64

    private var _isAbsent: Bool

    private var lockVersion: Int64

    @Frozen
    init(isAbsent: Bool, key: K, hash: Int64, index: Int64, map: std.collection.HashMap<K, V>) { /* compiled code */ }

    @Frozen
    public prop key: K {
        get() { /* compiled code */ }
    }

    @Frozen
    public mut prop value: std.core.Option<V> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Frozen
    internal func getKey(): K { /* compiled code */ }

    @Frozen
    internal func getValue(): std.core.Option<V> { /* compiled code */ }

    @Frozen
    private func setValue(v: V): Unit { /* compiled code */ }
}

public interface MapEntryView<K, V> {
    public open prop key: K

    public open mut prop value: std.core.Option<V>
}

public interface EquatableCollection<T> <: std.core.Collection<T> {
    public open func contains(element: T): Bool

    public open func contains(all!: std.core.Collection<T>): Bool
}

public interface ReadOnlySet<T> <: std.core.Collection<T> {
    public open func contains(element: T): Bool

    public open func contains(all!: std.core.Collection<T>): Bool

    public open func subsetOf(other: std.collection.ReadOnlySet<T>): Bool
}

public interface Set<T> <: std.collection.ReadOnlySet<T> {
    public open func add(element: T): Bool

    public open func add(all!: std.core.Collection<T>): Unit

    public open func remove(element: T): Bool

    public open func remove(all!: std.core.Collection<T>): Unit

    public open func removeIf(predicate: (T) -> Bool): Unit

    public open func clear(): Unit

    public open func retain(all!: std.collection.Set<T>): Unit
}

public class LinkedListNode<T> {
    internal var _next: std.core.Option<std.collection.LinkedListNode<T>>

    internal var _prev: std.core.Option<std.collection.LinkedListNode<T>>

    private var _item: T

    protected var _list: std.core.Option<std.collection.LinkedList<T>>

    init(element: T, prev: std.core.Option<std.collection.LinkedListNode<T>>, next: std.core.Option<std.collection.LinkedListNode<T>>, belongs: std.core.Option<std.collection.LinkedList<T>>) { /* compiled code */ }

    @Frozen
    public mut prop value: T {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    @Frozen
    public prop next: std.core.Option<std.collection.LinkedListNode<T>> {
        get() { /* compiled code */ }
    }

    @Frozen
    public prop prev: std.core.Option<std.collection.LinkedListNode<T>> {
        get() { /* compiled code */ }
    }

    @Frozen
    internal func insertBefore(element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    @Frozen
    internal func insertAfter(element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    @Frozen
    internal func remove(): T { /* compiled code */ }

    internal func reverse(): Unit { /* compiled code */ }

    private func checkNodeState(): Unit { /* compiled code */ }
}

public class LinkedList<T> <: std.core.Object & std.core.Collection<T> {
    internal var _size: Int64 = COMPILED_CODE /* compiled code */

    internal var _head: std.core.Option<std.collection.LinkedListNode<T>> = COMPILED_CODE /* compiled code */

    internal var _tail: std.core.Option<std.collection.LinkedListNode<T>> = COMPILED_CODE /* compiled code */

    internal var _version: Int64 = COMPILED_CODE /* compiled code */

    init() { /* compiled code */ }

    init(elements: std.core.Collection<T>) { /* compiled code */ }

    init(elements: std.core.Array<T>) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    public prop first: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    public prop last: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    public prop firstNode: std.core.Option<std.collection.LinkedListNode<T>> {
        get() { /* compiled code */ }
    }

    public prop lastNode: std.core.Option<std.collection.LinkedListNode<T>> {
        get() { /* compiled code */ }
    }

    internal prop version: Int64 {
        get() { /* compiled code */ }
    }

    @Frozen
    public func addFirst(element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    @Frozen
    public func addLast(element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    public func toArray(): std.core.Array<T> { /* compiled code */ }

    public func addBefore(node: std.collection.LinkedListNode<T>, element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    public func addAfter(node: std.collection.LinkedListNode<T>, element: T): std.collection.LinkedListNode<T> { /* compiled code */ }

    @Frozen
    public func remove(node: std.collection.LinkedListNode<T>): T { /* compiled code */ }

    public func removeIf(predicate: (T) -> Bool): Unit { /* compiled code */ }

    public func clear(): Unit { /* compiled code */ }

    public func reverse(): Unit { /* compiled code */ }

    public func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<T> { /* compiled code */ }

    public func removeFirst(): std.core.Option<T> { /* compiled code */ }

    public func removeLast(): std.core.Option<T> { /* compiled code */ }

    public func nodeAt(index: Int64): std.core.Option<std.collection.LinkedListNode<T>> { /* compiled code */ }

    public func splitOff(node: std.collection.LinkedListNode<T>): std.collection.LinkedList<T> { /* compiled code */ }

    public func forward(mark: std.collection.LinkedListNode<T>): std.core.Iterator<T> { /* compiled code */ }

    public func backward(mark: std.collection.LinkedListNode<T>): std.core.Iterator<T> { /* compiled code */ }

    private func checkNode(node: std.collection.LinkedListNode<T>): Unit { /* compiled code */ }
}

internal class LinkedListIterator<T> <: std.core.Iterator<T> {
    private let _lockVersion: Int64

    private let _data: std.collection.LinkedList<T>

    private var _currentNode: std.collection.LinkedListNode<T>

    private let _backward: Bool

    init(data: std.collection.LinkedList<T>, node: std.core.Option<std.collection.LinkedListNode<T>>, backward: Bool) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<T> { /* compiled code */ }
}

public interface Map<K, V> <: std.collection.ReadOnlyMap<K, V> {
    public open func add(key: K, value: V): std.core.Option<V>

    public open func add(all!: std.core.Collection<(K, V)>): Unit

    public open func remove(key: K): std.core.Option<V>

    public open func remove(all!: std.core.Collection<K>): Unit

    public open func removeIf(predicate: (K, V) -> Bool): Unit

    public open func clear(): Unit

    public open operator func [](key: K, value!: V): Unit

    public open func entryView(k: K): std.collection.MapEntryView<K, V>

    @Frozen
    public open func addIfAbsent(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    @Frozen
    public open func replace(key: K, value: V): std.core.Option<V> { /* compiled code */ }
}

internal struct TreeMapEntryView<K, V> <: std.collection.MapEntryView<K, V> {
    internal let map: std.collection.TreeMap<K, V>

    internal var curNode: std.collection.SearchResult<K, V>

    internal var lockVersion: Int64

    internal var _key: std.core.Option<K>

    init(map: std.collection.TreeMap<K, V>, curNode: std.collection.SearchResult<K, V>, key!: std.core.Option<K> = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    public prop key: K {
        get() { /* compiled code */ }
    }

    public mut prop value: std.core.Option<V> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal func nextNode(): std.collection.TreeMapEntryView<K, V> { /* compiled code */ }

    internal func prevNode(): std.collection.TreeMapEntryView<K, V> { /* compiled code */ }

    internal func isSome(): Bool { /* compiled code */ }
}

public class TreeMap<K, V> <: std.core.Object & std.collection.OrderedMap<K, V> {
    private var _size: Int64 = COMPILED_CODE /* compiled code */

    private var modCount: Int64 = COMPILED_CODE /* compiled code */

    private var root: std.collection.Node<K, V>

    init() { /* compiled code */ }

    init(elements: std.core.Collection<(K, V)>) { /* compiled code */ }

    init(elements: std.core.Array<(K, V)>) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> (K, V)) { /* compiled code */ }

    public func get(key: K): std.core.Option<V> { /* compiled code */ }

    public func contains(key: K): Bool { /* compiled code */ }

    public func contains(all!: std.core.Collection<K>): Bool { /* compiled code */ }

    public func add(key: K, value: V): std.core.Option<V> { /* compiled code */ }

    public func add(all!: std.core.Collection<(K, V)>): Unit { /* compiled code */ }

    public func remove(key: K): std.core.Option<V> { /* compiled code */ }

    public func remove(all!: std.core.Collection<K>): Unit { /* compiled code */ }

    public func removeIf(predicate: (K, V) -> Bool): Unit { /* compiled code */ }

    public func clear(): Unit { /* compiled code */ }

    public func clone(): std.collection.TreeMap<K, V> { /* compiled code */ }

    public operator func [](key: K): V { /* compiled code */ }

    public operator func [](key: K, value!: V): Unit { /* compiled code */ }

    public func keys(): std.collection.EquatableCollection<K> { /* compiled code */ }

    public func values(): std.core.Collection<V> { /* compiled code */ }

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    public func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<(K, V)> { /* compiled code */ }

    public prop first: std.core.Option<(K, V)> {
        get() { /* compiled code */ }
    }

    public func removeFirst(): std.core.Option<(K, V)> { /* compiled code */ }

    public prop last: std.core.Option<(K, V)> {
        get() { /* compiled code */ }
    }

    public func removeLast(): std.core.Option<(K, V)> { /* compiled code */ }

    internal func searchNearestEntryView(array: std.collection.ArrayList<K>, key: K, forward!: Bool): std.collection.TreeMapEntryView<K, V> { /* compiled code */ }

    public func backward(mark: K, inclusive!: Bool = COMPILED_CODE /* compiled code */): std.core.Iterator<(K, V)> { /* compiled code */ }

    public func forward(mark: K, inclusive!: Bool = COMPILED_CODE /* compiled code */): std.core.Iterator<(K, V)> { /* compiled code */ }

    internal func version(): Int64 { /* compiled code */ }

    internal func getFirstEntry(): std.collection.SearchResult<K, V> { /* compiled code */ }

    internal func getLastEntry(): std.collection.SearchResult<K, V> { /* compiled code */ }

    internal func searchEntry(node: std.core.Option<std.collection.Node<K, V>>, key: K): std.collection.SearchResult<K, V> { /* compiled code */ }

    internal func searchEntryFromRoot(key: K): std.collection.SearchResult<K, V> { /* compiled code */ }

    internal func inserEntry(node: std.collection.Node<K, V>, entry: std.collection.TreeMapEntry<K, V>): std.core.Option<V> { /* compiled code */ }

    internal func splitNode(node: std.collection.Node<K, V>, splitNode: std.collection.Node<K, V>, index: Int64): Unit { /* compiled code */ }

    internal func deleteEntry(node: std.collection.Node<K, V>, key: K): std.core.Option<V> { /* compiled code */ }

    internal func predecessor(sr: std.collection.SearchResult<K, V>): std.collection.SearchResult<K, V> { /* compiled code */ }

    internal func successor(sr: std.collection.SearchResult<K, V>): std.collection.SearchResult<K, V> { /* compiled code */ }

    internal func parentOf(node: std.collection.Node<K, V>): std.collection.SearchResult<K, V> { /* compiled code */ }

    public func entryView(k: K): std.collection.MapEntryView<K, V> { /* compiled code */ }
}

internal struct SearchResult<K, V> {
    private var _index: Int64

    private var _replaceNode: std.core.Option<std.collection.Node<K, V>>

    init(index: Int64, node: std.core.Option<std.collection.Node<K, V>>) { /* compiled code */ }

    internal prop index: Int64 {
        get() { /* compiled code */ }
    }

    internal prop replaceNode: std.core.Option<std.collection.Node<K, V>> {
        get() { /* compiled code */ }
    }
}

internal class TreeMapIterator<K, V> <: std.core.Iterator<(K, V)> {
    private var curNode: std.collection.TreeMapEntryView<K, V>

    init(node: std.collection.TreeMapEntryView<K, V>) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<(K, V)> { /* compiled code */ }
}

internal func checkVersion<K, V>(node: std.collection.TreeMapEntryView<K, V>): Unit { /* compiled code */ }

public class TreeSet<T> <: std.core.Object & std.collection.OrderedSet<T> {
    private var map: std.collection.TreeMap<T, Unit>

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    public prop first: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    public prop last: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }

    init(elements: std.core.Collection<T>) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    public static func of(elements: std.core.Array<T>): std.collection.TreeSet<T> { /* compiled code */ }

    public func contains(element: T): Bool { /* compiled code */ }

    public func contains(all!: std.core.Collection<T>): Bool { /* compiled code */ }

    public func add(element: T): Bool { /* compiled code */ }

    public func add(all!: std.core.Collection<T>): Unit { /* compiled code */ }

    public func remove(element: T): Bool { /* compiled code */ }

    public func remove(all!: std.core.Collection<T>): Unit { /* compiled code */ }

    public func removeIf(predicate: (T) -> Bool): Unit { /* compiled code */ }

    public func clear(): Unit { /* compiled code */ }

    public func clone(): std.collection.TreeSet<T> { /* compiled code */ }

    public func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<T> { /* compiled code */ }

    public func removeFirst(): std.core.Option<T> { /* compiled code */ }

    public func removeLast(): std.core.Option<T> { /* compiled code */ }

    public func backward(mark: T, inclusive!: Bool = COMPILED_CODE /* compiled code */): std.core.Iterator<T> { /* compiled code */ }

    public func forward(mark: T, inclusive!: Bool = COMPILED_CODE /* compiled code */): std.core.Iterator<T> { /* compiled code */ }

    public func retain(all!: std.collection.Set<T>): Unit { /* compiled code */ }

    public func toArray(): std.core.Array<T> { /* compiled code */ }

    public func subsetOf(other: std.collection.ReadOnlySet<T>): Bool { /* compiled code */ }

    public operator func &(other: std.collection.ReadOnlySet<T>): std.collection.TreeSet<T> { /* compiled code */ }

    public operator func |(other: std.collection.ReadOnlySet<T>): std.collection.TreeSet<T> { /* compiled code */ }

    public operator func -(other: std.collection.ReadOnlySet<T>): std.collection.TreeSet<T> { /* compiled code */ }
}

public interface Deque<T> <: std.core.Collection<T> {
    public open prop first: std.core.Option<T>

    public open prop last: std.core.Option<T>

    public open func addFirst(element: T): Unit

    public open func addLast(element: T): Unit

    public open func removeFirst(): std.core.Option<T>

    public open func removeLast(): std.core.Option<T>
}

extend std.collection.ArrayDeque<T> <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

public interface List<T> <: std.collection.ReadOnlyList<T> {
    public open func add(element: T): Unit

    public open func add(all!: std.core.Collection<T>): Unit

    public open func add(element: T, at!: Int64): Unit

    public open func add(all!: std.core.Collection<T>, at!: Int64): Unit

    public open func remove(at!: Int64): T

    public open func remove(range: std.core.Range<Int64>): Unit

    public open func removeIf(predicate: (T) -> Bool): Unit

    public open func clear(): Unit

    public open operator func [](index: Int64, value!: T): Unit
}

extend std.collection.ArrayList<T> <: Equatable {
    @Frozen
    public operator func ==(that: std.collection.ArrayList<T>): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(that: std.collection.ArrayList<T>): Bool { /* compiled code */ }

    @Frozen
    public func contains(element: T): Bool { /* compiled code */ }
}

extend std.collection.ArrayList<T> {
    @Frozen
    @Deprecated[message: "Use global function `public func sort<T>(data: ArrayList<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` in std.sort instead."]
    public func sort(stable!: Bool): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use global function `public func sort<T>(data: ArrayList<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` in std.sort instead."]
    public func sort(): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use global function `public func sort<T>(data: ArrayList<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` in std.sort instead."]
    public func sortDescending(stable!: Bool): Unit { /* compiled code */ }

    @Frozen
    @Deprecated[message: "Use global function `public func sort<T>(data: ArrayList<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` in std.sort instead."]
    public func sortDescending(): Unit { /* compiled code */ }
}

extend std.collection.ArrayList<T> <: ToString {
    @Frozen
    public func toString(): std.core.String { /* compiled code */ }
}

internal class TempArray<T> {
    private var data: std.core.Array<T>

    private let zero: T

    private let max: Int64

    init(n: Int64, z: T) { /* compiled code */ }

    internal func getArray(n: Int64): std.core.Array<T> { /* compiled code */ }
}

public class ArrayQueue<T> <: std.core.Object & std.collection.Queue<T> {
    internal let _data: std.collection.ArrayDeque<T>

    init() { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    public func peek(): std.core.Option<T> { /* compiled code */ }

    public func add(element: T): Unit { /* compiled code */ }

    public func remove(): std.core.Option<T> { /* compiled code */ }

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    public prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    public func isEmpty(): Bool { /* compiled code */ }

    public func clear(): Unit { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<T> { /* compiled code */ }

    public func toArray(): std.core.Array<T> { /* compiled code */ }

    public func reserve(additional: Int64): Unit { /* compiled code */ }
}

public interface Queue<T> <: std.core.Collection<T> {
    public open func add(element: T): Unit

    public open func peek(): std.core.Option<T>

    public open func remove(): std.core.Option<T>
}

extend std.collection.ArrayQueue<T> <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

public interface Stack<T> <: std.core.Collection<T> {
    public open func peek(): std.core.Option<T>

    public open func remove(): std.core.Option<T>

    public open func add(element: T): Unit
}

extend std.collection.ArrayStack<T> <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

public func fold<T, R>(initial: R, operation: (R, T) -> R): R { /* compiled code */ }

public func isEmpty<T>(it: std.core.Iterable<T>): Bool { /* compiled code */ }

public func count<T>(it: std.core.Iterable<T>): Int64 { /* compiled code */ }

public func contains<T>(element: T): Bool { /* compiled code */ }

public func max<T>(it: std.core.Iterable<T>): std.core.Option<T> { /* compiled code */ }

public func min<T>(it: std.core.Iterable<T>): std.core.Option<T> { /* compiled code */ }

public func all<T>(predicate: (T) -> Bool): Bool { /* compiled code */ }

public func any<T>(predicate: (T) -> Bool): Bool { /* compiled code */ }

public func none<T>(predicate: (T) -> Bool): Bool { /* compiled code */ }

public func first<T>(it: std.core.Iterable<T>): std.core.Option<T> { /* compiled code */ }

public func last<T>(it: std.core.Iterable<T>): std.core.Option<T> { /* compiled code */ }

public func at<T>(n: Int64): std.core.Option<T> { /* compiled code */ }

public func forEach<T>(action: (T) -> Unit): Unit { /* compiled code */ }

extend std.collection.HashMap<K, V> <: ToString {
    @Frozen
    public func toString(): std.core.String { /* compiled code */ }
}

extend std.collection.HashMap<K, V> <: Equatable {
    @Frozen
    public operator func ==(right: std.collection.HashMap<K, V>): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(right: std.collection.HashMap<K, V>): Bool { /* compiled code */ }
}

extend std.collection.HashSet<T> <: Equatable {
    @Frozen
    public operator func ==(that: std.collection.HashSet<T>): Bool { /* compiled code */ }

    @Frozen
    public operator func !=(that: std.collection.HashSet<T>): Bool { /* compiled code */ }
}

extend std.collection.HashSet<T> <: ToString {
    @Frozen
    public func toString(): std.core.String { /* compiled code */ }
}

extend std.collection.LinkedList<T> <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend std.collection.LinkedList<T> <: Equatable {
    public operator func ==(right: std.collection.LinkedList<T>): Bool { /* compiled code */ }

    public operator func !=(right: std.collection.LinkedList<T>): Bool { /* compiled code */ }
}

public interface ReadOnlyList<T> <: std.core.Collection<T> {
    public open prop first: std.core.Option<T>

    public open prop last: std.core.Option<T>

    public open func get(index: Int64): std.core.Option<T>

    public open operator func [](index: Int64): T
}

public interface ReadOnlyMap<K, V> <: std.core.Collection<(K, V)> {
    public open func get(key: K): std.core.Option<V>

    public open func contains(key: K): Bool

    public open func contains(all!: std.core.Collection<K>): Bool

    public open func keys(): std.collection.EquatableCollection<K>

    public open func values(): std.core.Collection<V>

    public open operator func [](key: K): V
}

public interface OrderedMap<K, V> <: std.collection.Map<K, V> {
    public open prop first: std.core.Option<(K, V)>

    public open prop last: std.core.Option<(K, V)>

    public open func removeFirst(): std.core.Option<(K, V)>

    public open func removeLast(): std.core.Option<(K, V)>

    public open func backward(mark: K, inclusive!: Bool): std.core.Iterator<(K, V)>

    public open func forward(mark: K, inclusive!: Bool): std.core.Iterator<(K, V)>
}

public func enumerate<T>(it: std.core.Iterable<T>): std.core.Iterator<(Int64, T)> { /* compiled code */ }

public func filterMap<T, R>(transform: (T) -> std.core.Option<R>): std.core.Iterator<R> { /* compiled code */ }

public func flatten<T, R>(it: std.core.Iterable<T>): std.core.Iterator<R> { /* compiled code */ }

public func flatMap<T, R>(transform: (T) -> std.core.Iterable<R>): std.core.Iterator<R> { /* compiled code */ }

public func zip<T, R>(other: std.core.Iterable<R>): std.core.Iterator<(T, R)> { /* compiled code */ }

public func skip<T>(count: Int64): std.core.Iterator<T> { /* compiled code */ }

public func take<T>(count: Int64): std.core.Iterator<T> { /* compiled code */ }

public func step<T>(count: Int64): std.core.Iterator<T> { /* compiled code */ }

public func inspect<T>(action: (T) -> Unit): std.core.Iterator<T> { /* compiled code */ }

internal class FlattenIterator<T, R> <: std.core.Iterator<R> {
    private var subIt: std.core.Option<std.core.Iterator<R>> = COMPILED_CODE /* compiled code */

    private let it: std.core.Iterator<T>

    init(it: std.core.Iterable<T>) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<R> { /* compiled code */ }
}

internal class FlatMapIterator<T, R> <: std.core.Iterator<R> {
    private var subIt: std.core.Option<std.core.Iterator<R>> = COMPILED_CODE /* compiled code */

    private let transform: (T) -> std.core.Iterable<R>

    private let it: std.core.Iterator<T>

    init(it: std.core.Iterable<T>, transform: (T) -> std.core.Iterable<R>) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<R> { /* compiled code */ }
}

public interface OrderedSet<T> <: std.collection.Set<T> {
    public open prop first: std.core.Option<T>

    public open prop last: std.core.Option<T>

    public open func removeFirst(): std.core.Option<T>

    public open func removeLast(): std.core.Option<T>

    public open func backward(mark: T, inclusive!: Bool): std.core.Iterator<T>

    public open func forward(mark: T, inclusive!: Bool): std.core.Iterator<T>
}

public func collectString<T>(delimiter!: std.core.String = COMPILED_CODE /* compiled code */): std.core.String { /* compiled code */ }

internal class Node<K, V> {
    internal var entries: std.collection.ArrayList<std.collection.TreeMapEntry<K, V>>

    internal var _parent: std.core.Option<std.collection.Node<K, V>>

    internal var children: std.collection.ArrayList<std.collection.Node<K, V>>

    internal static func nullNode(): std.core.Option<std.collection.Node<K, V>> { /* compiled code */ }

    init(parent: std.core.Option<std.collection.Node<K, V>>) { /* compiled code */ }

    internal func isLeaf(): Bool { /* compiled code */ }

    internal prop entrySize: Int64 {
        get() { /* compiled code */ }
    }

    internal prop childrenSize: Int64 {
        get() { /* compiled code */ }
    }

    internal func quickEquals(that: std.collection.Node<K, V>): Bool { /* compiled code */ }

    internal func isEmpty(): Bool { /* compiled code */ }

    internal func searchEntry(key: K): std.collection.SearchResult<K, V> { /* compiled code */ }

    internal func getEntry(index: Int64): std.core.Option<std.collection.TreeMapEntry<K, V>> { /* compiled code */ }

    internal func insertEntry(entry: std.collection.TreeMapEntry<K, V>): std.core.Option<V> { /* compiled code */ }

    internal func insertEntry(index: Int64, entry: std.collection.TreeMapEntry<K, V>): Unit { /* compiled code */ }

    internal func deleteEntry(index: Int64): Unit { /* compiled code */ }

    internal func getChild(index: Int64): std.core.Option<std.collection.Node<K, V>> { /* compiled code */ }

    internal func deleteChild(index: Int64): Unit { /* compiled code */ }

    internal func insertChild(index: Int64, node: std.collection.Node<K, V>): Unit { /* compiled code */ }
}

internal class TreeMapEntry<K, V> {
    private var _key: K

    private var _value: V

    init(key: K, value: V) { /* compiled code */ }

    internal prop key: K {
        get() { /* compiled code */ }
    }

    internal mut prop value: V {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    internal func reset(key: K, value: V): Unit { /* compiled code */ }
}

extend std.collection.TreeMap<K, V> <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend std.collection.TreeMap<K, V> <: Equatable {
    public operator func ==(right: std.collection.TreeMap<K, V>): Bool { /* compiled code */ }

    public operator func !=(right: std.collection.TreeMap<K, V>): Bool { /* compiled code */ }
}

internal class ForwardIterator<K, V> <: std.core.Iterator<(K, V)> {
    private var curNode: std.collection.TreeMapEntryView<K, V>

    private var key: K

    private var inclusive: Bool

    init(node: std.collection.TreeMapEntryView<K, V>, key: K, inclusive: Bool) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<(K, V)> { /* compiled code */ }
}

internal class BackwardIterator<K, V> <: std.core.Iterator<(K, V)> {
    private var curNode: std.collection.TreeMapEntryView<K, V>

    private var key: K

    private var inclusive: Bool

    init(node: std.collection.TreeMapEntryView<K, V>, key: K, inclusive: Bool) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<(K, V)> { /* compiled code */ }
}

internal class TreeMapKeys<K, V> <: std.core.Object & std.collection.EquatableCollection<K> {
    private let map: std.collection.TreeMap<K, V>

    init(m: std.collection.TreeMap<K, V>) { /* compiled code */ }

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    public func isEmpty(): Bool { /* compiled code */ }

    public func contains(element: K): Bool { /* compiled code */ }

    public func contains(all!: std.core.Collection<K>): Bool { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<K> { /* compiled code */ }
}

internal class TreeMapValues<K, V> <: std.core.Object & std.core.Collection<V> {
    private let map: std.collection.TreeMap<K, V>

    init(m: std.collection.TreeMap<K, V>) { /* compiled code */ }

    public prop size: Int64 {
        get() { /* compiled code */ }
    }

    public func isEmpty(): Bool { /* compiled code */ }

    @Frozen
    public func iterator(): std.core.Iterator<V> { /* compiled code */ }
}

internal class IteratorForSet<T> <: std.core.Iterator<T> {
    private let it: std.core.Iterator<(T, Unit)>

    init(it: std.core.Iterator<(T, Unit)>) { /* compiled code */ }

    @Frozen
    public func next(): std.core.Option<T> { /* compiled code */ }
}

extend std.collection.TreeSet<T> <: Equatable {
    public operator func ==(that: std.collection.TreeSet<T>): Bool { /* compiled code */ }

    public operator func !=(that: std.collection.TreeSet<T>): Bool { /* compiled code */ }
}

extend std.collection.TreeSet<T> <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}
