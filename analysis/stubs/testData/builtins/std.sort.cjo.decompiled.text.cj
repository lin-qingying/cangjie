// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.sort

import std.collection.*
import std.core.*
import std.math.*

@Frozen
public func sort<T>(data: std.core.Array<T>, stable!: Bool = COMPILED_CODE /* compiled code */, descending!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

@Deprecated[message: "Use global function `public func sort<T>(data: Array<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit` instead."]
public func stableSort<T>(data: std.core.Array<T>, comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

@Deprecated[message: "Use global function `public func sort<T>(data: Array<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit` instead."]
public func unstableSort<T>(data: std.core.Array<T>, comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

@Deprecated[message: "Use global function `public func sort<T>(data: Array<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` instead."]
public func stableSort<T>(data: std.core.Array<T>): Unit { /* compiled code */ }

@Deprecated[message: "Use global function `public func sort<T>(data: Array<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` instead."]
public func unstableSort<T>(data: std.core.Array<T>): Unit { /* compiled code */ }

@Frozen
public func sort<T>(data: std.core.Array<T>, by!: (T, T) -> std.core.Ordering, stable!: Bool = COMPILED_CODE /* compiled code */, descending!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func sort<T>(data: std.core.Array<T>, lessThan!: (T, T) -> Bool, stable!: Bool = COMPILED_CODE /* compiled code */, descending!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func sort<T, K>(data: std.core.Array<T>, key!: (T) -> K, stable!: Bool = COMPILED_CODE /* compiled code */, descending!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func sort<T>(data: std.collection.List<T>, stable!: Bool = COMPILED_CODE /* compiled code */, descending!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func sort<T>(data: std.collection.List<T>, by!: (T, T) -> std.core.Ordering, stable!: Bool = COMPILED_CODE /* compiled code */, descending!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func sort<T>(data: std.collection.List<T>, lessThan!: (T, T) -> Bool, stable!: Bool = COMPILED_CODE /* compiled code */, descending!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func sort<T, K>(data: std.collection.List<T>, key!: (T) -> K, stable!: Bool = COMPILED_CODE /* compiled code */, descending!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func sort<T>(data: std.collection.ArrayList<T>, stable!: Bool = COMPILED_CODE /* compiled code */, descending!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func sort<T>(data: std.collection.ArrayList<T>, by!: (T, T) -> std.core.Ordering, stable!: Bool = COMPILED_CODE /* compiled code */, descending!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func sort<T>(data: std.collection.ArrayList<T>, lessThan!: (T, T) -> Bool, stable!: Bool = COMPILED_CODE /* compiled code */, descending!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func sort<T, K>(data: std.collection.ArrayList<T>, key!: (T) -> K, stable!: Bool = COMPILED_CODE /* compiled code */, descending!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

@Deprecated[message: "The interface is deprecated, no substitutions."]
public interface SortByExtension<T> {
    public open func sortBy(comparator!: (T, T) -> std.core.Ordering): Unit

    public open func sortBy(stable!: Bool, comparator!: (T, T) -> std.core.Ordering): Unit
}

@Deprecated[message: "The interface is deprecated, no substitutions."]
public interface SortExtension {
    public open func sort(): Unit

    public open func sort(stable!: Bool): Unit

    public open func sortDescending(): Unit

    public open func sortDescending(stable!: Bool): Unit
}

extend std.core.Array<T> <: SortByExtension {
    @Deprecated[message: "Use global function `public func sort<T>(data: Array<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit` instead."]
    public func sortBy(comparator!: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

    @Deprecated[message: "Use global function `public func sort<T>(data: Array<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit` instead."]
    public func sortBy(stable!: Bool, comparator!: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }
}

extend std.core.Array<T> <: SortExtension {
    @Deprecated[message: "Use global function `public func sort<T>(data: Array<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` instead."]
    public func sort(): Unit { /* compiled code */ }

    @Deprecated[message: "Use global function `public func sort<T>(data: Array<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` instead."]
    public func sort(stable!: Bool): Unit { /* compiled code */ }

    @Deprecated[message: "Use global function `public func sort<T>(data: Array<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` instead."]
    public func sortDescending(): Unit { /* compiled code */ }

    @Deprecated[message: "Use global function `public func sort<T>(data: Array<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` instead."]
    public func sortDescending(stable!: Bool): Unit { /* compiled code */ }
}
