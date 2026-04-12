// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.




package std.sort
import std.collection.*

import std.math.*

/**
* Sorting element T.
*
* @param data Array of elements to be sorted.
* @param stable Whether to use stable sorting.
* @param descending Indicates whether to use descending sorting.
*
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func sort<T>(data: Array<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>

/**
* Sorting element T.
*
* @param data Array of elements to be sorted.
* @param by Comparator.
* @param stable Whether to use stable sorting.
* @param descending Indicates whether to use descending sorting.
*
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func sort<T>(data: Array<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit

/**
* Sorting element T.
*
* @param data Array of elements to be sorted.
* @param lessThan Comparator.
* @param stable Whether to use stable sorting.
* @param descending Indicates whether to use descending sorting.
*
*/
@!APILevel[12, atomicservice : true]
public func sort<T>(data: Array<T>, lessThan!: (T, T) -> Bool, stable!: Bool = false, descending!: Bool = false): Unit

/**
* Sorting element T.
*
* @param data Array of elements to be sorted.
* @param key Mapping of elements T to K.
* @param stable Whether to use stable sorting.
* @param descending Indicates whether to use descending sorting.
*
*/
@!APILevel[12, atomicservice : true]
public func sort<T, K>(data: Array<T>, key!: (T) -> K, stable!: Bool = false, descending!: Bool = false): Unit where K <: Comparable<K>

/**
* Sorting element T.
*
* @param data List of elements to be sorted.
* @param stable Whether to use stable sorting.
* @param descending Indicates whether to use descending sorting.
*
*/
@!APILevel[12, atomicservice : true]
public func sort<T>(data: List<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>

/**
* Sorting element T.
*
* @param data List of elements to be sorted.
* @param by Comparator.
* @param stable Whether to use stable sorting.
* @param descending Indicates whether to use descending sorting.
*
*/
@!APILevel[12, atomicservice : true]
public func sort<T>(data: List<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit

/**
* Sorting element T.
*
* @param data List of elements to be sorted.
* @param lessThan Comparator.
* @param stable Whether to use stable sorting.
* @param descending Indicates whether to use descending sorting.
*
*/
@!APILevel[12, atomicservice : true]
public func sort<T>(data: List<T>, lessThan!: (T, T) -> Bool, stable!: Bool = false, descending!: Bool = false): Unit

/**
* Sorting element T.
*
* @param data List of elements to be sorted.
* @param key Mapping of elements T to K.
* @param stable Whether to use stable sorting.
* @param descending Indicates whether to use descending sorting.
*
*/
@!APILevel[12, atomicservice : true]
public func sort<T, K>(data: List<T>, key!: (T) -> K, stable!: Bool = false, descending!: Bool = false): Unit where K <: Comparable<K>

/**
* Sorting element T.
*
* @param data List of elements to be sorted.
* @param stable Whether to use stable sorting.
* @param descending Indicates whether to use descending sorting.
*
*/
@!APILevel[12, atomicservice : true]
public func sort<T>(data: ArrayList<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>

/**
* Sorting element T.
*
* @param data List of elements to be sorted.
* @param by Comparator.
* @param stable Whether to use stable sorting.
* @param descending Indicates whether to use descending sorting.
*
*/
@!APILevel[12, atomicservice : true]
public func sort<T>(data: ArrayList<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit

/**
* Sorting element T.
*
* @param data List of elements to be sorted.
* @param lessThan Comparator.
* @param stable Whether to use stable sorting.
* @param descending Indicates whether to use descending sorting.
*
*/
@!APILevel[12, atomicservice : true]
public func sort<T>(data: ArrayList<T>, lessThan!: (T, T) -> Bool, stable!: Bool = false, descending!: Bool = false): Unit

/**
* Sorting element T.
*
* @param data List of elements to be sorted.
* @param key Mapping of elements T to K.
* @param stable Whether to use stable sorting.
* @param descending Indicates whether to use descending sorting.
*
*/
@!APILevel[12, atomicservice : true]
public func sort<T, K>(data: ArrayList<T>, key!: (T) -> K, stable!: Bool = false, descending!: Bool = false): Unit where K <: Comparable<K>

@Deprecated[message: "The interface is deprecated, no substitutions."]
@!APILevel[12, atomicservice : true]
public interface SortByExtension<T> {
}

@Deprecated[message: "The interface is deprecated, no substitutions."]
@!APILevel[12, atomicservice : true]
public interface SortExtension {
}

extend<T> Array<T> <: SortByExtension<T> {
    @Deprecated[message:"Use global function `public func sort<T>(data: Array<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit` instead."]@OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func sortBy(comparator!: (T, T) -> Ordering): Unit
    
    @Deprecated[message:"Use global function `public func sort<T>(data: Array<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit` instead."]@OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func sortBy(stable!: Bool, comparator!: (T, T) -> Ordering): Unit
}

extend<T> Array<T> <: SortExtension where T <: Comparable<T> {
    @Deprecated[message:"Use global function `public func sort<T>(data: Array<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` instead."]
    @!APILevel[12, atomicservice : true]
    public func sort(): Unit
    
    @Deprecated[message:"Use global function `public func sort<T>(data: Array<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` instead."]
    @!APILevel[12, atomicservice : true]
    public func sort(stable!: Bool): Unit
    
    @Deprecated[message:"Use global function `public func sort<T>(data: Array<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` instead."]
    @!APILevel[12, atomicservice : true]
    public func sortDescending(): Unit
    
    @Deprecated[message:"Use global function `public func sort<T>(data: Array<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` instead."]
    @!APILevel[12, atomicservice : true]
    public func sortDescending(stable!: Bool): Unit
}

/*
* Stable ascending sort
*
* @param data Array to be sorted.
*
* @since 0.27.3
*/
@Deprecated[message: "Use global function `public func sort<T>(data: Array<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` instead."]
@!APILevel[12, atomicservice : true]
public func stableSort<T>(data: Array<T>): Unit where T <: Comparable<T>

/*
* Stable sort
*
* @param data Array to be sorted.
* @param comparator The sorted comparison strategy.
*
* @since 0.27.3
*/
@Deprecated[message: "Use global function `public func sort<T>(data: Array<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit` instead."]
@!APILevel[12, atomicservice : true]
public func stableSort<T>(data: Array<T>, comparator: (T, T) -> Ordering): Unit

/**
* Unstable ascending sort
*
* @param data Array to be sorted.
*
* @since 0.27.3
*/
@Deprecated[message: "Use global function `public func sort<T>(data: Array<T>, stable!: Bool = false, descending!: Bool = false): Unit where T <: Comparable<T>` instead."]
@!APILevel[12, atomicservice : true]
public func unstableSort<T>(data: Array<T>): Unit where T <: Comparable<T>

/**
* Unstable sort
*
* @param data Array to be sorted.
* @param comparator The sorted comparison strategy.
*
* @since 0.27.3
*/
@Deprecated[message: "Use global function `public func sort<T>(data: Array<T>, by!: (T, T) -> Ordering, stable!: Bool = false, descending!: Bool = false): Unit` instead."]
@!APILevel[12, atomicservice : true]
public func unstableSort<T>(data: Array<T>, comparator: (T, T) -> Ordering): Unit

