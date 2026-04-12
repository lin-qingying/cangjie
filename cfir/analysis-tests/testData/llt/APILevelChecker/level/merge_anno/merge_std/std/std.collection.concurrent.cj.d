// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package std.collection.concurrent
import std.sync.*
import std.time.*

import std.collection.MapEntryView
import std.collection.ArrayList

/**
* ArrayBlockingQueue is a concurrent queue with a blocking mechanism and supports a user-specified upper bound of capacity.
* It is backed by an array.
*/
@!APILevel[since: "12", atomicservice : true]
public class ArrayBlockingQueue<E> {
    @!APILevel[since: "12", atomicservice : true]
    public let capacity: Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public init(capacity: Int64)
    
    @Deprecated[message: "Use the other version of `init` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public init(capacity: Int64, elements: Collection<E>)
    
    /**
    * Returns the number of elements in the BlockingQueue.
    *
    * It ensures to return the accurate number of elements in the BlockingQueue in sequential.
    * Note that getting 'size' prop is not atomic!
    * So, if elements are added or removed during execution of getting 'size',
    * the 'size' may be inaccurate.
    */
    @!APILevel[since: "12", atomicservice : true]
    public prop size: Int64
    
    /**
    * Blocking method to send the element @p element into the tail of the queue.
    * If there is no free space in the queue, the thread calling such method will be blocked,
    * and waiting for another thread calling 'dequeue' method to await.
    *
    * @param element: the element to send into the queue.
    *
    * A successful 'enqueue' will await the threads blocked by calling 'dequeue'.
    */
    @Deprecated[message: "Use member function `public func add(element: E): Unit` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func enqueue(element: E): Unit
    
    @!APILevel[since: "12", atomicservice : true]
    public func add(element: E): Unit
    
    /**
    * Blocking method to send the element @p element to the tail of the queue,
    * waiting up to @p timeout if the ArrayBlockingQueue is full.
    *
    * @param element: the element to send into the queue.
    * @param timeout: the waiting time.
    *
    * @return:
    * - true: if @p element is successfully pushed at the tail of the queue.
    * - false: if the waiting time exceeds @p timeout.
    */
    @Deprecated[message: "Use member function `public func add(element: E, timeout: Duration): Bool` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func enqueue(element: E, timeout: Duration): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public func add(element: E, timeout: Duration): Bool
    
    /**
    * Blocking method to get and remove the element at the head of the queue.
    * If the queue is empty, the thread calling such method will be blocked,
    * and waiting for another thread calling 'enqueue' method to await.
    *
    * A successful 'dequeue' will await blocked threads calling 'enqueue'.
    */
    @Deprecated[message: "Use member function `public func remove(): E` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func dequeue(): E
    
    @!APILevel[since: "12", atomicservice : true]
    public func remove(): E
    
    /**
    * Blocking method to get and remove the head of the queue,
    * waiting up to @p timeout if the ArrayBlockingQueue is empty.
    *
    * @param timeout: the waiting time.
    *
    * @return:
    * - Some(v): if gets and removes the element at the head of the queue successfully.
    * - None: if the waiting time exceeds @p timeout.
    */
    @Deprecated[message: "Use member function `public func remove(timeout: Duration): Option<E>` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func dequeue(timeout: Duration): Option<E>
    
    @!APILevel[since: "12", atomicservice : true]
    public func remove(timeout: Duration): Option<E>
    
    /**
    * Gets the head of the queue, but does not remove it.
    * Such method is non-blocking.
    *
    * @return:
    * - Some(v): if we successfully gets the element at the head of the queue.
    * - None: if the queue is empty, and we fails to get the head of the queue.
    */
    @Deprecated[message: "Use member function `public func peek(): Option<E>` instead."]@OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func head(): Option<E>
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func peek(): Option<E>
    
    /**
    * Non-blocking method to send the element @p element to the tail of the queue.
    * A successful 'tryEnqueue' will await blocked threads calling 'dequeue'.
    *
    * @param element: the element to send into the queue.
    *
    * @return:
    * - true: if @p element is successfully pushed at the tail of the queue.
    * - false: if the queue is full.
    */
    @Deprecated[message: "Use member function `public func tryAdd(element: E): Bool` instead."]@OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func tryEnqueue(element: E): Bool
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func tryAdd(element: E): Bool
    
    /**
    * Non-blocking method to get and remove the head of the queue.
    * A successful 'tryDequeue' will await blocked threads calling 'enqueue'.
    *
    * @return:
    * - Some(v): if gets and removes the element at the head of the queue successfully.
    * - None: if the queue is empty.
    */
    @Deprecated[message: "Use member function `public func tryRemove(): Option<E>` instead."]@OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func tryDequeue(): Option<E>
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func tryRemove(): Option<E>
}

@Deprecated[message: "Use LinkedBlockingQueue instead."]
public type BlockingQueue<E> = LinkedBlockingQueue<E>

/**
* BlockingQueue is a concurrent queue with a blocking mechanism and supports a user-specified upper bound of capacity.
*/
@!APILevel[since: "12", atomicservice : true]
public class LinkedBlockingQueue<E> {
    @!APILevel[since: "12", atomicservice : true]
    public let capacity: Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public init(capacity: Int64)
    
    @!APILevel[since: "12", atomicservice : true]
    public init()
    
    @Deprecated[message: "Use the other version of `init` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public init(capacity: Int64, elements: Array<E>)
    
    @Deprecated[message: "Use the other version of `init` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public init(capacity: Int64, elements: Collection<E>)
    
    /**
    * Returns the number of elements in the BlockingQueue.
    *
    * It ensures to return the accurate number of elements in the BlockingQueue in sequential.
    * Note that getting 'size' prop is not atomic!
    * So, if elements are added or removed during execution of getting 'size',
    * the 'size' may be inaccurate.
    */
    @!APILevel[since: "12", atomicservice : true]
    public prop size: Int64
    
    /**
    * Blocking method to send the element @p element into the tail of the queue.
    * If there is no free space in the queue, the thread calling such method will be blocked,
    * and waiting for another thread calling 'dequeue' method to await.
    *
    * @param element: the element to send into the queue.
    *
    * A successful 'enqueue' will await the threads blocked by calling 'dequeue'.
    */
    @Deprecated[message: "Use member function `public func add(element: E): Unit` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func enqueue(element: E): Unit
    
    @!APILevel[since: "12", atomicservice : true]
    public func add(element: E): Unit
    
    /**
    * Blocking method to send the element @p element to the tail of the queue,
    * waiting up to @p timeout if the BlockingQueue is full.
    *
    * @param element: the element to send into the queue.
    * @param timeout: the waiting time.
    *
    * @return:
    * - true: if @p element is successfully pushed at the tail of the queue.
    * - false: if the waiting time exceeds @p timeout.
    */
    @Deprecated[message: "Use member function `public func add(element: E, timeout: Duration): Bool` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func enqueue(element: E, timeout: Duration): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public func add(element: E, timeout: Duration): Bool
    
    /**
    * Blocking method to get and remove the element at the head of the queue.
    * If the queue is empty, the thread calling such method will be blocked,
    * and waiting for another thread calling 'enqueue' method to await.
    *
    * A successful 'dequeue' will await blocked threads calling 'enqueue'.
    *
    * @return:
    * - e: e is the element at the head of the queue.
    */
    @Deprecated[message: "Use member function `public func remove(): E` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func dequeue(): E
    
    @!APILevel[since: "12", atomicservice : true]
    public func remove(): E
    
    /**
    * Blocking method to get and remove the head of the queue,
    * waiting up to @p timeout if the BlockingQueue is empty.
    *
    * @param timeout: the waiting time.
    *
    * @return:
    * - Some(v): if gets and removes the element at the head of the queue successfully.
    * - None: if the waiting time exceeds @p timeout.
    */
    @Deprecated[message: "Use member function `public func remove(timeout: Duration): Option<E>` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func dequeue(timeout: Duration): Option<E>
    
    @!APILevel[since: "12", atomicservice : true]
    public func remove(timeout: Duration): Option<E>
    
    /**
    * Gets the head of the queue, but does not remove it.
    * Such method is non-blocking.
    *
    * @return:
    * - Some(v): if we successfully gets the element at the head of the queue.
    * - None: if the queue is empty, and we fails to get the head of the queue.
    */
    @Deprecated[message: "Use member function `public func peek(): Option<E>` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func head(): Option<E>
    
    @!APILevel[since: "12", atomicservice : true]
    public func peek(): Option<E>
    
    /**
    * Non-blocking method to send the element @p element to the tail of the queue.
    * A successful 'tryEnqueue' will await blocked threads calling 'dequeue'.
    *
    * @param element: the element to send into the queue.
    *
    * @return:
    * - true: if @p element is successfully pushed at the tail of the queue.
    * - false: if the queue is full.
    */
    @Deprecated[message: "Use member function `public func tryAdd(element: E): Bool` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func tryEnqueue(element: E): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public func tryAdd(element: E): Bool
    
    /**
    * Non-blocking method to get and remove the head of the queue.
    * A successful 'tryDequeue' will await blocked threads calling 'enqueue'.
    *
    * @return:
    * - Some(v): if gets and removes the element at the head of the queue successfully.
    * - None: if the queue is empty.
    */
    @Deprecated[message: "Use member function `public func tryRemove(): Option<E>` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func tryDequeue(): Option<E>
    
    @!APILevel[since: "12", atomicservice : true]
    public func tryRemove(): Option<E>
}

@!APILevel[since: "12", atomicservice : true]

/**
* The definition of the ConcurrentHashMap with load factor 1.
* Load factor is 1, indicating that the number of buckets in the ConcurrentHashMap equals to
* the number of key-value pairs usually.
* And if the number of key-value pairs is greater than the number of buckets in the ConcurrentHashMap,
* capacity expansion will occur.
*/
@!APILevel[since: "12", atomicservice : true]
public class ConcurrentHashMap<K, V> <: ConcurrentMap<K, V> & Collection<(K, V)>
                             where K <: Hashable & Equatable<K> {
    /************************ Public Methods *******************************//**
    * Create a ConcurrentHashMap, whose initial capacity is DEFAULT_CAPACITY (=16) and
    * concurrencyLevel is @p concurrencyLevel.
    * The default concurrencyLevel is DEFAULT_CONCUR_LEVEL (=16).
    *
    * @param concurrencyLevel: the number of reentrant mutexes for synchronization.
    */
    @!APILevel[since: "12", atomicservice : true]
    public init(concurrencyLevel!: Int64 = DEFAULT_CONCUR_LEVEL)
    
    /**
    * Create a ConcurrentHashMap, whose initial capacity is @p capacity and
    * concurrencyLevel is @p concurrencyLevel.
    * The default concurrencyLevel is DEFAULT_CONCUR_LEVEL (=16).
    *
    * @param capacity: initial capacity of the ConcurrentHashMap;
    * @param concurrencyLevel: the number of reentrant mutexes for synchronization.
    *
    * @throws IllegalArgumentException if capacity is less than zero.
    */
    @!APILevel[since: "12", atomicservice : true]
    public init(capacity: Int64, concurrencyLevel!: Int64 = DEFAULT_CONCUR_LEVEL)
    
    /**
    * Create a ConcurrentHashMap with an incoming list for initialization.
    *
    * @param elements: an incoming list is initialized;
    * @param concurrencyLevel: the number of reentrant mutexes for synchronization.
    */
    @!APILevel[since: "12", atomicservice : true]
    public init(elements: Collection<(K, V)>, concurrencyLevel!: Int64 = DEFAULT_CONCUR_LEVEL)
    
    /**
    * Create a ConcurrentHashMap with an incoming list for initialization.
    *
    * @param elements: an incoming list is initialized;
    * @param concurrencyLevel: the number of reentrant mutexes for synchronization.
    */
    @!APILevel[since: "12", atomicservice : true]
    public init(size: Int64, initElement: (Int64) -> (K, V),
                concurrencyLevel!: Int64 = DEFAULT_CONCUR_LEVEL)
    
    /**
    * Returns the value associated with @p key.
    *
    * @param key: transfer key to obtain the value.
    * @return: the value corresponding to the return key is encapsulated with option.
    */
    @Frozen
    @!APILevel[since: "12", atomicservice : true]
    public func get(key: K): ?V
    
    /**
    * Checks whether the mapping relationship corresponding to the specified key exists in this mapping.
    *
    * @param key: transfers the key to be judged.
    * @return: returns true if exists; otherwise, false.
    */
    @Frozen
    @!APILevel[since: "12", atomicservice : true]
    public func contains(key: K): Bool
    
    /**
    * Add a new key value to the map. If the key already exists,
    * the value will be overwritten and the overwritten value will be returned.
    *
    * @param key: the key to put;
    * @param value: the value to put.
    *
    * @return:
    * - None: if @p key does not exist before putting;
    * - Some(v): if @p key associated with v before putting.
    */
    @Deprecated[message: "Use member function `public func add(key: K, value: V): ?V` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func put(key: K, value: V): ?V
    
    @Frozen
    @!APILevel[since: "12", atomicservice : true]
    public func add(key: K, value: V): ?V
    
    /**
    * Associates the specified @p value with the specified @p key in this concurrent map,
    * if @p key does not in the concurrent map, before calling this method.
    * Otherwise, just returns the value that @p key maps to and does nothing.
    *
    * Such method is atomic (linearizable) in concurrency.
    *
    * @param key: the key to put.
    * @param value: the value to assign.
    *
    * @return:
    * - Some(v): if the pair of @p key and v exists before putting;
    * - None: if @p key does not exist in the concurrent map.
    */
    @Deprecated[message: "Use member function `public func addIfAbsent(key: K, value: V): ?V` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func putIfAbsent(key: K, value: V): ?V
    
    @Frozen
    @!APILevel[since: "12", atomicservice : true]
    public func addIfAbsent(key: K, value: V): ?V
    
    /**
    * Removes the key-value pair corresponding to @p key from this mapping, if one exists.
    *
    * @param key: pass in the key to be deleted.
    *
    * @return:
    * - Some(v): where the pair of @p key and v is the removed element;
    * - None: where @p key does not exist in the concurrent map.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func remove(key: K): ?V
    
    /**
    * Removes the pair of @p key and value, where predicate(value) = true holds,
    * from this concurrent map.
    * Otherwise, just returns 'false' and does nothing.
    *
    * @param key: the key of the key-value pair to be deleted.
    * @param predicate: the function justifies whether the pair of @p key and value should be removed.
    *
    * @return:
    * - Some(v): if exists the pair of @p key and v in concurrent map;
    * - None: if @p key does not in this concurrent map.
    */
    @Deprecated[message: "Use member function `public func entryView(K, (MapEntryView<K, V>) -> Unit)` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func remove(key: K, predicate: (V) -> Bool): ?V
    
    /**
    * Replaces the value associated with @p key to @p value,
    * if there exists a pair of @p key and some value v in the concurrent hashmap.
    * Otherwise, just returns 'Option<V>.None' and does nothing.
    *
    * @param key: the key of the key-value pair, whose value needs to be replaced.
    * @param value: the value to be replaced.
    *
    * @return:
    * - Some(v): if the pair of @p key and v exists in concurrent map before invoking this method.
    * - None: if @p key is not in the concurrent map.
    */
    @Frozen
    @!APILevel[since: "12", atomicservice : true]
    public func replace(key: K, value: V): ?V
    
    /**
    * Replaces the value associated with @p key to eval(v),
    * if there exists a pair of @p key and v in the concurrent hashmap.
    * Otherwise, just returns 'Option<V>.None' and does nothing.
    *
    * Such method is atomic (linearizable) in concurrency.
    *
    * @param key: the key to put.
    * @param eval: function to evaluate the value to put.
    * @p eval evaluates the value according to the old value v.
    *
    * @return:
    * - Some(v): if the pair of @p key and v exists in concurrent map before invoking this method.
    * - None: if @p key is not in the concurrent map.
    */
    @Deprecated[message: "Use member function `public func entryView(key: K, fn: (MapEntryView<K, V>)->Unit): ?V` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func replace(key: K, eval: (V) -> V): ?V
    
    /**
    * Replaces the value associated with @p key to eval(v),
    * if the pair of @p key and v in the concurrent map, and @p predicate(v) holds.
    *
    * Such method is atomic (linearizable) in concurrency.
    *
    * @param key: the key of the key-value pair, whose value needs to be replaced.
    * @param predicate: the function justifies whether @p key-v pair in the concurrent map should be replaced.
    * @param eval: the function evaluates the new value to replace the old value.
    *
    * @return:
    * - Some(v): if the pair of @p key and v exists in concurrent map before replacing;
    * - None: if @p key does not exist in concurrent map.
    */
    @Deprecated[message: "Use member function `public func entryView(key: K, fn: (MapEntryView<K, V>)->Unit): ?V` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func replace(key: K, predicate: (V) -> Bool, eval: (V) -> V): ?V
    
    @!APILevel[since: "12", atomicservice : true]
    public func entryView(key: K, fn: (MapEntryView<K, V>)->Unit): ?V
    
    /**
    * An exception is reported when the get operator is overloaded and the key does not exist.
    *
    * @param key: transfers the value for judgment.
    * @return: the value corresponding to the key.
    *
    * @throws NoneValueException if @p key does not exist.
    */
    @!APILevel[since: "12", atomicservice : true]
    public operator func [](key: K): V
    
    /**
    * The operator overloads the set. If the key does not exist, an exception is reported.
    *
    * @param key: transfers the value for judgment.
    * @param value: transfers the value to be set.
    */
    @!APILevel[since: "12", atomicservice : true]
    public operator func [](key: K, value!: V): Unit
    
    /**
    * Returns sizes of key-value.
    *
    * @return: size of key-value.
    */
    @!APILevel[since: "12", atomicservice : true]
    public prop size: Int64
    
    /**
    * Check whether the size is empty. If yes, true is returned. Otherwise, false is returned.
    *
    * @return: if yes, true is returned. Otherwise, false is returned.
    */
    @!APILevel[since: "12", atomicservice : true]
    public func isEmpty(): Bool
    
    /**
    * Returns iterator of Concurrent Hashmap.
    *
    * @return: iterator of Concurrent Hashmap.
    */
    @Frozen
    @!APILevel[since: "12", atomicservice : true]
    public func iterator(): ConcurrentHashMapIterator<K, V>
}

/**
* The Iterator of the ConcurrentHashMap.
* It is not atomic and does not ensure to iterator a snapshot of the ConcurrentHashMap in concurrency.
* The best way to iterate the ConcurrentHashMap is in the condition, where there is no other threads executing concurrently.
*/
@!APILevel[since: "12", atomicservice : true]
public class ConcurrentHashMapIterator<K, V> <: Iterator<(K, V)> where K <: Hashable & Equatable<K> {
    @!APILevel[since: "12", atomicservice : true]
    public init(cmap: ConcurrentHashMap<K, V>)
    
    /**
    * Returns the current element, and sets pointer to the next element.
    * - Some(val): if the current element is val;
    * - None: if there is no element in the collection.
    */
    @Frozen
    @!APILevel[since: "12", atomicservice : true]
    public func next(): Option<(K, V)>
}

/**
* This interface is a key-value pair that does not guarantee the order of elements.
* It declares the methods in a concurrent map that need to be atomic. 
*/
@!APILevel[since: "12", atomicservice : true]
public interface ConcurrentMap<K, V> {
}

@Deprecated[message: "Use ConcurrentLinkedQueue instead."]
public type NonBlockingQueue<E> = ConcurrentLinkedQueue<E>

/**
* NonBlockingQueue is an unbounded nonblocking concurrent queue.
*/
@!APILevel[since: "12", atomicservice : true]
public class ConcurrentLinkedQueue<E> <: Collection<E> {
    /**
    * The default method to initialize the concurrent queue.
    */
    @!APILevel[since: "12", atomicservice : true]
    public init()
    
    /**
    * Initialize the queue by putting all the element in @p elements into the queue.
    */
    @Deprecated[message: "Use the other version of `init` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public init(elements: Collection<E>)
    
    /**
    * Returns the number of elements in the NonBlockingQueue.
    *
    * It ensures to return the accurate number of elements in the NonBlockingQueue in sequential.
    * Note that getting 'size' prop is not atomic!
    * So, if elements are added or removed during execution of getting 'size',
    * the 'size' may be inaccurate.
    */
    @!APILevel[since: "12", atomicservice : true]
    public prop size: Int64
    
    /**
    * Sends the element @p element to the tail of the queue.
    *
    * @param element: the element to send into the queue.
    *
    * @return:
    * - true: if @p element is successfully pushed at the tail of the queue.
    *
    * Note that the NonBlockingQueue 'enqueue' will never return 'false'.
    */
    @Deprecated[message: "Use member function `func addIfAbsent(key: K, value: V): ?V` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func enqueue(element: E): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public func add(element: E): Bool
    
    /**
    * Gets and removes the head of the queue.
    *
    * @return:
    * - Some(v): if gets and removes the element at the head of the queue successfully.
    * - None: if the queue is empty.
    */
    @Deprecated[message: "Use member function `public func remove(): Option<E>` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func dequeue(): Option<E>
    
    @!APILevel[since: "12", atomicservice : true]
    public func remove(): Option<E>
    
    /**
    * Gets the head of the queue, but does not remove it.
    *
    * @return:
    * - Some(v): if gets the element at the head of the queue successfully.
    * - None: the queue is empty, and fails to get the head of the queue.
    *
    * Note: calling 'head()' can still get the head of the queue.
    */
    @Deprecated[message: "Use member function `public func peek(): Option<E>` instead."]
    @!APILevel[since: "12", atomicservice : true]
    public func head(): Option<E>
    
    @!APILevel[since: "12", atomicservice : true]
    public func peek(): Option<E>
    
    @!APILevel[since: "12", atomicservice : true]
    public func isEmpty(): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public func toArray(): Array<E>
    
    @Frozen
    @!APILevel[since: "12", atomicservice : true]
    public func iterator(): Iterator<E>
}

