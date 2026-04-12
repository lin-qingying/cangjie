// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package std.collection

import std.math.*

@!APILevel[12, atomicservice : true]
public class ArrayDeque<T> <: Deque<T> {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(capacity: Int64)
    
    @!APILevel[12, atomicservice : true]
    public prop size: Int64
    
    @!APILevel[12, atomicservice : true]
    public prop first: ?T
    
    @!APILevel[12, atomicservice : true]
    public prop last: ?T
    
    @!APILevel[12, atomicservice : true]
    public prop capacity: Int64
    
    @!APILevel[12, atomicservice : true]
    public func isEmpty(): Bool
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func addFirst(element: T): Unit
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func addLast(element: T): Unit
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func removeFirst(): ?T
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func removeLast(): ?T
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func toArray(): Array<T>
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func clear(): Unit
    
    @!APILevel[12, atomicservice : true]
    public func reserve(additional: Int64): Unit
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func iterator(): Iterator<T>
}

@!APILevel[12, atomicservice : true]
extend<T> ArrayDeque<T> <: ToString where T <: ToString {
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Resizable-array implementation of the Collection.
* Implements all Collection operations, and permits all elements, including null.
* In addition to implementing the Collection interface, this class provides methods to manipulate the size of the array that is used to store the ArrayList.
*
* @since 0.18.2
*/
@!APILevel[12, atomicservice : true]
public class ArrayList<T> <: List<T> {
    /* Defines the capacity of the ArrayList. */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public prop capacity: Int64
    
    /**
    * Initializes an empty ArrayList with the default capacity.
    *
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * Initializes an empty ArrayList with the specified initial capacity.
    *
    * @param capacity incoming initialized capacity.
    * @throws IllegalArgumentException if the capacity is less than 0.
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init(capacity: Int64)
    
    /**
    * Initializes an ArrayList with the specified initial size and specified initial function.
    *
    * @param size incoming initialize size.
    * @param initElement incoming initialize initElement.
    *
    * @throws IllegalArgumentException if size is negative.
    *
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init(size: Int64, initElement: (Int64) -> T)
    
    /**
    * Initializes an ArrayList with the iterable of elements initial function.
    *
    * @param  elements transfer the element type collection.
    *
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init(elements: Collection<T>)
    
    /**
    * Statically generate an ArrayList with the specified Array. 
    *
    * @param elements input element type array.
    *
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public static func of(elements: Array<T>): ArrayList<T>
    
    /**
    * Returns the raw data of the ArrayList.
    *
    * @since 0.23.3
    *
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public unsafe func getRawArray(): Array<T>
    
    /**
    * Returns the element in this ArrayList as an Array.
    *
    * @throws IndexOutOfBoundsException if "mySize" is out of bound.
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func toArray(): Array<T>
    
    /**
    * Gets the element at the specified location in this ArrayList.
    *
    * @param index indicates the index of the get interface.
    * @return the T type of value.
    *
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func get(index: Int64): ?T
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public prop first: ?T
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public prop last: ?T
    
    /**
    * Appends the specified element to the next index of the element at the end of the ArrayList
    *
    * @param element the type of the inserted element is T.
    *
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func add(element: T): Unit
    
    /**
    * Adds all elements in the specified collection after the end element of this ArrayList.
    *
    * @param elements import elements of the collection type and insert them one by one.
    * @throws ConcurrentModificationException if there are some system errors.
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func add(all!: Collection<T>): Unit
    
    /**
    * Inserts the specified element at the specified position in this ArrayList.
    *
    * @param index the index of the element to insert.
    * @param element the element to insert is of type T.
    * @throws IndexOutOfBoundsException if index is out of range.
    * @since 0.18.2
    */
    @Frozen@OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func add(element: T, at!: Int64): Unit
    
    /**
    * Inserts all of the elements in the specified Collection into this ArrayList, starting at the specified position.
    *
    * @param index the index of the element to insert.
    * @param elements the collection type element to insert.
    * @throws IndexOutOfBoundsException if index is out of bounds.
    * @since 0.18.2
    */
    @Frozen@OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func add(all!: Collection<T>, at!: Int64): Unit
    
    /**
    * Removes an element at the specified location from this ArrayList.
    *
    * @param index remove an Index of an Element.
    * @throws IndexOutOfBoundsException if index is out of range.
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func remove(at!: Int64): T
    
    /**
    * Removes multiple elements at the specified location from this ArrayList.
    *
    * @param range indexs of element to remove
    *
    * @throws IllegalArgumentException if range.step is not equal to 1.
    * @throws IndexOutOfBoundsException if range.start or range.end is less than zero, or range.end is greater than mySize.
    *
    * @since 0.31.3
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func remove(range: Range<Int64>): Unit
    
    /**
    * Removes all of the elements of this ArrayList that satisfy the given predicate.
    *
    * @param predicate transfer the condition for determining the deletion.
    *
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func removeIf(predicate: (T) -> Bool): Unit
    
    /**
    * Removes all of the elements from this ArrayList.
    *
    * @since 0.18.2
    *
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func clear(): Unit
    
    /**
    * Reserves capacity for at least additional more elements to be inserted in this ArrayList.
    *
    * @param additional ensure that there is sufficient capacity. additional indicates the quantity to be added.
    *
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func reserve(additional: Int64): Unit
    
    /**
    * Incoming Sort by Condition.
    *
    * @param comparator transfer the condition to be judged.
    * @param stable Whether to use stable sorting.
    *
    * @since 0.27.3
    */
    @Frozen@Deprecated[message:"Use global function `public func sort<T>(data: ArrayList<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit` in std.sort instead."]
    @!APILevel[12, atomicservice : true]
    public func sortBy(stable!: Bool, comparator!: (T, T) -> Ordering): Unit
    
    /**
    * Incoming Sort by Condition.
    *
    * @param comparator transfer the condition to be judged.
    *
    * @since 0.27.3
    */
    @Frozen@Deprecated[message:"Use global function `public func sort<T>(data: ArrayList<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit` in std.sort instead."]
    @!APILevel[12, atomicservice : true]
    public func sortBy(comparator!: (T, T) -> Ordering): Unit
    
    /**
    * Reverses the order of the elements in this ArrayList.
    *
    * @since 0.18.2
    *
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func reverse(): Unit
    
    /**
    * Operator overload slice.
    *
    * @param range transfer the range for the slice.
    *
    * @throws IllegalArgumentException if the step of range is not 1.
    * @throws IndexOutOfBoundsException if range is invalid.
    * @since 0.18.2
    *
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public operator func [](range: Range<Int64>): ArrayList<T>
    
    /**
    * The input parameter range is used as the index and the ArrayList corresponding to the index is returned.
    *
    * @param range transfer the range for the slice.
    *
    * @throws IllegalArgumentException if the step of range is not 1.
    * @throws IndexOutOfBoundsException if range is invalid.
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func slice(range: Range<Int64>): ArrayList<T>
    
    /**
    * Clone a new ArrayList.
    *
    * @return new ArrayList
    *
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func clone(): ArrayList<T>
    
    /**
    * Operator overload get.
    * An exception is reported when no index exists.
    *
    * @param index indicates the index of the get interface.
    * @return the T type of value.
    * @throws IndexOutOfBoundsException if index is out of range.
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public operator func [](index: Int64): T
    
    /**
    * Operator overload set.
    * An exception is reported when no index exists.
    *
    * @param index Index value to be set.
    * @param value the T type of element.
    *
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public operator func [](index: Int64, value!: T): Unit
    
    /**
    * Returns an iterator over the elements in this ArrayList in proper Collection.
    *
    * @return Iterator<T>.
    *
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func iterator(): Iterator<T>
    
    /**
    * Check whether the size is empty. If yes, true is returned. Otherwise, false is returned.
    *
    * @return If this size is empty, true is returned. Otherwise, false is returned.
    *
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func isEmpty(): Bool
    
    /**
    * Returns the number of elements in this ArrayList.
    *
    * @return the size.
    *
    * @since 0.18.2
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public prop size: Int64
}

/**
* The `ArrayList` conforms `Equatable` interface where `T` is `Equatable`
*
* @since 0.18.2
*/
extend<T> ArrayList<T> <: Equatable<ArrayList<T>> where T <: Equatable<T> {
    /** overloaded determination == method. */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: ArrayList<T>): Bool
    
    /** overloaded determination != method. */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: ArrayList<T>): Bool
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func contains(element: T): Bool
}

extend<T> ArrayList<T> where T <: Comparable<T> {
    /**
    * Sort data in ascending order.
    *
    * @param stable Whether to use stable sorting
    *
    * @since 0.27.3
    */
    @Frozen@Deprecated[message:"Use global function `public func sort<T>(data: ArrayList<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` in std.sort instead."]
    @!APILevel[12, atomicservice : true]
    public func sort(stable!: Bool): Unit
    
    @Frozen@Deprecated[message:"Use global function `public func sort<T>(data: ArrayList<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` in std.sort instead."]
    @!APILevel[12, atomicservice : true]
    public func sort(): Unit
    
    /**
    * Sort data in descending order.
    *
    * @param stable Whether to use stable sorting
    *
    * @since 0.27.3
    */
    @Frozen@Deprecated[message:"Use global function `public func sort<T>(data: ArrayList<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` in std.sort instead."]
    @!APILevel[12, atomicservice : true]
    public func sortDescending(stable!: Bool): Unit
    
    @Frozen@Deprecated[message:"Use global function `public func sort<T>(data: ArrayList<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` in std.sort instead."]
    @!APILevel[12, atomicservice : true]
    public func sortDescending(): Unit
}

extend<T> ArrayList<T> <: ToString where T <: ToString {
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

@!APILevel[12, atomicservice : true]
public class ArrayQueue<T> <: Queue<T> {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(capacity: Int64)
    
    @!APILevel[12, atomicservice : true]
    public func peek(): ?T
    
    @!APILevel[12, atomicservice : true]
    public func add(element: T): Unit
    
    @!APILevel[12, atomicservice : true]
    public func remove(): ?T
    
    @!APILevel[12, atomicservice : true]
    public prop size: Int64
    
    @!APILevel[12, atomicservice : true]
    public prop capacity: Int64
    
    @!APILevel[12, atomicservice : true]
    public func isEmpty(): Bool
    
    @!APILevel[12, atomicservice : true]
    public func clear(): Unit
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func iterator(): Iterator<T>
    
    @!APILevel[12, atomicservice : true]
    public func toArray(): Array<T>
    
    @!APILevel[12, atomicservice : true]
    public func reserve(additional: Int64): Unit
}

extend <T> ArrayQueue<T> <: ToString where T <: ToString {
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

@!APILevel[12, atomicservice : true]
public class ArrayStack<T> <: Stack<T> {
    /**
    * Constructs an empty stack with the specified capacity.
    *
    * @param capacity the capacity of the stack
    * @throws IllegalArgumentException if the capacity is negative
    */
    @!APILevel[12, atomicservice : true]
    public init(capacity: Int64)
    
    /**
    * Constructs an empty stack with the default capacity.
    */
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * Returns the element at the top of the stack without removing it, or null if the stack is empty.
    *
    * @return the element at the top of the stack, or null if the stack is empty
    */
    @!APILevel[12, atomicservice : true]
    public func peek(): ?T
    
    /**
    * Removes and returns the element at the top of the stack, or null if the stack is empty.
    *
    * @return the element at the top of the stack, or null if the stack is empty
    */
    @!APILevel[12, atomicservice : true]
    public func remove(): ?T
    
    /**
    * Adds an element to the top of the stack.
    *
    * @param element the element to add
    */
    @!APILevel[12, atomicservice : true]
    public func add(element: T): Unit
    
    /**
    * Returns the capacity of the stack.
    *
    * @return the capacity of the stack
    */
    @!APILevel[12, atomicservice : true]
    public prop capacity: Int64
    
    /**
    * Returns the size of the stack.
    *
    * @return the size of the stack
    */
    @!APILevel[12, atomicservice : true]
    public prop size: Int64
    
    /**
    * Returns true if the stack is empty, and false otherwise.
    *
    * @return true if the stack is empty, and false otherwise
    */
    @!APILevel[12, atomicservice : true]
    public func isEmpty(): Bool
    
    /**
    * Reserves space for additional elements in the stack.
    *
    * @param additional the number of additional elements to reserve space for
    */
    @!APILevel[12, atomicservice : true]
    public func reserve(additional: Int64): Unit
    
    /**
    * Clears the stack.
    */
    @!APILevel[12, atomicservice : true]
    public func clear(): Unit
    
    /**
    * Converts the stack to an array.
    *
    * @return an array containing the elements of the stack in the order of popping the stack
    */
    @!APILevel[12, atomicservice : true]
    public func toArray(): Array<T>
    
    /**
    * Returns an iterator over the elements in the stack.
    *
    * @return an iterator over the elements in the stack
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func iterator(): Iterator<T>
}

extend<T> ArrayStack<T> <: ToString where T <: ToString {
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

@!APILevel[12, atomicservice : true]
public class ConcurrentModificationException <: Exception {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

/**
* This func reduce is calculated from left to right using the first value as initial value.
* 
* @param operation This is the callback function that executes each value in Iterator.
* @return (Iterable<T>) -> Option<T> An iteration function accept an Iterable as its input.
* 
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func reduce<T>(operation: (T, T) -> T): (Iterable<T>) -> Option<T>

/**
* This func fold is calculated from left to right using the specified initial value.
* 
* @param initial This is initial value.
* @param operation This is the callback function that executes each value in Iterator.
* @return (Iterable<T>) -> R An iteration function accept an Iterable as its input.
* 
*/
@!APILevel[12, atomicservice : true]
public func fold<T, R>(initial: R, operation: (R, T) -> R): (Iterable<T>) -> R

/**
* This func isEmpty is used to check whether Iterable<T> is empty.
* 
* @param it An Iteratable.
* @return Bool Whether the Iterable is empty.
* 
*/
@!APILevel[12, atomicservice : true]
public func isEmpty<T>(it: Iterable<T>): Bool

/**
* This func count is used to count the number of Iterable<T>.
* 
* @param it An Iteratable.
* @return Int64 The number of the Iterable.
* 
*/
@!APILevel[12, atomicservice : true]
public func count<T>(it: Iterable<T>): Int64

/**
* This func contains is used to check whether Iterable<T> contains a specified element.
* 
* @param element The specified element.
* @return (Iterable<T>) -> Bool An iteration function accept an Iterable as its input.
* 
*/
@!APILevel[12, atomicservice : true]
public func contains<T>(element: T): (Iterable<T>) -> Bool where T <: Equatable<T>

/**
* This func max is used to find the maximum value of Iterable<T>.
* 
* @param it An Iteratable.
* @return Option<T> The maximum value.
* 
*/
@!APILevel[12, atomicservice : true]
public func max<T>(it: Iterable<T>): Option<T> where T <: Comparable<T>

/**
* This func min is used to find the minimum value of Iterable<T>.
* 
* @param it An Iteratable.
* @return Option<T> The minimum value.
* 
*/
@!APILevel[12, atomicservice : true]
public func min<T>(it: Iterable<T>): Option<T> where T <: Comparable<T>

/**
* This func all is used to check whether all elements pass the test.
* 
* @param predicate The function used to test each element.
* @return (Iterable<T>) -> Bool An iteration function accept an Iterable as its input.
* 
*/
@!APILevel[12, atomicservice : true]
public func all<T>(predicate: (T) -> Bool): (Iterable<T>) -> Bool

/**
* This func any is used to check that at least one element passes the test.
* 
* @param predicate The function used to test each element.
* @return (Iterable<T>) -> Bool An iteration function accept an Iterable as its input.
* 
*/
@!APILevel[12, atomicservice : true]
public func any<T>(predicate: (T) -> Bool): (Iterable<T>) -> Bool

/**
* This func none is used to check whether all elements fail the test.
* 
* @param predicate The function used to test each element.
* @return (Iterable<T>) -> Bool An iteration function accept an Iterable as its input.
* 
*/
@!APILevel[12, atomicservice : true]
public func none<T>(predicate: (T) -> Bool): (Iterable<T>) -> Bool

/**
* This func first is used to get the first element of the Iterator.
* 
* @param it An Iteratable.
* @return Option<T> The first element.
* 
*/
@!APILevel[12, atomicservice : true]
public func first<T>(it: Iterable<T>): Option<T>

/**
* This func last is used to get the last element of the Iterator.
* 
* @param it An Iteratable.
* @return Option<T> The last element.
* 
*/
@!APILevel[12, atomicservice : true]
public func last<T>(it: Iterable<T>): Option<T>

/**
* This func at function is used to get the specified index element of the iterator.
* 
* @param n The specified index.
* @return (Iterable<T>) -> Option<T> An iteration function accept an Iterable as its input.
* 
*/
@!APILevel[12, atomicservice : true]
public func at<T>(n: Int64): (Iterable<T>) -> Option<T>

/**
* This func forEach function is used to perform a specific function on each element of Iterable.
* 
* @param action The function executed by each element.
* @return (Iterable<T>) -> Unit An iteration function accept an Iterable as its input.
* 
*/
@!APILevel[12, atomicservice : true]
public func forEach<T>(action: (T) -> Unit): (Iterable<T>) -> Unit

/**
* The hashmap is implemented based on the hash algorithm.
* You can use put key-value to obtain the value and get key to obtain the value.
*
* @since 0.18.4
*/
@!APILevel[12, atomicservice : true]
public class HashMap<K, V> <: Map<K, V> where K <: Hashable & Equatable<K> {
    /**
    * Initializes an empty HashMap with a default initial DEFAULT_CAPACITY (16) and a default load factor.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * Initializes a HashMap with an incoming iterator for initialization.
    *
    * @param elements an incoming iterator is initialized.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init(elements: Collection<(K, V)>)
    
    /**
    * Initializes a hashmap with an incoming list for initialization.
    *
    * @param elements an incoming list is initialized.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init(elements: Array<(K, V)>)
    
    /**
    * Initializes a HashMap with an incoming @p DEFAULT_CAPACITY for initialization.
    *
    * @param capacity an incoming capacity is initialized.
    *
    * @throws IllegalArgumentException if capacity is less than zero
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init(capacity: Int64)
    
    /**
    * Initializes a hash map with an incoming size and an initial element for initialization.
    *
    * @param size the size of the incoming initial element.
    * @param initElement an incoming initElement is initialized.
    *
    * @throws IllegalArgumentException if size is less than zero
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init(size: Int64, initElement: (Int64) -> (K, V))
    
    /**
    * Returns value Iterator<T> - Iterator<T> type, which can be traversed using an iterator.
    *
    * @param key transfer key to obtain the value.
    * @return the value corresponding to the return key is encapsulated with option.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func get(key: K): ?V
    
    /**
    * Associates the specified @p value with the specified @p key in this map.
    * If you map a mapping that previously contained a key, the old value is replaced.
    *
    * @param key the key to put.
    * @param value the value to assign.
    * @return If the key exists before the assignment, the value before the assignment is encapsulated with Option.
    * Otherwise, return to Option<V>.None
    *
    * @since 0.18.4
    */
    @Frozen@OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func add(key: K, value: V): Option<V>
    
    /**
    * A view of a single entry in a hashmap, which can be empty or has a value.
    *
    * @param key the key to put.
    * @return If the hashmap has this key, a view with a value is returned. Otherwise, an empty view is returned.
    *
    * @since 0.45.1
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func entryView(key: K): MapEntryView<K, V>
    
    /**
    * Transfer specified elements for traversal and assign values in sequence.
    * If you map a mapping that previously contained a key, the old value is replaced.
    *
    * @param elements the element passing in for traversal assignment.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func add(all!: Collection<(K, V)>): Unit
    
    /**
    * Removes the key-value pair corresponding to the key based on the specified key from this mapping, if one exists.
    *
    * @param key pass in the key to be deleted.
    * @return removed element
    *
    * @since 0.18.4
    */
    @Frozen@OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func remove(key: K): Option<V>
    
    /**
    * Traverse the set of transferred keys and delete them based on the traversal result.
    *
    * @param keys pass in the collection to traverse.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func remove(all!: Collection<K>): Unit
    
    /**
    * Transfer a lambda expression and delete the corresponding key value if the condition is met.
    *
    * @param predicate transfer a lambda expression for judgment.
    *
    * @since 0.18.4
    */
    @Frozen@OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func removeIf(predicate: (K, V) -> Bool): Unit
    
    /**
    * Clear all key-value pairs.
    *
    * @since 0.18.4
    */
    @Frozen@OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func clear(): Unit
    
    /**
    * Reserves capacity for at least additional more elements to be inserted in this hashmap.
    * If the additional parameter is negative, the hash map does not change.
    * If the additional parameter plus the size of the hashmap is smaller than the capacity of the  array, the hashmap does not change.
    *
    * @param additional ensure that there is sufficient capacity. additional indicates the quantity to be added.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func reserve(additional: Int64): Unit
    
    /**
    * Returns the capacity of the hashmap.
    * The return value is the size of the array, not the size of the elements that can be accommodated.
    *
    * @return the capacity of the hashmap.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public prop capacity: Int64
    
    /**
    * Checks whether the mapping relationship corresponding to the collection key exists in this mapping.
    *
    * @param keys transfer the collection key to be judged.
    * @return bool returns true if exists; otherwise, false.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func contains(all!: Collection<K>): Bool
    
    /**
    * Checks whether the mapping relationship corresponding to the specified key exists in this mapping.
    *
    * @param key transfer the key to be judged.
    * @return bool returns true if exists; otherwise, false.
    *
    * @since 0.18.4
    */
    @Frozen@OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func contains(key: K): Bool
    
    /**
    * Copy a HashMap.
    *
    * @return a clone value of HashMap.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func clone(): HashMap<K, V>
    
    /**
    * Returns the Set view of all keys in this HashMap.
    *
    * @return the Set view of the keys.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func keys(): EquatableCollection<K>
    
    /**
    * Returns the Set view of all values in this HashMap.
    *
    * @return the list view of the values.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func values(): Collection<V>
    
    /**
    * An exception is reported when the get operator is overloaded and the key does not exist.
    *
    * @param key transfer the value for judgment.
    * @return the value corresponding to the key.
    *
    * @throws NoneValueException if value does not exist.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public operator func [](key: K): V
    
    /**
    * The operator overloads the set. If the key does not exist, an exception is reported.
    *
    * @param key transfer the value for judgment.
    * @param value transfer the value to be set.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public operator func [](key: K, value!: V): Unit
    
    /**
    * Returns sizes of key-value.
    *
    * @return sizes of key-value.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public prop size: Int64
    
    /**
    * Returns iterator of hashmap.
    *
    * @return iterator of hashmap.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func iterator(): HashMapIterator<K, V>
    
    /**
    * Check whether the size is empty. If yes, true is returned. Otherwise, false is returned.
    *
    * @return bool if yes, true is returned. Otherwise, false is returned.
    *
    * @since 0.18.4
    */
    @Frozen@OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func isEmpty(): Bool
    
    /**
    * Returns the element in this Map as an Array.
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func toArray(): Array<(K, V)>
}

/**
*
*
* @since 0.18.4
*/
extend<K, V> HashMap<K, V> <: ToString where V <: ToString, K <: ToString {
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* This is the function of hashmap equal and not equal. If the keys and values are same for two hasmaps, then they are equal.
*
* Returns true if the two hashmaps are equal for ==. Return true if the two hashmaps are not equal for !=.
*
* @since 0.18.4
*/
extend<K, V> HashMap<K, V> <: Equatable<HashMap<K, V>> where V <: Equatable<V> {
    @Frozen
    @!APILevel[12, atomicservice : true]
    public operator func ==(right: HashMap<K, V>): Bool
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public operator func !=(right: HashMap<K, V>): Bool
}

/**
* This is a hashmap iterator used to iterate effects.
*
* @since 0.18.4
*/
@!APILevel[12, atomicservice : true]
public class HashMapIterator<K, V> <: Iterator<(K, V)> where K <: Hashable & Equatable<K> {
    /**
    * Initialize the iterator and transfer the hashmap.
    *
    * @param map the hashmap to be transferred.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init(map: HashMap<K, V>)
    
    /**
    * Iterates the bucketIndex element. The return type is option, which contains key and value.
    *
    * @return type is option, which contains key and value.
    *
    * @throws ConcurrentModificationException if lockVersion is not equal to "data.version()".
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func next(): ?(K, V)
    
    /**
    * Remove the element returned by the next function of this iterator.
    * This method can be called only once when the next function is called.
    *
    * @see Currently, the Cangjie modifier does not implement the function visible in the package,
    * and the API function modified by public is insecure. It is recommended that this method be used temporarily.
    * This method can be improved after the function visible in the package is supported in the future.
    * In the future, the  function should be used to implement the function.
    *
    * @throws ConcurrentModificationException if lockVersion is not equal to "data.version()".
    *
    * @since 0.24.1
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func remove(): Option<(K, V)>
}

/**
* This class implements the Set interface. The underlying structure is a hash table.
* (Actually, It is an instance of encapsulating HashMap.) . It is not an ordered set.
* Please note that, this class is asynchronous. When multiple threads access this class 
* at the same time and at least one thread modifies it,it may cause thread insecureness.
*
* @see Collection
* @see Set
* @see HashMap
*
* @since 0.18.4
*/
@!APILevel[12, atomicservice : true]
public class HashSet<T> <: Set<T> where T <: Hashable & Equatable<T> {
    /**
    * Constructs a new, empty set; the backing @p HashSet instance has
    * default initial capacity (16) and load factor (0.75).
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * Construct a HashSet with an incoming iterator for initialization.
    *
    * @param elements an incoming iterator is initialized.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init(elements: Collection<T>)
    
    /**
    * Construct a HashSet with an incoming array for initialization.
    *
    * @param elements an incoming array is initialized.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init(elements: Array<T>)
    
    /**
    * Construct a HashSet with an incoming @p myCapacity for initialization.
    *
    * @param capacity myCapacity an incoming capacity is initialized.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init(capacity: Int64)
    
    /**
    * Constructs a hash map with an incoming size and an initial element for initialization.
    *
    * @param size the size of the incoming initial element.
    * @param initElement an incoming initElement is initialized.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public init(size: Int64, initElement: (Int64) -> T)
    
    /**
    * Checks whether the mapping relationship corresponding to the specified key exists in this mapping.
    *
    * @param element key transfer the key to be judged.
    * @return bool returns true if exists; otherwise, false.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func contains(element: T): Bool
    
    /**
    * Check whether the set is a subset of other.
    *
    * @param other a set of the set type.
    * @return bool returns true if it is a subset, false otherwise.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func subsetOf(other: ReadOnlySet<T>): Bool
    
    /**
    * Checks whether the mapping relationship corresponding to the collection key exists in this mapping.
    *
    * @param elements keys transfer the collection key to be judged.
    * @return bool returns true if exists; otherwise, false.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func contains(all!: Collection<T>): Bool
    
    /**
    * Add element operation. If the element already exists, it will not be added.
    *
    * @param element the key to put.
    * @return bool returns true if element is added; otherwise, false.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func add(element: T): Bool
    
    /**
    * Removes the key-value pair corresponding to the key based on the specified key from this mapping, if one exists.
    *
    * @param element key pass in the key to be deleted.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func remove(element: T): Bool
    
    /**
    * Transfer specified elements for traversal and assign values in sequence.
    * If you map a mapping that previously contained a key, the old value is replaced.
    *
    * @param elements the element passing in for traversal assignment.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func add(all!: Collection<T>): Unit
    
    /**
    * Traverse the set of transferred keys and delete them based on the traversal result.
    *
    * @param elements keys pass in the collection to traverse.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func remove(all!: Collection<T>): Unit
    
    /**
    * Transfer a lambda expression and delete the corresponding key value if the condition is met.
    *
    * @param predicate transfer a lambda expression for judgment.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func removeIf(predicate: (T) -> Bool): Unit
    
    /**
    * Clear all key-value pairs.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func clear(): Unit
    
    /**
    * Retain only duplicate T.
    * Removes all elements from this collection that are not contained
    * in the specified collection. This function depends on the contains function.
    * Please note that. If the incoming interface is a subtype of a malicious implementation.
    * We do not guarantee the correctness of results for retaining duplicate elements.
    *
    * @param elements collections to be saved.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func retain(all!: Set<T>): Unit
    
    /**
    * Clone of hashset.
    *
    * @return a clone value of HashSet.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func clone(): HashSet<T>
    
    /**
    * Ensure that there is sufficient capacity. additional indicates the quantity to be added.
    * If the additional parameter is negative, the hash map does not change.
    * If the additional parameter plus the size of the hashset is smaller than the capacity of the  array, the hashset does not change.
    *
    * @param additional size of the increment.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func reserve(additional: Int64): Unit
    
    /**
    * Returns the capacity of the hashSet.
    *
    * @return the capacity of the hashSet.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public prop capacity: Int64
    
    /**
    * Returns iterator of hashSet.
    *
    * @return iterator of Keys.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func iterator(): Iterator<T>
    
    /**
    * Returns sizes of key-value.
    *
    * @return sizes of key-value.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public prop size: Int64
    
    /**
    * Check whether the size is empty. If yes, true is returned. Otherwise, false is returned.
    *
    * @return bool if yes, true is returned. Otherwise, false is returned.
    *
    * @since 0.18.4
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func isEmpty(): Bool
    
    /**
    * Returns the element in this HashSet as an Array.
    *
    * @throws IndexOutOfBoundsException if "mySize" is out of bound.
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func toArray(): Array<T>
    
    /**
    * Computes the intersection of this set and another set.
    * Returns a new set containing only the elements that are present in both sets.
    *
    * - Parameter other: Another set to intersect with.
    * - Returns: A new `HashSet` containing elements common to both sets.
    *
    * Example:
    * ```
    * let set1 = HashSet([1, 2, 3])
    * let set2 = HashSet([2, 3, 4])
    * let result = set1 & set2 // result is [2, 3]
    * ```
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public operator func &(other: ReadOnlySet<T>): HashSet<T>
    
    /**
    * Computes the union of this set and another set.
    * Returns a new set containing all unique elements from both sets.
    *
    * - Parameter other: Another set to unite with.
    * - Returns: A new `HashSet` containing all unique elements from both sets.
    *
    * Example:
    * ```
    * let set1 = HashSet([1, 2, 3])
    * let set2 = HashSet([3, 4, 5])
    * let result = set1 | set2 // result is [1, 2, 3, 4, 5]
    * ```
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public operator func |(other: ReadOnlySet<T>): HashSet<T>
    
    /**
    * Computes the difference of this set and another set.
    * Returns a new set containing elements that are present in this set but not in the other set.
    *
    * - Parameter other: Another set to subtract from this set.
    * - Returns: A new `HashSet` containing elements unique to this set.
    *
    * Example:
    * ```
    * let set1 = HashSet([1, 2, 3])
    * let set2 = HashSet([3, 4, 5])
    * let result = set1 - set2 // result is [1, 2]
    * ```
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public operator func -(other: ReadOnlySet<T>): HashSet<T>
}

/**
* Defines the HashSet inherits the Equalable method and determines whether = = and! = methods.
*
* @since 0.18.4
*/
extend<T> HashSet<T> <: Equatable<HashSet<T>> {
    /** overloaded determination equal function. */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: HashSet<T>): Bool
    
    /** overloaded determination unequal function. */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: HashSet<T>): Bool
}

extend<T> HashSet<T> <: ToString where T <: ToString {
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

@!APILevel[12, atomicservice : true]
public class LinkedListNode<T> {
    @Frozen
    @!APILevel[12, atomicservice : true]
    public mut prop value: T
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public prop next: Option<LinkedListNode<T>>
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public prop prev: Option<LinkedListNode<T>>
}

@!APILevel[12, atomicservice : true]
public class LinkedList<T> <: Collection<T> {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(elements: Collection<T>)
    
    @!APILevel[12, atomicservice : true]
    public init(elements: Array<T>)
    
    @!APILevel[12, atomicservice : true]
    public init(size: Int64, initElement: (Int64)-> T)
    
    @!APILevel[12, atomicservice : true]
    public prop size: Int64
    
    @!APILevel[12, atomicservice : true]
    public prop first: ?T
    
    @!APILevel[12, atomicservice : true]
    public prop last: ?T
    
    @!APILevel[12, atomicservice : true]
    public prop firstNode: ?T
    
    @!APILevel[12, atomicservice : true]
    public prop lastNode: ?T
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func addFirst(element: T): LinkedListNode<T>
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func addLast(element: T): LinkedListNode<T>
    
    @!APILevel[12, atomicservice : true]
    public func toArray(): Array<T>
    
    @!APILevel[12, atomicservice : true]
    public func addBefore(node: LinkedListNode<T>, element: T): LinkedListNode<T>
    
    @!APILevel[12, atomicservice : true]
    public func addAfter(node: LinkedListNode<T>, element: T): LinkedListNode<T>
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func remove(node: LinkedListNode<T>): T
    
    @!APILevel[12, atomicservice : true]
    public func removeIf(predicate: (T)-> Bool): Unit
    
    @!APILevel[12, atomicservice : true]
    public func clear(): Unit
    
    @!APILevel[12, atomicservice : true]
    public func reverse(): Unit
    
    @!APILevel[12, atomicservice : true]
    public func isEmpty(): Bool
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func iterator(): Iterator<T>
    
    @!APILevel[12, atomicservice : true]
    public func removeFirst(): ?T
    
    @!APILevel[12, atomicservice : true]
    public func removeLast(): ?T
    
    @!APILevel[12, atomicservice : true]
    public func nodeAt(index: Int64): Option<LinkedListNode<T>>
    
    @!APILevel[12, atomicservice : true]
    public func splitOff(node: LinkedListNode<T>): LinkedList<T>
    
    @!APILevel[12, atomicservice : true]
    public func forward(mark: LinkedListNode<T>): Iterator<T>
    
    @!APILevel[12, atomicservice : true]
    public func backward(mark: LinkedListNode<T>): Iterator<T>
}

extend<T> LinkedList<T> <: ToString where T <: ToString {
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

extend<T> LinkedList<T> <: Equatable<LinkedList<T>> where T <: Equatable<T> {
    /** overloaded determination == method. */
    @!APILevel[12, atomicservice : true]
    public operator func ==(right: LinkedList<T>): Bool
    
    /** overloaded determination != method. */
    @!APILevel[12, atomicservice : true]
    public operator func !=(right: LinkedList<T>): Bool
}

@!APILevel[12, atomicservice : true]
public interface ReadOnlyList<T> <: Collection<T> {
}

@!APILevel[12, atomicservice : true]
public interface List<T> <: ReadOnlyList<T> {
}

@!APILevel[12, atomicservice : true]
public interface MapEntryView<K, V> {
    @!APILevel[12, atomicservice : true]
    public prop key: K
    
    @!APILevel[12, atomicservice : true]
    public mut prop value: ?V
}

@!APILevel[12, atomicservice : true]
public interface ReadOnlyMap<K, V> <: Collection<(K, V)> {
}

/**
* This interface is a key-value pair that does not guarantee the sequence of elements.
* That is, the sequence of adding elements is different from the sequence of obtaining elements.
*
* @since 0.24.2
*/
@!APILevel[12, atomicservice : true]
public interface Map<K, V> <: ReadOnlyMap<K, V> {
}

/**
* This interface is a collection of keys returned by a key-value pair.
*
* @since 0.24.2
*/
@!APILevel[12, atomicservice : true]
public interface EquatableCollection<T> <: Collection<T> {
}

@!APILevel[12, atomicservice : true]
public interface OrderedMap<K, V> <: Map<K, V> {
    @!APILevel[12, atomicservice : true]
    public prop first: ?(K, V)
    
    @!APILevel[12, atomicservice : true]
    public prop last: ?(K, V)
    
    @!APILevel[12, atomicservice : true]
    public func removeFirst(): ?(K, V)
    
    @!APILevel[12, atomicservice : true]
    public func removeLast(): ?(K, V)
    
    @!APILevel[12, atomicservice : true]
    public func backward(mark: K, inclusive!: Bool): Iterator<(K, V)>
    
    @!APILevel[12, atomicservice : true]
    public func forward(mark: K, inclusive!: Bool): Iterator<(K, V)>
}

/**
* This func enumerate returns an Iterator with index.
*
* @param it An Iteratable.
* @return Iterator<Int64*T> An Iterator with index.
*
*/
@!APILevel[12, atomicservice : true]
public func enumerate<T>(it: Iterable<T>): Iterator<(Int64, T)>

/**
* This func filter is used to filter Iterator<T>.
*
* @param predicate Filter condition.
* @return (Iterable<T>) -> Iterator<T> A filter function accept an Iterable.
*
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func filter<T>(predicate: (T) -> Bool): (Iterable<T>) -> Iterator<T>

/**
* This func filterMap is used to filter and map Iterator<T>.
*
* @param transform Filter and map function.
* @return (Iterable<T>) -> Iterator<R> A filterMap function accept an Iterable.
*
*/
@!APILevel[12, atomicservice : true]
public func filterMap<T, R>(transform: (T) -> ?R): (Iterable<T>) -> Iterator<R>

/**
* This func map is used to Convert Iterable<T> to Iterator<R>.
*
* @param transform The callback function, that converts T to R.
* @return (Iterable<T>) -> Iterator<R> A map function accept an Iterable as its input.
*
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func map<T, R>(transform: (T) -> R): (Iterable<T>) -> Iterator<R>

/**
* This func flatten is used to flatten nested structure.
*
* @param it An Iteratable.
* @return Iterator<R>, an Iterator removing one level of indirection.
*
*/
@!APILevel[12, atomicservice : true]
public func flatten<T, R>(it: Iterable<T>): Iterator<R> where T <: Iterable<R>

/**
* This func flatMap is used to map and flatten Iterator.
*
* @param transform The callback function, that converts T to Iterable<R>.
* @return (Iterable<T>) -> Iterator<R> A map-flatten function accept an Iterable as its input.
*
*/
@!APILevel[12, atomicservice : true]
public func flatMap<T, R>(transform: (T) -> Iterable<R>): (Iterable<T>) -> Iterator<R>

/**
* This func zip is used to zip two Iterables into one (mainly the short one).
*
* @param other An Iteratable.
* @return (Iterable<T>) -> Iterator<(T, R)> A zip function accept an Iterable as its input.
*
*/
@!APILevel[12, atomicservice : true]
public func zip<T, R>(other: Iterable<R>): (Iterable<T>) -> Iterator<(T, R)>

/**
* This func concat is used to concat two Iterables into one.
*
* @param other An Iteratable.
* @return (Iterable<T>) -> Iterator<T> A concat function accept an Iterable as its input.
*
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func concat<T>(other: Iterable<T>): (Iterable<T>) -> Iterator<T>

/**
* This func skip is used to skip count in Iterable<T>.
* if count < 0, an exception will be throwed.
* if count == 0, no operations will be done.
* if 0 < count < size, return the remainning elements after skipping.
* if count >= size, skip all elements and return an empty Iterator.
*
* @param count This is an Int64,number of skip.
* @return (Iterable<T>) -> Iterator<T> A skip function accept an Iterable as its input.
* @throws IllegalArgumentException if skip count is less than 0.
*/
@!APILevel[12, atomicservice : true]
public func skip<T>(count: Int64): (Iterable<T>) -> Iterator<T>

/**
* This func take is used to take count in Iterable<T>.
*
* @param count This is an Int64
* @return (Iterable<T>) -> Iterator<T> A take function accept an Iterable as its input.
* @throws IllegalArgumentException if count is less than 0.
*/
@!APILevel[12, atomicservice : true]
public func take<T>(count: Int64): (Iterable<T>) -> Iterator<T>

/**
* This func step is used to step count in Iterable<T>.
*
* @param count This is an Int64, number of step.
* @return (Iterable<T>) -> Iterator<T> A step function accept an Iterable as its input.
* @throws IllegalArgumentException if step count is less than 0.
*/
@!APILevel[12, atomicservice : true]
public func step<T>(count: Int64): (Iterable<T>) -> Iterator<T>

/**
* This func inspect performs an extra operation on the current element each time when use next().
*
* @param action The function executed by each element.
* @return (Iterable<T>) -> Iterator<T> An iteration function accept an Iterable as its input.
* 
*/
@!APILevel[12, atomicservice : true]
public func inspect<T>(action: (T) -> Unit): (Iterable<T>) -> Iterator<T>

/**
* Interface for a double-ended queue.
*
* @param <T> The type of elements in the queue.
*/
@!APILevel[12, atomicservice : true]
public interface Deque<T> <: Collection<T> {
}

/**
* Queue interface that inherits from the Collection interface.
* @param <T> The type of elements in the queue.
*/
@!APILevel[12, atomicservice : true]
public interface Queue<T> <: Collection<T> {
}

@!APILevel[12, atomicservice : true]
public interface ReadOnlySet<T> <: Collection<T> {
}

/**
* This interface is a collection that does not guarantee the sequence of elements.
* That is, the sequence of adding elements is different from the sequence of obtaining elements.
*
* @since 0.18.4
*/
@!APILevel[12, atomicservice : true]
public interface Set<T> <: ReadOnlySet<T> {
}

@!APILevel[12, atomicservice : true]
public interface OrderedSet<T> <: Set<T> {
    @!APILevel[12, atomicservice : true]
    public prop first: ?T
    
    @!APILevel[12, atomicservice : true]
    public prop last: ?T
    
    @!APILevel[12, atomicservice : true]
    public func removeFirst(): ?T
    
    @!APILevel[12, atomicservice : true]
    public func removeLast(): ?T
    
    @!APILevel[12, atomicservice : true]
    public func backward(mark: T, inclusive!: Bool): Iterator<T>
    
    @!APILevel[12, atomicservice : true]
    public func forward(mark: T, inclusive!: Bool): Iterator<T>
}

/**
* Stack interface, inherits from Collection interface
* @param <T> Generic parameter
*/
@!APILevel[12, atomicservice : true]
public interface Stack<T> <: Collection<T> {
}

/**
* This function is used to convert Iterable<T> to String.
*
* @param delimiter Delimiter used in concatenation.
* @return String A new string containing all elements's string value in original iterator.
*
*/
@!APILevel[12, atomicservice : true]
public func collectString<T>(delimiter!: String = ""): (Iterable<T>) -> String where T <: ToString

/**
* This function is used to convert Iterable<T> to Array<T>.
*
* @param it This is an Iterable.
* @return Array<T> A new Array<T> containing all elements in original iterator.
*
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func collectArray<T>(it: Iterable<T>): Array<T>

/**
* This function is used to convert Iterable<T> to ArrayList<T>.
*
* @param it This is an Iterable.
* @return ArrayList<T> A new ArrayList<T> containing all elements in original iterator.
*
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func collectArrayList<T>(it: Iterable<T>): ArrayList<T>

/**
* This function is used to convert Iterable<T> to HashSet<T>.
*
* @param it This is an Iterable.
* @return HashSet<T> A new HashSet<T> containing all elements in original iterator.
*
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func collectHashSet<T>(it: Iterable<T>): HashSet<T> where T <: Hashable & Equatable<T>

/**
* This function is used to convert Iterable<(K, V)> to HashMap<K, V>.
*
* @param it This is an Iterable.
* @return HashMap<K, V> A new HashMap<K, V> containing all elements in original iterator.
*
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func collectHashMap<K, V>(it: Iterable<(K, V)>): HashMap<K, V> where K <: Hashable & Equatable<K>

@!APILevel[12, atomicservice : true]
public class TreeMap<K, V> <: OrderedMap<K, V> where K <: Comparable<K> {
    /**
    * Initializes an empty TreeMap.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * Initializes a TreeMap with an incoming iterator for initialization.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public init(elements: Collection<(K, V)>)
    
    /**
    * Initializes a TreeMap with an incoming list for initialization.
    *
    * @param elements an incoming list is initialized.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public init(elements: Array<(K, V)>)
    
    /**
    * Initializes a TreeMap with an incoming size and an initial element for initialization.
    *
    * @param size the size of the incoming initial element.
    * @param initElement an incoming initElement is initialized.
    *
    * @throws IllegalArgumentException if size is less than zero 
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public init(size: Int64, initElement: (Int64) -> (K, V))
    
    /**
    * Returns the value to which the specified key is mapped.
    *
    * @param key transfer key to obtain the value.
    * @return the value corresponding to the return key is encapsulated with Option.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func get(key: K): ?V
    
    /**
    * Checks whether the mapping relationship corresponding to the specified key exists in this mapping.
    *
    * @param key transfer the key to be judged.
    * @return bool returns true if exists; otherwise, false.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func contains(key: K): Bool
    
    /**
    * Checks whether the mapping relationship corresponding to the collection key exists in this mapping.
    *
    * @param keys transfer the collection key to be judged.
    * @return bool returns true if exists; otherwise, false.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func contains(all!: Collection<K>): Bool
    
    /**
    * Associates the specified @p value with the specified @p key in this map.
    * If you map a mapping that previously contained a key, the old value is replaced.
    *
    * @param key the key to put.
    * @param value the value to assign.
    * @return If the key exists before the assignment, the value before the assignment is encapsulated with Option.
    * Otherwise, return to Option<V>.None
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func add(key: K, value: V): Option<V>
    
    /**
    * Transfer specified elements for traversal and assign values in sequence.
    * If you map a mapping that previously contained a key, the old value is replaced.
    *
    * @param elements the element passing in for traversal assignment.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func add(all!: Collection<(K, V)>): Unit
    
    /**
    * Removes the key-value pair corresponding to the key based on the specified key from this mapping, if one exists.
    *
    * @param key pass in the key to be deleted.
    * @return removed element
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func remove(key: K): Option<V>
    
    /**
    * Traverse the set of transferred keys and delete them based on the traversal result.
    *
    * @param keys pass in the collection to traverse.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func remove(all!: Collection<K>): Unit
    
    /**
    * Transfer a lambda expression and delete the corresponding key value if the condition is met.
    *
    * @param predicate transfer a lambda expression for judgment.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func removeIf(predicate: (K, V) -> Bool): Unit
    
    /**
    * Clear all key-value pairs.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func clear(): Unit
    
    /**
    * Copy a TreeMap.
    *
    * @return a clone value of TreeMap.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func clone(): TreeMap<K, V>
    
    /**
    * An exception is reported when the get operator is overloaded and the key does not exist.
    *
    * @param key transfer the value for judgment.
    * @return the value corresponding to the key.
    *
    * @throws NoneValueException if value does not exist.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public operator func [](key: K): V
    
    /**
    * The operator overloads the set. If the key does not exist, an exception is reported.
    *
    * @param key transfer the value for judgment.
    * @param value transfer the value to be set.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public operator func [](key: K, value!: V): Unit
    
    /**
    * Returns the Set view of all keys in this TreeMap.
    *
    * @return the Set view of the keys.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func keys(): EquatableCollection<K>
    
    /**
    * Returns the Set view of all values in this TreeMap.
    *
    * @return the list view of the values.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func values(): Collection<V>
    
    /**
    * Returns sizes of key-value.
    *
    * @return sizes of key-value.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public prop size: Int64
    
    /**
    * Check whether the size is empty. If yes, true is returned. Otherwise, false is returned.
    *
    * @return bool if yes, true is returned. Otherwise, false is returned.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func isEmpty(): Bool
    
    /**
    * Get an iterator.
    *
    * @return type is iterator, which contains key and value.
    *
    * @since 0.43.1
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func iterator(): Iterator<(K, V)>
    
    /**
    * Gets the first node of the TreeMap.
    *
    * @return If the first node exists, the key-value pair stored on the first node is returned. Otherwise, None is returned.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public prop first: ?(K, V)
    
    /**
    * Delete the first node of the TreeMap.
    *
    * @return If there is a first node, return the deletion and return the key-value pair it stores; Otherwise, None is returned.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func removeFirst(): ?(K, V)
    
    /**
    * Gets the last node of the TreeMap.
    *
    * @return If the last node exists, the key-value pair stored on the first node is returned. Otherwise, None is returned.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public prop last: ?(K, V)
    
    /**
    * Delete the last node of the TreeMap.
    *
    * @return If there is a last node, return the deletion and return the key-value pair it stores; Otherwise, None is returned.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func removeLast(): ?(K, V)
    
    /**
    * Generate an iterator in positive order from the current node to the end of the bound node.
    *
    * @param bound - Passed key
    * @param inclusive: indicates whether the input key is included. The default value is true, indicating that the input key is included.
    *
    * @return value Iterator<(K, V)> - Returns an iterator in positive order from the current node to the end of the bound.
    *
    * @throws ConcurrentModificationException if lockVersion is not equal to "this.map.version()".
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func backward(mark: K, inclusive!: Bool = true): Iterator<(K, V)>
    
    /**
    * Generate an iterator in positive order from the current node to the end of the bound node.
    *
    * @param bound - Passed key
    * @param inclusive: indicates whether the input key is included. The default value is true, indicating that the input key is included.
    *
    * @return value Iterator<(K, V)> - Returns an iterator in positive order from the current node to the end of the bound.
    *
    * @throws ConcurrentModificationException if lockVersion is not equal to "this.map.version()".
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func forward(mark: K, inclusive!: Bool = true): Iterator<(K, V)>
    
    @!APILevel[12, atomicservice : true]
    public func entryView(k: K): MapEntryView<K, V>
}

extend<K, V> TreeMap<K, V> <: ToString where V <: ToString, K <: ToString & Comparable<K> {
    /**
    * Returns the string form of a TreeMap
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

extend<K, V> TreeMap<K, V> <: Equatable<TreeMap<K, V>> where V <: Equatable<V> {
    /**
    * Compares whether two TreeMaps are equal.
    *
    * @param that Another TreeMap instance.
    * @return If two TreeMaps are equal, true is returned. Otherwise, false is returned.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public operator func ==(right: TreeMap<K, V>): Bool
    
    /**
    * Compares whether two TreeMaps are not equal.
    *
    * @param that Another TreeMap instance.
    * @return If two TreeMaps are equal, false is returned. Otherwise, true is returned.
    *
    * @since 0.43.1
    */
    @!APILevel[12, atomicservice : true]
    public operator func !=(right: TreeMap<K, V>): Bool
}

@!APILevel[12, atomicservice : true]
public class TreeSet<T> <: OrderedSet<T> where T <: Comparable<T> {
    /**
    * Returns the size.
    *
    * @return size.
    *
    */
    @!APILevel[12, atomicservice : true]
    public prop size: Int64
    
    /**
    * Gets the first node of the TreeSet.
    *
    * @return If the first node exists, the element on the first node is returned. Otherwise, None is returned.
    *
    */
    @!APILevel[12, atomicservice : true]
    public prop first: ?T
    
    /**
    * Gets the last node of the TreeSet.
    *
    * @return If the last node exists, the element on the last node is returned. Otherwise, None is returned.
    *
    */
    @!APILevel[12, atomicservice : true]
    public prop last: ?T
    
    /**
    * Constructs a new, empty set.
    */
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * Construct a TreeSet with an incoming collection for initialization.
    *
    * @param elements an incoming collection.
    *
    */
    @!APILevel[12, atomicservice : true]
    public init(elements: Collection<T>)
    
    /**
    * Constructs a tree map with an incoming size and an initial element for initialization.
    *
    * @param size the size of the incoming initial element.
    * @param initElement an incoming initElement is initialized.
    *
    */
    @!APILevel[12, atomicservice : true]
    public init(size: Int64, initElement: (Int64) -> T)
    
    /**
    * Statically generate an TreeSet with the specified Array. 
    *
    * @param elements input element type array.
    * @return new TreeSet
    */
    @!APILevel[12, atomicservice : true]
    public static func of(elements: Array<T>): TreeSet<T>
    
    /**
    * Checks whether the specified element is contained in this set.
    *
    * @param element elements to judge.
    * @return bool returns true if contained; otherwise, false.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func contains(element: T): Bool
    
    /**
    * Checks whether the specified collection of elements is all contained in this set.
    *
    * @param all collection of elements to be judged.
    * @return bool returns true if all elements are included, false otherwise.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func contains(all!: Collection<T>): Bool
    
    /**
    * Add element operation. If the element already exists, it will not be added.
    *
    * @param element the element to put.
    * @return Bool returns true if element is added; otherwise, false.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func add(element: T): Bool
    
    /**
    * All elements in the incoming collection are added.
    *
    * @param all the collection.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func add(all!: Collection<T>): Unit
    
    /**
    * Removes a specified element from a set.
    *
    * @param element a specified element.
    * @return Bool if the removal is successful, true is returned. If the removal fails or the element does not exist, false is returned.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func remove(element: T): Bool
    
    /**
    * Traversal removal of elements in this collection.
    *
    * @param all the collection.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func remove(all!: Collection<T>): Unit
    
    /**
    * Transfer a lambda expression and delete the corresponding element if the condition is met.
    *
    * @param predicate transfer a lambda expression for judgment.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func removeIf(predicate: (T) -> Bool): Unit
    
    /**
    * Clears all elements.
    */
    @!APILevel[12, atomicservice : true]
    public func clear(): Unit
    
    /**
    * Copy a TreeSet.
    *
    * @return returns a cloned TreeSet.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func clone(): TreeSet<T>
    
    /**
    * Check whether the size is empty. If yes, true is returned. Otherwise, false is returned.
    *
    * @return bool if yes, true is returned. Otherwise, false is returned.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func isEmpty(): Bool
    
    /**
    * Get an iterator.
    *
    * @return the return type is an iterator and contains all elements.
    *
    */
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func iterator(): Iterator<T>
    
    /**
    * Delete the first node of the TreeSet.
    *
    * @return If the first node exists, it is deleted and the element it stores is returned. Otherwise, it returns None.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func removeFirst(): ?T
    
    /**
    * Delete the last node of the TreeSet.
    *
    * @return If the last node exists, it is deleted and the element it stores is returned. Otherwise, it returns None.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func removeLast(): ?T
    
    /**
    * Gets a descending iterator. 
    * The head node of the iterator is the first node whose value is less than or equal to the marked element, and the tail node is the head node of the original set.
    *
    * @param mark Marked element.
    * @param inclusive Indicates whether the iterator includes the marked element node. 
    * If yes, the iterator includes the marked element node. Otherwise, the iterator does not include the marked element node.
    *
    * @return iterator.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func backward(mark: T, inclusive!: Bool = true): Iterator<T>
    
    /**
    * Gets a Ascending iterator. 
    * The head node of the iterator is the first node with a value greater than or equal to the marked element, and the tail node is the tail node of the original set.
    *
    * @param mark Marked element.
    * @param inclusive Indicates whether the iterator includes the marked element node. 
    * If yes, the iterator includes the marked element node. Otherwise, the iterator does not include the marked element node.
    *
    * @return iterator.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func forward(mark: T, inclusive!: Bool = true): Iterator<T>
    
    /**
    * Retain only duplicate T.
    * Removes all elements from this collection that are not contained
    * in the specified collection. This function depends on the contains function.
    * Please note that. If the incoming interface is a subtype of a malicious implementation.
    * We do not guarantee the correctness of results for retaining duplicate elements.
    *
    * @param all the other set.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func retain(all!: Set<T>): Unit
    
    /**
    * Returns the element in this TreeSet as an Array.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func toArray(): Array<T>
    
    /**
    * Check whether the set is a subset of other.
    *
    * @param other a set of the set type.
    * @return bool returns true if it is a subset, false otherwise.
    *
    */
    @!APILevel[12, atomicservice : true]
    public func subsetOf(other: ReadOnlySet<T>): Bool
    
    /**
    * Computes the intersection of this set and another set.
    * Returns a new set containing only the elements that are present in both sets.
    *
    * @param other Another set to intersect with.
    * @return A new `TreeSet` containing elements common to both sets.
    *
    * Example:
    * ```
    * let set1 = TreeSet([1, 2, 3])
    * let set2 = TreeSet([2, 3, 4])
    * let result = set1 & set2 // result is [2, 3]
    * ```
    */
    @!APILevel[12, atomicservice : true]
    public operator func &(other: ReadOnlySet<T>): TreeSet<T>
    
    /**
    * Computes the union of this set and another set.
    * Returns a new set containing all unique elements from both sets.
    *
    * @param other Another set to unite with.
    * @return A new `TreeSet` containing all unique elements from both sets.
    *
    * Example:
    * ```
    * let set1 = TreeSet([1, 2, 3])
    * let set2 = TreeSet([3, 4, 5])
    * let result = set1 | set2 // result is [1, 2, 3, 4, 5]
    * ```
    */
    @!APILevel[12, atomicservice : true]
    public operator func |(other: ReadOnlySet<T>): TreeSet<T>
    
    /**
    * Computes the difference of this set and another set.
    * Returns a new set containing elements that are present in this set but not in the other set.
    *
    * @param other Another set to subtract from this set.
    * @return A new `TreeSet` containing elements unique to this set.
    *
    * Example:
    * ```
    * let set1 = TreeSet([1, 2, 3])
    * let set2 = TreeSet([3, 4, 5])
    * let result = set1 - set2 // result is [1, 2]
    * ```
    */
    @!APILevel[12, atomicservice : true]
    public operator func -(other: ReadOnlySet<T>): TreeSet<T>
}

/**
* Defines the TreeSet inherits the Equalable method and determines whether == and != methods.
*
*/
extend<T> TreeSet<T> <: Equatable<TreeSet<T>> {
    /** overloaded determination equal function. */
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: TreeSet<T>): Bool
    
    /** overloaded determination unequal function. */
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: TreeSet<T>): Bool
}

extend<T> TreeSet<T> <: ToString where T <: ToString {
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

