// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.sort

import std.collection.*
import std.core.*
import std.math.*

func sort(data: std.core.Array<T>, stable: Bool, descending: Bool): Unit { /* compiled code */ }

func stableSort(data: std.core.Array<T>, comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

func unstableSort(data: std.core.Array<T>, comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

func stableSort(data: std.core.Array<T>): Unit { /* compiled code */ }

func unstableSort(data: std.core.Array<T>): Unit { /* compiled code */ }

func sort(data: std.core.Array<T>, by: (T, T) -> std.core.Ordering, stable: Bool, descending: Bool): Unit { /* compiled code */ }

func sort(data: std.core.Array<T>, lessThan: (T, T) -> Bool, stable: Bool, descending: Bool): Unit { /* compiled code */ }

func sort(data: std.core.Array<T>, key: (T) -> K, stable: Bool, descending: Bool): Unit { /* compiled code */ }

func sort(data: std.collection.List<T>, stable: Bool, descending: Bool): Unit { /* compiled code */ }

func sort(data: std.collection.List<T>, by: (T, T) -> std.core.Ordering, stable: Bool, descending: Bool): Unit { /* compiled code */ }

func sort(data: std.collection.List<T>, lessThan: (T, T) -> Bool, stable: Bool, descending: Bool): Unit { /* compiled code */ }

func sort(data: std.collection.List<T>, key: (T) -> K, stable: Bool, descending: Bool): Unit { /* compiled code */ }

func sort(data: std.collection.ArrayList<T>, stable: Bool, descending: Bool): Unit { /* compiled code */ }

func sort(data: std.collection.ArrayList<T>, by: (T, T) -> std.core.Ordering, stable: Bool, descending: Bool): Unit { /* compiled code */ }

func sort(data: std.collection.ArrayList<T>, lessThan: (T, T) -> Bool, stable: Bool, descending: Bool): Unit { /* compiled code */ }

func sort(data: std.collection.ArrayList<T>, key: (T) -> K, stable: Bool, descending: Bool): Unit { /* compiled code */ }

interface SortByExtension {
    func sortBy(comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

    func sortBy(stable: Bool, comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }
}

interface SortExtension {
    func sort(): Unit { /* compiled code */ }

    func sort(stable: Bool): Unit { /* compiled code */ }

    func sortDescending(): Unit { /* compiled code */ }

    func sortDescending(stable: Bool): Unit { /* compiled code */ }
}

extend Array <: SortByExtension {
    func sortBy(comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }

    func sortBy(stable: Bool, comparator: (T, T) -> std.core.Ordering): Unit { /* compiled code */ }
}

extend Array <: SortExtension {
    func sort(): Unit { /* compiled code */ }

    func sort(stable: Bool): Unit { /* compiled code */ }

    func sortDescending(): Unit { /* compiled code */ }

    func sortDescending(stable: Bool): Unit { /* compiled code */ }
}
