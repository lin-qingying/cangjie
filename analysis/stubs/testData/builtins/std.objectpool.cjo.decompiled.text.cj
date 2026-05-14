// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.objectpool

import std.collection.concurrent.*
import std.core.*

class ObjectPool {
    var pool: std.core.Array<std.collection.concurrent.ConcurrentLinkedQueue<T>>

    let newFunc: () -> T

    let resetFunc: std.core.Option<(T) -> T>

    init(newFunc: () -> T, resetFunc: std.core.Option<(T) -> T>) { /* compiled code */ }

    func `get`(): T { /* compiled code */ }

    func put(item: T): Unit { /* compiled code */ }
}
