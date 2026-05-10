// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.ref

import std.core.*

func getEagerWeakRefPolicy: std.ref.CleanupPolicy

enum CleanupPolicy <: std.core.Equatable<std.ref.CleanupPolicy> {
    | EAGER

    | DEFERRED

    operator func ==(that: std.ref.CleanupPolicy): Bool

    operator func !=(that: std.ref.CleanupPolicy): Bool
}

func getDeferredWeakRefPolicy: std.ref.CleanupPolicy

class WeakRefBase {
    var _value: std.core.Option<std.core.Object>

    let _cleanupPolicy: std.ref.CleanupPolicy

    init(value: std.core.Option<std.core.Object>, cleanupPolicy: std.ref.CleanupPolicy) { /* compiled code */ }
}

class WeakRef <: std.ref.WeakRefBase {
    init(value: T, cleanupPolicy: std.ref.CleanupPolicy) { /* compiled code */ }

    prop cleanupPolicy: std.ref.CleanupPolicy

    prop value: std.core.Option<T>

    func clear: Unit
}
