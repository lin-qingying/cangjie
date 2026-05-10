// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.sort

import std.collection.*
import std.core.*
import std.math.*

func sort(data: std.core.Array<T>, stable: Bool, descending: Bool): Unit

func stableSort(data: std.core.Array<T>, comparator: (T, T) -> std.core.Ordering): Unit

func unstableSort(data: std.core.Array<T>, comparator: (T, T) -> std.core.Ordering): Unit

func stableSort(data: std.core.Array<T>): Unit

func unstableSort(data: std.core.Array<T>): Unit

func sort(data: std.core.Array<T>, by: (T, T) -> std.core.Ordering, stable: Bool, descending: Bool): Unit

func sort(data: std.core.Array<T>, lessThan: (T, T) -> Bool, stable: Bool, descending: Bool): Unit

func sort(data: std.core.Array<T>, key: (T) -> K, stable: Bool, descending: Bool): Unit

func sort(data: std.collection.List<T>, stable: Bool, descending: Bool): Unit

func sort(data: std.collection.List<T>, by: (T, T) -> std.core.Ordering, stable: Bool, descending: Bool): Unit

func sort(data: std.collection.List<T>, lessThan: (T, T) -> Bool, stable: Bool, descending: Bool): Unit

func sort(data: std.collection.List<T>, key: (T) -> K, stable: Bool, descending: Bool): Unit

func sort(data: std.collection.ArrayList<T>, stable: Bool, descending: Bool): Unit

func sort(data: std.collection.ArrayList<T>, by: (T, T) -> std.core.Ordering, stable: Bool, descending: Bool): Unit

func sort(data: std.collection.ArrayList<T>, lessThan: (T, T) -> Bool, stable: Bool, descending: Bool): Unit

func sort(data: std.collection.ArrayList<T>, key: (T) -> K, stable: Bool, descending: Bool): Unit

interface SortByExtension {
    func sortBy(comparator: (T, T) -> std.core.Ordering): Unit

    func sortBy(stable: Bool, comparator: (T, T) -> std.core.Ordering): Unit
}

interface SortExtension {
    func sort: Unit

    func sort(stable: Bool): Unit

    func sortDescending: Unit

    func sortDescending(stable: Bool): Unit
}

extend Array <: SortByExtension {
    func sortBy(comparator: (T, T) -> std.core.Ordering): Unit

    func sortBy(stable: Bool, comparator: (T, T) -> std.core.Ordering): Unit
}

extend Array <: SortExtension {
    func sort: Unit

    func sort(stable: Bool): Unit

    func sortDescending: Unit

    func sortDescending(stable: Bool): Unit
}
