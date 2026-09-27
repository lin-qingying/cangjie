// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.ref

import std.core.*

func getEagerWeakRefPolicy(): std.ref.CleanupPolicy { /* compiled code */ }

enum CleanupPolicy <: std.core.Equatable<std.ref.CleanupPolicy> {
    | EAGER

    | DEFERRED

    operator func ==(that: std.ref.CleanupPolicy): Bool { /* compiled code */ }

    operator func !=(that: std.ref.CleanupPolicy): Bool { /* compiled code */ }
}

func getDeferredWeakRefPolicy(): std.ref.CleanupPolicy { /* compiled code */ }

abstract open class WeakRefBase {
    var _value: std.core.Option<std.core.Object>

    let _cleanupPolicy: std.ref.CleanupPolicy

    init(value: std.core.Option<std.core.Object>, cleanupPolicy: std.ref.CleanupPolicy) { /* compiled code */ }
}

class WeakRef<T> <: std.ref.WeakRefBase {
    init(value: T, cleanupPolicy: std.ref.CleanupPolicy) { /* compiled code */ }

    @Annotation
    prop cleanupPolicy: std.ref.CleanupPolicy {
        get() { /* compiled code */ }
    }

    @Annotation
    prop value: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    func clear(): Unit { /* compiled code */ }
}
