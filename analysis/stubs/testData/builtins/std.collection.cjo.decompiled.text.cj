// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.collection

import std.core.*
import std.math.*

func collectionToString(col: C): std.core.String

func max(a: Int64, b: Int64): Int64

func reduce(operation: (T, T) -> T): (std.core.Iterable<T>) -> std.core.Option<T>

func filter(predicate: (T) -> Bool): (std.core.Iterable<T>) -> std.core.Iterator<T>

func map(transform: (T) -> R): (std.core.Iterable<T>) -> std.core.Iterator<R>

func concat(other: std.core.Iterable<T>): (std.core.Iterable<T>) -> std.core.Iterator<T>

func collectArray(it: std.core.Iterable<T>): std.core.Array<T>

class ArrayList <: std.core.Object & std.collection.List<T> {
    var myData: std.core.Array<T>

    var mySize: Int64

    var myVersion: Int64

    let DEFAULT_CAPACITY: Int64

    prop capacity: Int64

    init() { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    init(elements: std.core.Array<T>) { /* compiled code */ }

    init(elements: std.core.Collection<T>) { /* compiled code */ }

    func of(elements: std.core.Array<T>): std.collection.ArrayList<T>

    func getRawArray: std.core.Array<T>

    func toArray: std.core.Array<T>

    func `get`(index: Int64): std.core.Option<T>

    prop first: std.core.Option<T>

    prop last: std.core.Option<T>

    func add(element: T): Unit

    func add(all: std.core.Collection<T>): Unit

    func add(element: T, at: Int64): Unit

    func add(all: std.core.Collection<T>, at: Int64): Unit

    func remove(at: Int64): T

    func remove(range: std.core.Range<Int64>): Unit

    func removeIf(predicate: (T) -> Bool): Unit

    func clear: Unit

    func reserve(additional: Int64): Unit

    func sortBy(stable: Bool, comparator: (T, T) -> std.core.Ordering): Unit

    func sortBy(comparator: (T, T) -> std.core.Ordering): Unit

    func reverse: Unit

    operator func [](range: std.core.Range<Int64>): std.collection.ArrayList<T>

    func slice(range: std.core.Range<Int64>): std.collection.ArrayList<T>

    func rangeSlice(arr: std.core.Array<T>, range: std.core.Range<Int64>, size: Int64): std.core.Array<T>

    func clone: std.collection.ArrayList<T>

    operator func [](index: Int64): T

    operator func [](index: Int64, value: T): Unit

    func iterator: std.core.Iterator<T>

    func isEmpty: Bool

    prop size: Int64

    func checkRange(index: Int64): Unit

    func checkRangeInsert(index: Int64): Unit

    func grow(minCapacity: Int64, startIndex: Int64): Unit

    func updateVersion: Unit
}

func collectArrayList(it: std.core.Iterable<T>): std.collection.ArrayList<T>

func collectHashSet(it: std.core.Iterable<T>): std.collection.HashSet<T>

class HashSet <: std.core.Object & std.collection.Set<T> {
    var map: std.collection.HashMap<T, Unit>

    init() { /* compiled code */ }

    init(elements: std.core.Collection<T>) { /* compiled code */ }

    init(elements: std.core.Array<T>) { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    func contains(element: T): Bool

    func subsetOf(other: std.collection.ReadOnlySet<T>): Bool

    func contains(all: std.core.Collection<T>): Bool

    func add(element: T): Bool

    func remove(element: T): Bool

    func add(all: std.core.Collection<T>): Unit

    func remove(all: std.core.Collection<T>): Unit

    func removeIf(predicate: (T) -> Bool): Unit

    func clear: Unit

    func retain(all: std.collection.Set<T>): Unit

    func clone: std.collection.HashSet<T>

    func reserve(additional: Int64): Unit

    prop capacity: Int64

    func iterator: std.core.Iterator<T>

    prop size: Int64

    func isEmpty: Bool

    func toArray: std.core.Array<T>

    operator func &(other: std.collection.ReadOnlySet<T>): std.collection.HashSet<T>

    operator func |(other: std.collection.ReadOnlySet<T>): std.collection.HashSet<T>

    operator func -(other: std.collection.ReadOnlySet<T>): std.collection.HashSet<T>
}

func collectHashMap(it: std.core.Iterable<(K, V)>): std.collection.HashMap<K, V>

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

    func `get`(key: K): std.core.Option<V>

    func add(key: K, value: V): std.core.Option<V>

    func putWithoutResize(key: K, value: V): Unit

    func entryView(key: K): std.collection.MapEntryView<K, V>

    func add(all: std.core.Collection<(K, V)>): Unit

    func remove(key: K): std.core.Option<V>

    func remove(all: std.core.Collection<K>): Unit

    func removeIf(predicate: (K, V) -> Bool): Unit

    func clear: Unit

    func reserve(additional: Int64): Unit

    prop capacity: Int64

    func contains(all: std.core.Collection<K>): Bool

    func contains(key: K): Bool

    func clone: std.collection.HashMap<K, V>

    func keys: std.collection.EquatableCollection<K>

    func values: std.core.Collection<V>

    operator func [](key: K): V

    operator func [](key: K, value: V): Unit

    func getIndex(hash: Int64, size: Int64): Int64

    func getHash(hashCode: Int64): Int64

    prop size: Int64

    func iterator: std.collection.HashMapIterator<K, V>

    func isEmpty: Bool

    func version: Int64

    func max(l: Int64, r: Int64): Int64

    func toArray: std.core.Array<(K, V)>

    func resize(argCap: Int64): Bool

    func tableSizeFor(cap: Int64): Int64

    func removePosition(hash: Int64, pos: Int64): Unit
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

    func next: std.core.Option<T>
}

class ArrayDeque <: std.core.Object & std.collection.Deque<T> {
    var _data: std.core.Array<T>

    var _version: Int64

    var _head: Int64

    var _tail: Int64

    init() { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    prop size: Int64

    prop first: std.core.Option<T>

    prop last: std.core.Option<T>

    prop capacity: Int64

    func isEmpty: Bool

    func addFirst(element: T): Unit

    func addLast(element: T): Unit

    func removeFirst: std.core.Option<T>

    func removeLast: std.core.Option<T>

    func toArray: std.core.Array<T>

    func clear: Unit

    func reserve(additional: Int64): Unit

    func iterator: std.core.Iterator<T>
}

class ConcurrentModificationException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class ArrayListIterator <: std.core.Iterator<T> {
    var myPosition: Int64

    let myData: std.collection.ArrayList<T>

    let initVersion: Int64

    init(data: std.collection.ArrayList<T>) { /* compiled code */ }

    func next: std.core.Option<T>
}

func stableSort(data: std.core.Array<T>, comparator: (T, T) -> std.core.Ordering): Unit

func unstableSort(data: std.core.Array<T>, comparator: (T, T) -> std.core.Ordering): Unit

class ArrayStackIterator <: std.core.Iterator<T> {
    let _stack: std.collection.ArrayStack<T>

    var _position: Int64

    let _lockVersion: Int64

    init(data: std.collection.ArrayStack<T>) { /* compiled code */ }

    func next: std.core.Option<T>
}

class ArrayStack <: std.core.Object & std.collection.Stack<T> {
    var _data: std.core.Array<T>

    var _size: Int64

    var _version: Int64

    init(capacity: Int64) { /* compiled code */ }

    init() { /* compiled code */ }

    func peek: std.core.Option<T>

    func remove: std.core.Option<T>

    func add(element: T): Unit

    prop capacity: Int64

    prop size: Int64

    func isEmpty: Bool

    func reserve(additional: Int64): Unit

    func clear: Unit

    func toArray: std.core.Array<T>

    func iterator: std.core.Iterator<T>
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

    prop size: Int64

    func isEmpty: Bool

    func contains(element: K): Bool

    func contains(all: std.core.Collection<K>): Bool

    func iterator: std.core.Iterator<K>

    func toArray: std.core.Array<K>
}

class HashMapIterator <: std.core.Iterator<(K, V)> {
    var lockVersion: Int64

    var index: Int64

    let data: std.collection.HashMap<K, V>

    var isRemoved: Bool

    init(map: std.collection.HashMap<K, V>) { /* compiled code */ }

    func next: std.core.Option<(K, V)>

    func remove: std.core.Option<(K, V)>
}

class HashMapValues <: std.core.Object & std.core.Collection<V> {
    let map: std.collection.HashMap<K, V>

    init(m: std.collection.HashMap<K, V>) { /* compiled code */ }

    prop size: Int64

    func isEmpty: Bool

    func iterator: std.core.Iterator<V>

    func toArray: std.core.Array<V>
}

class HashMapEntryView <: std.core.Object & std.collection.MapEntryView<K, V> {
    let map: std.collection.HashMap<K, V>

    let _key: K

    let hash: Int64

    var index: Int64

    var _isAbsent: Bool

    var lockVersion: Int64

    init(isAbsent: Bool, key: K, hash: Int64, index: Int64, map: std.collection.HashMap<K, V>) { /* compiled code */ }

    prop key: K

    prop value: std.core.Option<V>

    func getKey: K

    func getValue: std.core.Option<V>

    func setValue(v: V): Unit
}

interface MapEntryView {
    prop key: K

    prop value: std.core.Option<V>
}

interface EquatableCollection <: std.core.Collection<T> {
    func contains(element: T): Bool

    func contains(all: std.core.Collection<T>): Bool
}

interface ReadOnlySet <: std.core.Collection<T> {
    func contains(element: T): Bool

    func contains(all: std.core.Collection<T>): Bool

    func subsetOf(other: std.collection.ReadOnlySet<T>): Bool
}

interface Set <: std.collection.ReadOnlySet<T> {
    func add(element: T): Bool

    func add(all: std.core.Collection<T>): Unit

    func remove(element: T): Bool

    func remove(all: std.core.Collection<T>): Unit

    func removeIf(predicate: (T) -> Bool): Unit

    func clear: Unit

    func retain(all: std.collection.Set<T>): Unit
}

class LinkedListNode {
    var _next: std.core.Option<std.collection.LinkedListNode<T>>

    var _prev: std.core.Option<std.collection.LinkedListNode<T>>

    var _item: T

    var _list: std.core.Option<std.collection.LinkedList<T>>

    init(element: T, prev: std.core.Option<std.collection.LinkedListNode<T>>, next: std.core.Option<std.collection.LinkedListNode<T>>, belongs: std.core.Option<std.collection.LinkedList<T>>) { /* compiled code */ }

    prop value: T

    prop next: std.core.Option<std.collection.LinkedListNode<T>>

    prop prev: std.core.Option<std.collection.LinkedListNode<T>>

    func insertBefore(element: T): std.collection.LinkedListNode<T>

    func insertAfter(element: T): std.collection.LinkedListNode<T>

    func remove: T

    func reverse: Unit

    func checkNodeState: Unit
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

    prop size: Int64

    prop first: std.core.Option<T>

    prop last: std.core.Option<T>

    prop firstNode: std.core.Option<std.collection.LinkedListNode<T>>

    prop lastNode: std.core.Option<std.collection.LinkedListNode<T>>

    prop version: Int64

    func addFirst(element: T): std.collection.LinkedListNode<T>

    func addLast(element: T): std.collection.LinkedListNode<T>

    func toArray: std.core.Array<T>

    func addBefore(node: std.collection.LinkedListNode<T>, element: T): std.collection.LinkedListNode<T>

    func addAfter(node: std.collection.LinkedListNode<T>, element: T): std.collection.LinkedListNode<T>

    func remove(node: std.collection.LinkedListNode<T>): T

    func removeIf(predicate: (T) -> Bool): Unit

    func clear: Unit

    func reverse: Unit

    func isEmpty: Bool

    func iterator: std.core.Iterator<T>

    func removeFirst: std.core.Option<T>

    func removeLast: std.core.Option<T>

    func nodeAt(index: Int64): std.core.Option<std.collection.LinkedListNode<T>>

    func splitOff(node: std.collection.LinkedListNode<T>): std.collection.LinkedList<T>

    func forward(mark: std.collection.LinkedListNode<T>): std.core.Iterator<T>

    func backward(mark: std.collection.LinkedListNode<T>): std.core.Iterator<T>

    func checkNode(node: std.collection.LinkedListNode<T>): Unit
}

class LinkedListIterator <: std.core.Iterator<T> {
    let _lockVersion: Int64

    let _data: std.collection.LinkedList<T>

    var _currentNode: std.collection.LinkedListNode<T>

    let _backward: Bool

    init(data: std.collection.LinkedList<T>, node: std.core.Option<std.collection.LinkedListNode<T>>, backward: Bool) { /* compiled code */ }

    func next: std.core.Option<T>
}

interface Map <: std.collection.ReadOnlyMap<K, V> {
    func add(key: K, value: V): std.core.Option<V>

    func add(all: std.core.Collection<(K, V)>): Unit

    func remove(key: K): std.core.Option<V>

    func remove(all: std.core.Collection<K>): Unit

    func removeIf(predicate: (K, V) -> Bool): Unit

    func clear: Unit

    operator func [](key: K, value: V): Unit

    func entryView(k: K): std.collection.MapEntryView<K, V>

    func addIfAbsent(key: K, value: V): std.core.Option<V>

    func replace(key: K, value: V): std.core.Option<V>
}

struct TreeMapEntryView <: std.collection.MapEntryView<K, V> {
    let map: std.collection.TreeMap<K, V>

    var curNode: std.collection.SearchResult<K, V>

    var lockVersion: Int64

    var _key: std.core.Option<K>

    init(map: std.collection.TreeMap<K, V>, curNode: std.collection.SearchResult<K, V>, key: std.core.Option<K>) { /* compiled code */ }

    prop key: K

    prop value: std.core.Option<V>

    func nextNode: std.collection.TreeMapEntryView<K, V>

    func prevNode: std.collection.TreeMapEntryView<K, V>

    func isSome: Bool
}

class TreeMap <: std.core.Object & std.collection.OrderedMap<K, V> {
    var _size: Int64

    var modCount: Int64

    var root: std.collection.Node<K, V>

    init() { /* compiled code */ }

    init(elements: std.core.Collection<(K, V)>) { /* compiled code */ }

    init(elements: std.core.Array<(K, V)>) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> (K, V)) { /* compiled code */ }

    func `get`(key: K): std.core.Option<V>

    func contains(key: K): Bool

    func contains(all: std.core.Collection<K>): Bool

    func add(key: K, value: V): std.core.Option<V>

    func add(all: std.core.Collection<(K, V)>): Unit

    func remove(key: K): std.core.Option<V>

    func remove(all: std.core.Collection<K>): Unit

    func removeIf(predicate: (K, V) -> Bool): Unit

    func clear: Unit

    func clone: std.collection.TreeMap<K, V>

    operator func [](key: K): V

    operator func [](key: K, value: V): Unit

    func keys: std.collection.EquatableCollection<K>

    func values: std.core.Collection<V>

    prop size: Int64

    func isEmpty: Bool

    func iterator: std.core.Iterator<(K, V)>

    prop first: std.core.Option<(K, V)>

    func removeFirst: std.core.Option<(K, V)>

    prop last: std.core.Option<(K, V)>

    func removeLast: std.core.Option<(K, V)>

    func searchNearestEntryView(array: std.collection.ArrayList<K>, key: K, forward: Bool): std.collection.TreeMapEntryView<K, V>

    func backward(mark: K, inclusive: Bool): std.core.Iterator<(K, V)>

    func forward(mark: K, inclusive: Bool): std.core.Iterator<(K, V)>

    func version: Int64

    func getFirstEntry: std.collection.SearchResult<K, V>

    func getLastEntry: std.collection.SearchResult<K, V>

    func searchEntry(node: std.core.Option<std.collection.Node<K, V>>, key: K): std.collection.SearchResult<K, V>

    func searchEntryFromRoot(key: K): std.collection.SearchResult<K, V>

    func inserEntry(node: std.collection.Node<K, V>, entry: std.collection.TreeMapEntry<K, V>): std.core.Option<V>

    func splitNode(node: std.collection.Node<K, V>, splitNode: std.collection.Node<K, V>, index: Int64): Unit

    func deleteEntry(node: std.collection.Node<K, V>, key: K): std.core.Option<V>

    func predecessor(sr: std.collection.SearchResult<K, V>): std.collection.SearchResult<K, V>

    func successor(sr: std.collection.SearchResult<K, V>): std.collection.SearchResult<K, V>

    func parentOf(node: std.collection.Node<K, V>): std.collection.SearchResult<K, V>

    func entryView(k: K): std.collection.MapEntryView<K, V>
}

struct SearchResult {
    var _index: Int64

    var _replaceNode: std.core.Option<std.collection.Node<K, V>>

    init(index: Int64, node: std.core.Option<std.collection.Node<K, V>>) { /* compiled code */ }

    prop index: Int64

    prop replaceNode: std.core.Option<std.collection.Node<K, V>>
}

class TreeMapIterator <: std.core.Iterator<(K, V)> {
    var curNode: std.collection.TreeMapEntryView<K, V>

    init(node: std.collection.TreeMapEntryView<K, V>) { /* compiled code */ }

    func next: std.core.Option<(K, V)>
}

func checkVersion(node: std.collection.TreeMapEntryView<K, V>): Unit

class TreeSet <: std.core.Object & std.collection.OrderedSet<T> {
    var map: std.collection.TreeMap<T, Unit>

    prop size: Int64

    prop first: std.core.Option<T>

    prop last: std.core.Option<T>

    init() { /* compiled code */ }

    init(elements: std.core.Collection<T>) { /* compiled code */ }

    init(size: Int64, initElement: (Int64) -> T) { /* compiled code */ }

    func of(elements: std.core.Array<T>): std.collection.TreeSet<T>

    func contains(element: T): Bool

    func contains(all: std.core.Collection<T>): Bool

    func add(element: T): Bool

    func add(all: std.core.Collection<T>): Unit

    func remove(element: T): Bool

    func remove(all: std.core.Collection<T>): Unit

    func removeIf(predicate: (T) -> Bool): Unit

    func clear: Unit

    func clone: std.collection.TreeSet<T>

    func isEmpty: Bool

    func iterator: std.core.Iterator<T>

    func removeFirst: std.core.Option<T>

    func removeLast: std.core.Option<T>

    func backward(mark: T, inclusive: Bool): std.core.Iterator<T>

    func forward(mark: T, inclusive: Bool): std.core.Iterator<T>

    func retain(all: std.collection.Set<T>): Unit

    func toArray: std.core.Array<T>

    func subsetOf(other: std.collection.ReadOnlySet<T>): Bool

    operator func &(other: std.collection.ReadOnlySet<T>): std.collection.TreeSet<T>

    operator func |(other: std.collection.ReadOnlySet<T>): std.collection.TreeSet<T>

    operator func -(other: std.collection.ReadOnlySet<T>): std.collection.TreeSet<T>
}

interface Deque <: std.core.Collection<T> {
    prop first: std.core.Option<T>

    prop last: std.core.Option<T>

    func addFirst(element: T): Unit

    func addLast(element: T): Unit

    func removeFirst: std.core.Option<T>

    func removeLast: std.core.Option<T>
}

extend ArrayDeque <: ToString {
    func toString: std.core.String
}

interface List <: std.collection.ReadOnlyList<T> {
    func add(element: T): Unit

    func add(all: std.core.Collection<T>): Unit

    func add(element: T, at: Int64): Unit

    func add(all: std.core.Collection<T>, at: Int64): Unit

    func remove(at: Int64): T

    func remove(range: std.core.Range<Int64>): Unit

    func removeIf(predicate: (T) -> Bool): Unit

    func clear: Unit

    operator func [](index: Int64, value: T): Unit
}

extend ArrayList <: Equatable {
    operator func ==(that: std.collection.ArrayList<T>): Bool

    operator func !=(that: std.collection.ArrayList<T>): Bool

    func contains(element: T): Bool
}

extend ArrayList {
    func sort(stable: Bool): Unit

    func sort: Unit

    func sortDescending(stable: Bool): Unit

    func sortDescending: Unit
}

extend ArrayList <: ToString {
    func toString: std.core.String
}

class TempArray {
    var data: std.core.Array<T>

    let zero: T

    let max: Int64

    init(n: Int64, z: T) { /* compiled code */ }

    func getArray(n: Int64): std.core.Array<T>
}

class ArrayQueue <: std.core.Object & std.collection.Queue<T> {
    let _data: std.collection.ArrayDeque<T>

    init() { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    func peek: std.core.Option<T>

    func add(element: T): Unit

    func remove: std.core.Option<T>

    prop size: Int64

    prop capacity: Int64

    func isEmpty: Bool

    func clear: Unit

    func iterator: std.core.Iterator<T>

    func toArray: std.core.Array<T>

    func reserve(additional: Int64): Unit
}

interface Queue <: std.core.Collection<T> {
    func add(element: T): Unit

    func peek: std.core.Option<T>

    func remove: std.core.Option<T>
}

extend ArrayQueue <: ToString {
    func toString: std.core.String
}

interface Stack <: std.core.Collection<T> {
    func peek: std.core.Option<T>

    func remove: std.core.Option<T>

    func add(element: T): Unit
}

extend ArrayStack <: ToString {
    func toString: std.core.String
}

func fold(initial: R, operation: (R, T) -> R): (std.core.Iterable<T>) -> R

func isEmpty(it: std.core.Iterable<T>): Bool

func count(it: std.core.Iterable<T>): Int64

func contains(element: T): (std.core.Iterable<T>) -> Bool

func max(it: std.core.Iterable<T>): std.core.Option<T>

func min(it: std.core.Iterable<T>): std.core.Option<T>

func all(predicate: (T) -> Bool): (std.core.Iterable<T>) -> Bool

func any(predicate: (T) -> Bool): (std.core.Iterable<T>) -> Bool

func none(predicate: (T) -> Bool): (std.core.Iterable<T>) -> Bool

func first(it: std.core.Iterable<T>): std.core.Option<T>

func last(it: std.core.Iterable<T>): std.core.Option<T>

func at(n: Int64): (std.core.Iterable<T>) -> std.core.Option<T>

func forEach(action: (T) -> Unit): (std.core.Iterable<T>) -> Unit

extend HashMap <: ToString {
    func toString: std.core.String
}

extend HashMap <: Equatable {
    operator func ==(right: std.collection.HashMap<K, V>): Bool

    operator func !=(right: std.collection.HashMap<K, V>): Bool
}

extend HashSet <: Equatable {
    operator func ==(that: std.collection.HashSet<T>): Bool

    operator func !=(that: std.collection.HashSet<T>): Bool
}

extend HashSet <: ToString {
    func toString: std.core.String
}

extend LinkedList <: ToString {
    func toString: std.core.String
}

extend LinkedList <: Equatable {
    operator func ==(right: std.collection.LinkedList<T>): Bool

    operator func !=(right: std.collection.LinkedList<T>): Bool
}

interface ReadOnlyList <: std.core.Collection<T> {
    prop first: std.core.Option<T>

    prop last: std.core.Option<T>

    func `get`(index: Int64): std.core.Option<T>

    operator func [](index: Int64): T
}

interface ReadOnlyMap <: std.core.Collection<(K, V)> {
    func `get`(key: K): std.core.Option<V>

    func contains(key: K): Bool

    func contains(all: std.core.Collection<K>): Bool

    func keys: std.collection.EquatableCollection<K>

    func values: std.core.Collection<V>

    operator func [](key: K): V
}

interface OrderedMap <: std.collection.Map<K, V> {
    prop first: std.core.Option<(K, V)>

    prop last: std.core.Option<(K, V)>

    func removeFirst: std.core.Option<(K, V)>

    func removeLast: std.core.Option<(K, V)>

    func backward(mark: K, inclusive: Bool): std.core.Iterator<(K, V)>

    func forward(mark: K, inclusive: Bool): std.core.Iterator<(K, V)>
}

func enumerate(it: std.core.Iterable<T>): std.core.Iterator<(Int64, T)>

func filterMap(transform: (T) -> std.core.Option<R>): (std.core.Iterable<T>) -> std.core.Iterator<R>

func flatten(it: std.core.Iterable<T>): std.core.Iterator<R>

func flatMap(transform: (T) -> std.core.Iterable<R>): (std.core.Iterable<T>) -> std.core.Iterator<R>

func zip(other: std.core.Iterable<R>): (std.core.Iterable<T>) -> std.core.Iterator<(T, R)>

func skip(count: Int64): (std.core.Iterable<T>) -> std.core.Iterator<T>

func take(count: Int64): (std.core.Iterable<T>) -> std.core.Iterator<T>

func step(count: Int64): (std.core.Iterable<T>) -> std.core.Iterator<T>

func inspect(action: (T) -> Unit): (std.core.Iterable<T>) -> std.core.Iterator<T>

class FlattenIterator <: std.core.Iterator<R> {
    var subIt: std.core.Option<std.core.Iterator<R>>

    let it: std.core.Iterator<T>

    init(it: std.core.Iterable<T>) { /* compiled code */ }

    func next: std.core.Option<R>
}

class FlatMapIterator <: std.core.Iterator<R> {
    var subIt: std.core.Option<std.core.Iterator<R>>

    let transform: (T) -> std.core.Iterable<R>

    let it: std.core.Iterator<T>

    init(it: std.core.Iterable<T>, transform: (T) -> std.core.Iterable<R>) { /* compiled code */ }

    func next: std.core.Option<R>
}

interface OrderedSet <: std.collection.Set<T> {
    prop first: std.core.Option<T>

    prop last: std.core.Option<T>

    func removeFirst: std.core.Option<T>

    func removeLast: std.core.Option<T>

    func backward(mark: T, inclusive: Bool): std.core.Iterator<T>

    func forward(mark: T, inclusive: Bool): std.core.Iterator<T>
}

func collectString(delimiter: std.core.String): (std.core.Iterable<T>) -> std.core.String

class Node {
    var entries: std.collection.ArrayList<std.collection.TreeMapEntry<K, V>>

    var _parent: std.core.Option<std.collection.Node<K, V>>

    var children: std.collection.ArrayList<std.collection.Node<K, V>>

    func nullNode: std.core.Option<std.collection.Node<K, V>>

    init(parent: std.core.Option<std.collection.Node<K, V>>) { /* compiled code */ }

    func isLeaf: Bool

    prop entrySize: Int64

    prop childrenSize: Int64

    func quickEquals(that: std.collection.Node<K, V>): Bool

    func isEmpty: Bool

    func searchEntry(key: K): std.collection.SearchResult<K, V>

    func getEntry(index: Int64): std.core.Option<std.collection.TreeMapEntry<K, V>>

    func insertEntry(entry: std.collection.TreeMapEntry<K, V>): std.core.Option<V>

    func insertEntry(index: Int64, entry: std.collection.TreeMapEntry<K, V>): Unit

    func deleteEntry(index: Int64): Unit

    func getChild(index: Int64): std.core.Option<std.collection.Node<K, V>>

    func deleteChild(index: Int64): Unit

    func insertChild(index: Int64, node: std.collection.Node<K, V>): Unit
}

class TreeMapEntry {
    var _key: K

    var _value: V

    init(key: K, value: V) { /* compiled code */ }

    prop key: K

    prop value: V

    func reset(key: K, value: V): Unit
}

extend TreeMap <: ToString {
    func toString: std.core.String
}

extend TreeMap <: Equatable {
    operator func ==(right: std.collection.TreeMap<K, V>): Bool

    operator func !=(right: std.collection.TreeMap<K, V>): Bool
}

class ForwardIterator <: std.core.Iterator<(K, V)> {
    var curNode: std.collection.TreeMapEntryView<K, V>

    var key: K

    var inclusive: Bool

    init(node: std.collection.TreeMapEntryView<K, V>, key: K, inclusive: Bool) { /* compiled code */ }

    func next: std.core.Option<(K, V)>
}

class BackwardIterator <: std.core.Iterator<(K, V)> {
    var curNode: std.collection.TreeMapEntryView<K, V>

    var key: K

    var inclusive: Bool

    init(node: std.collection.TreeMapEntryView<K, V>, key: K, inclusive: Bool) { /* compiled code */ }

    func next: std.core.Option<(K, V)>
}

class TreeMapKeys <: std.core.Object & std.collection.EquatableCollection<K> {
    let map: std.collection.TreeMap<K, V>

    init(m: std.collection.TreeMap<K, V>) { /* compiled code */ }

    prop size: Int64

    func isEmpty: Bool

    func contains(element: K): Bool

    func contains(all: std.core.Collection<K>): Bool

    func iterator: std.core.Iterator<K>
}

class TreeMapValues <: std.core.Object & std.core.Collection<V> {
    let map: std.collection.TreeMap<K, V>

    init(m: std.collection.TreeMap<K, V>) { /* compiled code */ }

    prop size: Int64

    func isEmpty: Bool

    func iterator: std.core.Iterator<V>
}

class IteratorForSet <: std.core.Iterator<T> {
    let it: std.core.Iterator<(T, Unit)>

    init(it: std.core.Iterator<(T, Unit)>) { /* compiled code */ }

    func next: std.core.Option<T>
}

extend TreeSet <: Equatable {
    operator func ==(that: std.collection.TreeSet<T>): Bool

    operator func !=(that: std.collection.TreeSet<T>): Bool
}

extend TreeSet <: ToString {
    func toString: std.core.String
}
