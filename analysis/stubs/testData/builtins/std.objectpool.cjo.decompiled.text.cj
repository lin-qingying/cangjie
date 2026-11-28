// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.objectpool

import std.collection.concurrent.*
import std.core.*

@Deprecated
public class ObjectPool<T> {
    internal var pool: std.core.Array<std.collection.concurrent.ConcurrentLinkedQueue<T>>

    internal let newFunc: () -> T

    internal let resetFunc: std.core.Option<(T) -> T>

    init(newFunc: () -> T, resetFunc!: std.core.Option<(T) -> T> = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    public func get(): T { /* compiled code */ }

    public func put(item: T): Unit { /* compiled code */ }
}
