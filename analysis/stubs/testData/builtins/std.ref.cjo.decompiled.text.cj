// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.ref

import std.core.*

internal func getEagerWeakRefPolicy(): std.ref.CleanupPolicy { /* compiled code */ }

public enum CleanupPolicy <: std.core.Equatable<std.ref.CleanupPolicy> {
    | EAGER

    | DEFERRED

    public operator func ==(that: std.ref.CleanupPolicy): Bool { /* compiled code */ }

    public operator func !=(that: std.ref.CleanupPolicy): Bool { /* compiled code */ }
}

internal func getDeferredWeakRefPolicy(): std.ref.CleanupPolicy { /* compiled code */ }

public abstract open class WeakRefBase {
    internal var _value: std.core.Option<std.core.Object>

    internal let _cleanupPolicy: std.ref.CleanupPolicy

    init(value: std.core.Option<std.core.Object>, cleanupPolicy: std.ref.CleanupPolicy) { /* compiled code */ }
}

public class WeakRef<T> <: std.ref.WeakRefBase {
    init(value: T, cleanupPolicy: std.ref.CleanupPolicy) { /* compiled code */ }

    public prop cleanupPolicy: std.ref.CleanupPolicy {
        get() { /* compiled code */ }
    }

    public prop value: std.core.Option<T> {
        get() { /* compiled code */ }
    }

    public func clear(): Unit { /* compiled code */ }
}
