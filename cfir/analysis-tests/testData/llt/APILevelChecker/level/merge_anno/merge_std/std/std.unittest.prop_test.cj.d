// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.








package std.unittest.prop_test
import std.collection.*
import std.convert.*
import std.math.*
import std.unittest.common.Configuration
import std.unittest.common.DataProvider
import std.unittest.common.DataShrinker
import std.unittest.common.DataStrategy

import std.random.Random
import std.unittest.common.KeyFor

/*
* Arbitrary is an interface that allows to generate random values of type T
*/
@!APILevel[since: "12", atomicservice : true]
public interface Arbitrary<T> {
}

// this does not compile (backend error)
// extend Nothing <: Arbitrary<Nothing> {
//     public static func arbitrary(random: RandomSource): Generator<Nothing> {
//         Generators.generate<Nothing> { => throw Exception("Arbitrary<Nothing>.generate") }
//     }
// }
/*
* Arbitrary instance for Unit: always returns ()
*/
extend Unit <: Arbitrary<Unit> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(_: RandomSource): Generator<Unit>
}

/*
* Arbitrary instance for Bool: returns true or false randomly
*/
extend Bool <: Arbitrary<Bool> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<Bool>
}

/*
* Arbitrary instance for signed integer: has a higher probability of generating
* border values: 0, 1, -1, Max and Min
*/
extend Int8 <: Arbitrary<Int8> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<Int8>
}

/*
* Arbitrary instance for signed integer: has a higher probability of generating
* border values: 0, 1, -1, Max and Min. Short numbers are generated more frequently.
*/
extend Int16 <: Arbitrary<Int16> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<Int16>
}

/*
* Arbitrary instance for signed integer: has a higher probability of generating
* border values: 0, 1, -1, Max and Min. Short numbers are generated more frequently.
*/
extend Int32 <: Arbitrary<Int32> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<Int32>
}

/*
* Arbitrary instance for signed integer: has a higher probability of generating
* border values: 0, 1, -1, Max and Min. Short numbers are generated more frequently.
*/
extend Int64 <: Arbitrary<Int64> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<Int64>
}

/*
* Arbitrary instance for signed integer: has a higher probability of generating
* border values: 0, 1, -1, Max and Min. Short numbers are generated more frequently.
*/
extend IntNative <: Arbitrary<IntNative> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<IntNative>
}

/*
* Arbitrary instance for unsigned integer: has a higher probability of generating
* border values: 0, 1 and Max
*/
extend UInt8 <: Arbitrary<UInt8> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<UInt8>
}

/*
* Arbitrary instance for unsigned integer: has a higher probability of generating
* border values: 0, 1 and Max. Short numbers are generated more frequently.
*/
extend UInt16 <: Arbitrary<UInt16> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<UInt16>
}

/*
* Arbitrary instance for unsigned integer: has a higher probability of generating
* border values: 0, 1 and Max
*/
extend UInt32 <: Arbitrary<UInt32> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<UInt32>
}

/*
* Arbitrary instance for unsigned integer: has a higher probability of generating
* border values: 0, 1 and Max
*/
extend UInt64 <: Arbitrary<UInt64> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<UInt64>
}

/*
* Arbitrary instance for unsigned integer: has a higher probability of generating
* border values: 0, 1 and Max. Short numbers are generated more frequently.
*/
extend UIntNative <: Arbitrary<UIntNative> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<UIntNative>
}

/*
* Arbitrary instance for float: returns random float
*/
extend Float16 <: Arbitrary<Float16> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<Float16>
}

/*
* Arbitrary instance for float: returns random float
*/
extend Float32 <: Arbitrary<Float32> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<Float32>
}

/*
* Arbitrary instance for float: returns random float
*/
extend Float64 <: Arbitrary<Float64> {
    // TODO: make a better generator
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<Float64>
}

/*
* Arbitrary instance for Rune: has a higher probability to return characters from basic set,
* otherwise can return any valid UTF-8 character
*/
extend Rune <: Arbitrary<Rune> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<Rune>
}

/*
* Arbitrary instance for Array: has a higher probability to generate empty or single-element arrays,
* the size is random but leaned towards smaller values and not bigger than SIZE_UPPER_LIMIT
* Elements are random according to their respective Arbitrary instances
*/
extend<T> Array<T> <: Arbitrary<Array<T>> where T <: Arbitrary<T> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<Array<T>>
}

/*
* ArrayList instance size has same principals as for Array
*/
extend<T> ArrayList<T> <: Arbitrary<ArrayList<T>> where T <: Arbitrary<T> {
    // `Generators.mapped` not used due to possible OOM,// So here is copypaste of `Arbitrary<Array<T>>`
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<ArrayList<T>>
}

/*
* HashSet instance size has same principals as for Array
*/
extend<T> HashSet<T> <: Arbitrary<HashSet<T>> where T <: Arbitrary<T> {
    // `Generators.mapped` not used due to possible OOM,// So here is copypaste of `Arbitrary<HashMap<T, Unit>>`
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<HashSet<T>>
}

/*
* instance size has same principals as for Array
* Keys and values are random according to their respective Arbitrary instances
*/
extend<K, V> HashMap<K, V> <: Arbitrary<HashMap<K, V>> where K <: Arbitrary<K>, V <: Arbitrary<V> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<HashMap<K, V>>
}

/*
* Arbitrary instance for String: has a higher probability to generate empty or single-element arrays,
* the size is random but leaned towards smaller values and not bigger than SIZE_UPPER_LIMIT
* Elements are random according to their respective Arbitrary instances
*/
extend String <: Arbitrary<String> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<String>
}

/*
* Arbitrary instance for Option: has a high probability to generate None,
* in other cases generates values according to elements' respective Arbitrary instance
*/
extend<T> Option<T> <: Arbitrary<Option<T>> where T <: Arbitrary<T> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<Option<T>>
}

/*
* Arbitrary instance for Ordering: generates random Ordering values
*/
extend Ordering <: Arbitrary<Ordering> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<Ordering>
}

/*
* Arbitrary instance for zero-argument functions, through wrapper struct
* Generates functions returning arbitrary values.
* NOTE: research whether we need to support other kinds of functions
*/
extend<R> Function0Wrapper<R> <: Arbitrary<Function0Wrapper<R>> where R <: Arbitrary<R> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<Function0Wrapper<R>>
}

/*
* Arbitrary instance for tuple (through TupleWrapper classes)
* Generates random tuple values using random element values
*/
extend<T0, T1> TupleWrapper2<T0, T1> <: Arbitrary<TupleWrapper2<T0, T1>>
        where T0 <: Arbitrary<T0>,
              T1 <: Arbitrary<T1> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<TupleWrapper2<T0, T1>>
}

/*
* Arbitrary instance for tuple (through TupleWrapper classes)
* Generates random tuple values using random element values
*/
extend<T0, T1, T2> TupleWrapper3<T0, T1, T2> <: Arbitrary<TupleWrapper3<T0, T1, T2>>
        where T0 <: Arbitrary<T0>,
              T1 <: Arbitrary<T1>,
              T2 <: Arbitrary<T2> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<TupleWrapper3<T0, T1, T2>>
}

/*
* Arbitrary instance for tuple (through TupleWrapper classes)
* Generates random tuple values using random element values
*/
extend<T0, T1, T2, T3> TupleWrapper4<T0, T1, T2, T3> <: Arbitrary<TupleWrapper4<T0, T1, T2, T3>>
        where T0 <: Arbitrary<T0>,
              T1 <: Arbitrary<T1>,
              T2 <: Arbitrary<T2>,
              T3 <: Arbitrary<T3> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<TupleWrapper4<T0, T1, T2, T3>>
}

/*
* Arbitrary instance for tuple (through TupleWrapper classes)
* Generates random tuple values using random element values
*/
extend<T0, T1, T2, T3, T4> TupleWrapper5<T0, T1, T2, T3, T4> <: Arbitrary<TupleWrapper5<T0, T1, T2, T3, T4>>
        where T0 <: Arbitrary<T0>,
              T1 <: Arbitrary<T1>,
              T2 <: Arbitrary<T2>,
              T3 <: Arbitrary<T3>,
              T4 <: Arbitrary<T4> {
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitrary(random: RandomSource): Generator<TupleWrapper5<T0, T1, T2, T3, T4>>
}

@!APILevel[since: "12", atomicservice : true]
public interface ArbitraryRange<T> where T <: Arbitrary<T> & Comparable<T> {
}

extend Int64 <: ArbitraryRange<Int64> {
    @!APILevel[since: "12", atomicservice : true]
    public static func min(): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public static func max(): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitraryRange(random: RandomSource, min: Int64, max: Int64): Generator<Int64>
}

extend UInt64 <: ArbitraryRange<UInt64> {
    @!APILevel[since: "12", atomicservice : true]
    public static func min(): UInt64
    
    @!APILevel[since: "12", atomicservice : true]
    public static func max(): UInt64
    
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitraryRange(random: RandomSource, min: UInt64, max: UInt64): Generator<UInt64>
}

extend Int32 <: ArbitraryRange<Int32> {
    @!APILevel[since: "12", atomicservice : true]
    public static func min(): Int32
    
    @!APILevel[since: "12", atomicservice : true]
    public static func max(): Int32
    
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitraryRange(random: RandomSource, min: Int32, max: Int32): Generator<Int32>
}

extend UInt32 <: ArbitraryRange<UInt32> {
    @!APILevel[since: "12", atomicservice : true]
    public static func min(): UInt32
    
    @!APILevel[since: "12", atomicservice : true]
    public static func max(): UInt32
    
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitraryRange(random: RandomSource, min: UInt32, max: UInt32): Generator<UInt32>
}

extend Int16 <: ArbitraryRange<Int16> {
    @!APILevel[since: "12", atomicservice : true]
    public static func min(): Int16
    
    @!APILevel[since: "12", atomicservice : true]
    public static func max(): Int16
    
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitraryRange(random: RandomSource, min: Int16, max: Int16): Generator<Int16>
}

extend UInt16 <: ArbitraryRange<UInt16> {
    @!APILevel[since: "12", atomicservice : true]
    public static func min(): UInt16
    
    @!APILevel[since: "12", atomicservice : true]
    public static func max(): UInt16
    
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitraryRange(random: RandomSource, min: UInt16, max: UInt16): Generator<UInt16>
}

extend Int8 <: ArbitraryRange<Int8> {
    @!APILevel[since: "12", atomicservice : true]
    public static func min(): Int8
    
    @!APILevel[since: "12", atomicservice : true]
    public static func max(): Int8
    
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitraryRange(random: RandomSource, min: Int8, max: Int8): Generator<Int8>
}

extend UInt8 <: ArbitraryRange<UInt8> {
    @!APILevel[since: "12", atomicservice : true]
    public static func min(): UInt8
    
    @!APILevel[since: "12", atomicservice : true]
    public static func max(): UInt8
    
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitraryRange(random: RandomSource, min: UInt8, max: UInt8): Generator<UInt8>
}

extend IntNative <: ArbitraryRange<IntNative> {
    @!APILevel[since: "12", atomicservice : true]
    public static func min(): IntNative
    
    @!APILevel[since: "12", atomicservice : true]
    public static func max(): IntNative
    
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitraryRange(random: RandomSource, min: IntNative, max: IntNative): Generator<IntNative>
}

extend UIntNative <: ArbitraryRange<UIntNative> {
    @!APILevel[since: "12", atomicservice : true]
    public static func min(): UIntNative
    
    @!APILevel[since: "12", atomicservice : true]
    public static func max(): UIntNative
    
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitraryRange(random: RandomSource, min: UIntNative, max: UIntNative): Generator<UIntNative>
}

extend Float64 <: ArbitraryRange<Float64> {
    @!APILevel[since: "12", atomicservice : true]
    public static func min(): Float64
    
    @!APILevel[since: "12", atomicservice : true]
    public static func max(): Float64
    
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitraryRange(random: RandomSource, min: Float64, max: Float64): Generator<Float64>
}

extend Float32 <: ArbitraryRange<Float32> {
    @!APILevel[since: "12", atomicservice : true]
    public static func min(): Float32
    
    @!APILevel[since: "12", atomicservice : true]
    public static func max(): Float32
    
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitraryRange(random: RandomSource, min: Float32, max: Float32): Generator<Float32>
}

extend Float16 <: ArbitraryRange<Float16> {
    @!APILevel[since: "12", atomicservice : true]
    public static func min(): Float16
    
    @!APILevel[since: "12", atomicservice : true]
    public static func max(): Float16
    
    @!APILevel[since: "12", atomicservice : true]
    public static func arbitraryRange(random: RandomSource, min: Float16, max: Float16): Generator<Float16>
}

/**
* Empty iterable: iterator that returns nothing
**/
@!APILevel[since: "12", atomicservice : true]
public func emptyIterable<T>(): Iterable<T>

/**
* Generator: similar to an iterator, but next() is non-optional, so the values are produced infinitely
*/
@!APILevel[since: "12", atomicservice : true]
public interface Generator<T> {
}

/**
* Class that contains helper functions to help developer write their own Generators
**/
@!APILevel[since: "12", atomicservice : true]
public class Generators {
    /**
    * Generator that generates values by repeatedly calling a function body that generates random values in [l, r]
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func generate<T>(l: T, r: T, body: (T, T) -> T): Generator<T>
    
    /**
    * Generator that always returns the same value
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func single<T>(value: T): Generator<T>
    
    /**
    * Generator that generates values by repeatedly calling a function
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func generate<T>(body: () -> T): Generator<T>
    
    /**
    * Generator that generates values by picking randomly from an array
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func iterable<T>(random: RandomSource, collection: Array<T>): Generator<T>
    
    /**
    * Generator that generates values by picking randomly from an array of pairs (weight, generator).
    * The result is randomly chosen between the generators in the array with higher weight having higher probability.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func weighted<T>(random: RandomSource, variants: Array<(UInt64, Generator<T>)>): Generator<T>
    
    /**
    * Generator that generates values by picking randomly from an array of generators.
    * The result is randomly chosen between the generators in the array uniformly.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func pick<T>(random: RandomSource, variants: Array<Generator<T>>): Generator<T>
    
    /**
    * Get the generator provided by T's Arbitrary instance
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func lookup<T>(random: RandomSource): Generator<T> where T <: Arbitrary<T>
    
    /**
    * Get the generator provided by T's Arbitrary instance and use function body to generate values of type R
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func mapped<T, R>(random: RandomSource, body: (T) -> R): Generator<R> where T <: Arbitrary<T>
    
    /**
    * Get the generators provided by T1 and T2 Arbitrary instances and use function body to generate values of type R
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func mapped<T1, T2, R>(random: RandomSource, body: (T1, T2) -> R): Generator<R> where T1 <: Arbitrary<T1>, T2 <: Arbitrary<T2>
    
    /**
    * Get the generators provided by T1, T2 and T3 Arbitrary instances and use function body to generate values of type R
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func mapped<T1, T2, T3, R>(random: RandomSource, body: (T1, T2, T3) -> R): Generator<R>
            where T1 <: Arbitrary<T1>, T2 <: Arbitrary<T2>, T3 <: Arbitrary<T3>
    
    /**
    * Get the generators provided by T1, T2, T3 and T4 Arbitrary instances and use function body to generate values of type R
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func mapped<T1, T2, T3, T4, R>(random: RandomSource, body: (T1, T2, T3, T4) -> R): Generator<R>
            where T1 <: Arbitrary<T1>, T2 <: Arbitrary<T2>, T3 <: Arbitrary<T3>, T4 <: Arbitrary<T4>
}

/*
* A lazily computed sequence of values of type T.
* Useful to compute and memoize values while iterating.
* This is completely immutable and every operation produces a new sequence.
* Implementation note: recursion is OK in most of the methods of this class
* because it will not cause a stack overflow during iteration.
*/
@!APILevel[since: "12", atomicservice : true]
public class LazySeq<T> <: Iterable<T> {
    /*
    * Construct a sequence from an iterable.
    * If the iterable is already a LazySeq, do not do anything to avoid producing unnesessary wrappers.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func of(iterable: Iterable<T>): LazySeq
    
    /*
    * Construct a sequence from an array.
    * A little bit optimized compared to the generic Iterable version.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func of(array: Array<T>): LazySeq
    
    /**
    * Make a sequence of one element `element`
    */
    @!APILevel[since: "12", atomicservice : true]
    public init(element: T)
    
    /**
    * Make an empty sequence
    */
    @!APILevel[since: "12", atomicservice : true]
    public init()
    
    /**
    * Iterator implementation
    */
    @!APILevel[since: "12", atomicservice : true]
    public func iterator(): Iterator<T>
    
    /*
    * Append another sequence to this sequence
    * Note: this is actually O(1) because of laziness
    * @param `other` other sequence to append
    */
    @!APILevel[since: "12", atomicservice : true]
    public func concat(other: LazySeq<T>): LazySeq<T>
    
    /*
    * Append an element to the beginning of this sequence
    */
    @!APILevel[since: "12", atomicservice : true]
    public func prepend(element: T): LazySeq<T>
    
    /*
    * Append an element to the end of this sequence
    */
    @!APILevel[since: "12", atomicservice : true]
    public func append(element: T): LazySeq<T>
    
    /*
    * Mix elements of this sequence with another sequence.
    * Example: {1,2,3,4}.mixWith({5,6,7}) -> {1,5,2,6,3,7,4}
    */
    @!APILevel[since: "12", atomicservice : true]
    public func mixWith(other: LazySeq<T>): LazySeq<T>
    
    /*
    * Mix two sequences.
    * Example: mix({1,2,3,4}, {5,6,7}) -> {1,5,2,6,3,7,4}
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func mix(l1: LazySeq<T>, l2: LazySeq<T>): LazySeq
    
    /*
    * Mix three sequences.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func mix(l1: LazySeq<T>, l2: LazySeq<T>, l3: LazySeq<T>): LazySeq
    
    /*
    * Mix four sequences.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func mix(l1: LazySeq<T>, l2: LazySeq<T>, l3: LazySeq<T>, l4: LazySeq<T>): LazySeq
    
    /*
    * Mix five sequences.
    */
    @!APILevel[since: "12", atomicservice : true]
    public static func mix(l1: LazySeq<T>, l2: LazySeq<T>, l3: LazySeq<T>, l4: LazySeq<T>, l5: LazySeq<T>): LazySeq
    
    /*
    * Apply a function to each element of this sequence, return the sequence of results.
    * Note: this is O(1) because of laziness
    */
    @!APILevel[since: "12", atomicservice : true]
    public func map<U>(body: (T) -> U): LazySeq<U>
}

/**
* Some very basic extensions for Random
*/
extend Random {
    @!APILevel[since: "12", atomicservice : true]
    public func pick<T>(choices: Array<T>): T
    
    @!APILevel[since: "12", atomicservice : true]
    public func pickWeighted<T>(choices: WeightTable<T>): T
}

extend Int16 <: FromFloat64<Int16> {
    @!APILevel[since: "12", atomicservice : true]
    public static func fromFloat64(f: Float64): Int16
}

extend Int32 <: FromFloat64<Int32> {
    @!APILevel[since: "12", atomicservice : true]
    public static func fromFloat64(f: Float64): Int32
}

extend Int64 <: FromFloat64<Int64> {
    @!APILevel[since: "12", atomicservice : true]
    public static func fromFloat64(f: Float64): Int64
}

extend IntNative <: FromFloat64<IntNative> {
    @!APILevel[since: "12", atomicservice : true]
    public static func fromFloat64(f: Float64): IntNative
}

extend UInt16 <: FromFloat64<UInt16> {
    @!APILevel[since: "12", atomicservice : true]
    public static func fromFloat64(f_: Float64): UInt16
}

extend UInt32 <: FromFloat64<UInt32> {
    @!APILevel[since: "12", atomicservice : true]
    public static func fromFloat64(f_: Float64): UInt32
}

extend UInt64 <: FromFloat64<UInt64> {
    @!APILevel[since: "12", atomicservice : true]
    public static func fromFloat64(f_: Float64): UInt64
}

extend UIntNative <: FromFloat64<UIntNative> {
    @!APILevel[since: "12", atomicservice : true]
    public static func fromFloat64(f_: Float64): UIntNative
}

extend Random <: RandomSource {
    @!APILevel[since: "12", atomicservice : true]
    public func suggestBool(): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestInt8(): Int8
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestInt16(): Int16
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestInt32(): Int32
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestInt64(): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func suggestIntNative(): IntNative
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestUInt8(): UInt8
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestUInt16(): UInt16
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestUInt32(): UInt32
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestUInt64(): UInt64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func suggestUIntNative(): UIntNative
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestFloat16(): Float16
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestFloat32(): Float32
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestFloat64(): Float64
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestRune(): Rune
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func nextIntNative(): IntNative
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func nextUIntNative(): UIntNative
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestInt64(l: Int64, r: Int64): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestUInt64(l: UInt64, r: UInt64): UInt64
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestInt32(l: Int32, r: Int32): Int32
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestUInt32(l: UInt32, r: UInt32): UInt32
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestInt16(l: Int16, r: Int16): Int16
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestUInt16(l: UInt16, r: UInt16): UInt16
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestInt8(l: Int8, r: Int8): Int8
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestUInt8(l: UInt8, r: UInt8): UInt8
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestIntNative(l: IntNative, r: IntNative): IntNative
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestUIntNative(l: UIntNative, r: UIntNative): UIntNative
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestFloat64(l: Float64, r: Float64): Float64
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestFloat32(l: Float32, r: Float32): Float32
    
    @!APILevel[since: "12", atomicservice : true]
    public func suggestFloat16(l: Float16, r: Float16): Float16
}

@!APILevel[since: "12", atomicservice : true]
public interface RandomSource {
}

@!APILevel[since: "12", atomicservice : true]
public struct KeyRandom <: KeyFor<RandomSource> {
    @!APILevel[since: "12", atomicservice : true]
    static public prop random: KeyRandom
    
    @!APILevel[since: "12", atomicservice : true]
    public prop name: String
}

/**
* An implementation of DataProvider interface using random data generation.
* Supported configuration:
* - "random" of type std.random.Random the random generator to use
* @param T the type of data to provide
*/
@!APILevel[since: "12", atomicservice : true]
public class RandomDataProvider<T> <: DataProvider<T> where T <: Arbitrary<T> {
    /**
    * Random data provider constructor
    * @param configuration the configuration object, this must contain a random generator
    *                      with name "random" and type random.RandomSource
    * @throws IllegalArgumentException if the configuration does not contain instance of random
    */
    @!APILevel[since: "12", atomicservice : true]
    public RandomDataProvider(private let configuration: Configuration)
    
    /**
    * Provide the randomly generated data
    * @return An infinite Iterable created from Arbitrary instance for T
    */
    @!APILevel[since: "12", atomicservice : true]
    public override func provide(): Iterable<T>
}

/**
* An implementation of DataShrinker interface using random data generation.
* @param T the type of data to reduce
*/
@!APILevel[since: "12", atomicservice : true]
public class RandomDataShrinker<T> <: DataShrinker<T> {
    /**
    * Reduce the given value using Shrink instance
    * @return if value implements Shrink<T>, return reduced values
    *         if value does not implement Shrink<T>, return empty array
    */
    @!APILevel[since: "12", atomicservice : true]
    public override func shrink(value: T): Iterable<T>
}

/**
* An implementation of DataStrategy interface using random data generation.
* @param T the type of data to generate
*/
@!APILevel[since: "12", atomicservice : true]
public class RandomDataStrategy<T> <: DataStrategy<T> where T <: Arbitrary<T> {
    /**
    * Get the provider of random data. Returns an instance of RandomDataProvider
    * @param configuration the configuration object
    *        Uses the following configuration keys:
    *        - "random" of type std.random.Random: the random generator to use for generation
    */
    @!APILevel[since: "12", atomicservice : true]
    public override func provider(configuration: Configuration): RandomDataProvider<T>
    
    /**
    * Get the reducer of random data. Returns an instance of RandomDataShrinker
    * @param configuration the configuration object, ignored for now
    */
    @!APILevel[since: "12", atomicservice : true]
    public override func shrinker(_: Configuration): RandomDataShrinker<T>
    
    /**
    * Does this produce infinite data?
    * @return always true
    */
    @!APILevel[since: "12", atomicservice : true]
    public override prop isInfinite: Bool
}

/**
* An implementation of DataStrategy interface using random data generation.
* @param T the type of data to generate
* @return an instance of RandomDataStrategy<T>
*/
@!APILevel[since: "12", atomicservice : true]
public func random<T>(): RandomDataStrategy<T> where T <: Arbitrary<T>

@!APILevel[since: "12", atomicservice : true]
public class RandomDataProviderRange<T> <: DataProvider<T> where T <: ArbitraryRange<T> {
    @!APILevel[since: "12", atomicservice : true]
    public RandomDataProviderRange(private let configuration: Configuration, private let min: T, private let max: T)
    
    @!APILevel[since: "12", atomicservice : true]
    public override func provide(): Iterable<T>
}

@!APILevel[since: "12", atomicservice : true]
public class RandomDataShrinkerRange<T> <: DataShrinker<T> where T <: Comparable<T> {
    @!APILevel[since: "12", atomicservice : true]
    public override func shrink(value: T): Iterable<T>
}

@!APILevel[since: "12", atomicservice : true]
public class RandomDataStrategyRange<T> <: DataStrategy<T> where T <: ArbitraryRange<T> {
    @!APILevel[since: "12", atomicservice : true]
    public override func provider(configuration: Configuration): RandomDataProviderRange<T>
    
    @!APILevel[since: "12", atomicservice : true]
    public override func shrinker(_: Configuration): RandomDataShrinkerRange<T>
    
    @!APILevel[since: "12", atomicservice : true]
    public override prop isInfinite: Bool
}

@!APILevel[since: "12", atomicservice : true]
public func randomInRange<T>(min!: Option<T> = None, max!: Option<T> = None): RandomDataStrategyRange<T> where T <: ArbitraryRange<T>

/**
* Shrink interface: reduce values of type T to a number of "smaller" values
*/
@!APILevel[since: "12", atomicservice : true]
public interface Shrink<T> {
}

extend Unit <: Shrink<Unit> {
}

extend Bool <: Shrink<Bool> {
}

/*****************************************************************
@ForEach[IntType in [Int8, Int16, Int32, Int64, IntNative]]
extend IntType <: Shrink<IntType> {
public func shrink(): Iterable<IntType> {
let res = HashSet<IntType>()
if (this == 0) { return [] }
if (this == 1) { return [0] }
res.add(0)
res.add(1)
if (this < 0) {
res.add(-this)
}
res.add(this / 2)
res
}
}
******************************************************************//**
* Shrink instance for signed integer
*/
extend Int8 <: Shrink<Int8> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<Int8>
}

/**
* Shrink instance for signed integer
*/
extend Int16 <: Shrink<Int16> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<Int16>
}

/**
* Shrink instance for signed integer
*/
extend Int32 <: Shrink<Int32> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<Int32>
}

/**
* Shrink instance for signed integer
*/
extend Int64 <: Shrink<Int64> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<Int64>
}

/**
* Shrink instance for signed integer
*/
extend IntNative <: Shrink<IntNative> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<IntNative>
}

/********************************************************************
@ForEach[UIntType in [UInt8, UInt16, UInt32, UInt64, UIntNative]]
extend UIntType <: Shrink<UIntType> {
public func shrink(): Iterable<UIntType> {
let res = HashSet<UIntType>()
if (this == 0) { return [] }
if (this == 1) { return [0] }
res.add(0)
res.add(1)
res.add(this / 2)
res
}
}
*********************************************************************//**
* Shrink instance for unsigned integer
*/
extend UInt8 <: Shrink<UInt8> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<UInt8>
}

/**
* Shrink instance for unsigned integer
*/
extend UInt16 <: Shrink<UInt16> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<UInt16>
}

/**
* Shrink instance for unsigned integer
*/
extend UInt32 <: Shrink<UInt32> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<UInt32>
}

/**
* Shrink instance for unsigned integer
*/
extend UInt64 <: Shrink<UInt64> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<UInt64>
}

/**
* Shrink instance for unsigned integer
*/
extend UIntNative <: Shrink<UIntNative> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<UIntNative>
}

extend Float16 <: Shrink<Float16> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<Float16>
}

extend Float32 <: Shrink<Float32> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<Float32>
}

extend Float64 <: Shrink<Float64> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<Float64>
}

/**
* Shrink instance for Rune
*/
extend Rune <: Shrink<Rune> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<Rune>
}

extend <T> Array<T> <: CanShrinkElements<Array<T>> where T <: Shrink<T> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrinkUsingElements(): LazySeq<Array<T>>
}

extend <T> Array<T> <: Shrink<Array<T>> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<Array<T>>
}

extend <T> ArrayList<T> <: CanShrinkElements<ArrayList<T>> where T <: Shrink<T> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrinkUsingElements(): LazySeq<ArrayList<T>>
}

extend <T> ArrayList<T> <: Shrink<ArrayList<T>> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<ArrayList<T>>
}

extend <T> HashSet<T> <: CanShrinkElements<HashSet<T>> where T <: Shrink<T> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrinkUsingElements(): LazySeq<HashSet<T>>
}

extend <T> HashSet<T> <: Shrink<HashSet<T>> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<HashSet<T>>
}

/**
* ShrinkKeyWrapper<K, V> implements CanShrinkElements<HashMap<K, V>> iff K is Shrink<K>
* and provides shrinking via keys
*/
extend <K, V> ShrinkKeyWrapper<K, V> <: CanShrinkElements<HashMap<K, V>> where K <: Shrink<K> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrinkUsingElements(): LazySeq<HashMap<K, V>>
}

/**
* HashMap<K, V> implements CanShrinkElements<HashMap<K, V>> iff V is Shrink<V>
* and provides shrinking via values
*/
extend <K, V> HashMap<K, V> <: CanShrinkElements<HashMap<K, V>> where V <: Shrink<V> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrinkUsingElements(): LazySeq<HashMap<K, V>>
}

extend <K, V> HashMap<K, V> <: Shrink<HashMap<K, V>> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<HashMap<K, V>>
}

extend String <: Shrink<String> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<String>
}

extend<T> Option<T> <: CanShrinkElements<Option<T>> where T <: Shrink<T> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrinkUsingElements(): LazySeq<Option<T>>
}

extend<T> Option<T> <: Shrink<Option<T>> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<Option<T>>
}

extend<R> Function0Wrapper<R> <: Shrink<Function0Wrapper<R>> {
}

extend<T0, T1> TupleWrapper2<T0, T1> <: Shrink<TupleWrapper2<T0, T1>>
        where T0 <: Shrink<T0>,
              T1 <: Shrink<T1> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<TupleWrapper2<T0, T1>>
}

extend<T0, T1, T2> TupleWrapper3<T0, T1, T2> <: Shrink<TupleWrapper3<T0, T1, T2>>
        where T0 <: Shrink<T0>,
              T1 <: Shrink<T1>,
              T2 <: Shrink<T2> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<TupleWrapper3<T0, T1, T2>>
}

extend<T0, T1, T2, T3> TupleWrapper4<T0, T1, T2, T3> <: Shrink<TupleWrapper4<T0, T1, T2, T3>>
        where T0 <: Shrink<T0>,
              T1 <: Shrink<T1>,
              T2 <: Shrink<T2>,
              T3 <: Shrink<T3> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<TupleWrapper4<T0, T1, T2, T3>>
}

extend<T0, T1, T2, T3, T4> TupleWrapper5<T0, T1, T2, T3, T4> <: Shrink<TupleWrapper5<T0, T1, T2, T3, T4>>
        where T0 <: Shrink<T0>,
              T1 <: Shrink<T1>,
              T2 <: Shrink<T2>,
              T3 <: Shrink<T3>,
              T4 <: Shrink<T4> {
    @!APILevel[since: "12", atomicservice : true]
    public func shrink(): Iterable<TupleWrapper5<T0, T1, T2, T3, T4>>
}

/**
* Utility class used to help developers write their own implementations of Shrink
*/
@!APILevel[since: "12", atomicservice : true]
public class ShrinkHelpers {
    /**
    * Build a shrinking sequence for tuple (T0, T1) from sequences for T0 and T1
    **/
    @!APILevel[since: "12", atomicservice : true]
    public static func shrinkTuple<T0, T1>(
        tuple: (T0, T1),
        t0: Iterable<T0>,
        t1: Iterable<T1>
    ): Iterable<(T0, T1)>
    
    /**
    * Build a shrinking sequence for tuple (T0, T1, T2) from sequences for T0, T1 and T2
    **/
    @!APILevel[since: "12", atomicservice : true]
    public static func shrinkTuple<T0, T1, T2>(
        tuple: (T0, T1, T2),
        t0: Iterable<T0>,
        t1: Iterable<T1>,
        t2: Iterable<T2>
    ): Iterable<(T0, T1, T2)>
    
    /**
    * Build a shrinking sequence for tuple (T0, T1, T2, T3) from sequences for T0, T1, T2 and T3
    **/
    @!APILevel[since: "12", atomicservice : true]
    public static func shrinkTuple<T0, T1, T2, T3>(
        tuple: (T0, T1, T2, T3),
        t0: Iterable<T0>,
        t1: Iterable<T1>,
        t2: Iterable<T2>,
        t3: Iterable<T3>
    ): Iterable<(T0, T1, T2, T3)>
    
    /**
    * Build a shrinking sequence for tuple (T0, T1, T2, T3, T4) from sequences for T0, T1, T2, T3 and T4
    **/
    @!APILevel[since: "12", atomicservice : true]
    public static func shrinkTuple<T0, T1, T2, T3, T4>(
        tuple: (T0, T1, T2, T3, T4),
        t0: Iterable<T0>,
        t1: Iterable<T1>,
        t2: Iterable<T2>,
        t3: Iterable<T3>,
        t4: Iterable<T4>
    ): Iterable<(T0, T1, T2, T3, T4)>
    
    @!APILevel[since: "12", atomicservice : true]
    public static func mix<T>(iterables: Array<Iterable<T>>): Iterable<T>
}

/**
* Wrapper struct over function of type `() -> R`
*/
@!APILevel[since: "12", atomicservice : true]
public struct Function0Wrapper<R> {
    @!APILevel[since: "12", atomicservice : true]
    public Function0Wrapper(public let function: () -> R)
    
    @!APILevel[since: "12", atomicservice : true]
    public operator func () (): R
}

/**
* Wrapper struct over tuple 
*/
@!APILevel[since: "12", atomicservice : true]
public struct TupleWrapper2<T0, T1> {
    @!APILevel[since: "12", atomicservice : true]
    public TupleWrapper2(public let tuple: (T0, T1))
    
    @!APILevel[since: "12", atomicservice : true]
    public func apply<R>(f: (T0, T1) -> R): R
}

/**
* Wrapper struct over tuple 
*/
@!APILevel[since: "12", atomicservice : true]
public struct TupleWrapper3<T0, T1, T2> {
    @!APILevel[since: "12", atomicservice : true]
    public TupleWrapper3(public let tuple: (T0, T1, T2))
    
    @!APILevel[since: "12", atomicservice : true]
    public func apply<R>(f: (T0, T1, T2) -> R): R
}

/**
* Wrapper struct over tuple 
*/
@!APILevel[since: "12", atomicservice : true]
public struct TupleWrapper4<T0, T1, T2, T3> {
    @!APILevel[since: "12", atomicservice : true]
    public TupleWrapper4(public let tuple: (T0, T1, T2, T3))
    
    @!APILevel[since: "12", atomicservice : true]
    public func apply<R>(f: (T0, T1, T2, T3) -> R): R
}

/**
* Wrapper struct over tuple 
*/
@!APILevel[since: "12", atomicservice : true]
public struct TupleWrapper5<T0, T1, T2, T3, T4> {
    @!APILevel[since: "12", atomicservice : true]
    public TupleWrapper5(public let tuple: (T0, T1, T2, T3, T4))
    
    @!APILevel[since: "12", atomicservice : true]
    public func apply<R>(f: (T0, T1, T2, T3, T4) -> R): R
}

/**
* ToString implementations, behave exactly the same as toString() for tuples
*/
extend<T0, T1> TupleWrapper2<T0, T1> <: ToString
        where T0 <: ToString, T1 <: ToString {
    @!APILevel[since: "12", atomicservice : true]
    public func toString()
}

extend<T0, T1, T2> TupleWrapper3<T0, T1, T2> <: ToString
        where T0 <: ToString, T1 <: ToString, T2 <: ToString {
    @!APILevel[since: "12", atomicservice : true]
    public func toString()
}

extend<T0, T1, T2, T3> TupleWrapper4<T0, T1, T2, T3> <: ToString
        where T0 <: ToString, T1 <: ToString, T2 <: ToString, T3 <: ToString {
    @!APILevel[since: "12", atomicservice : true]
    public func toString()
}

extend<T0, T1, T2, T3, T4> TupleWrapper5<T0, T1, T2, T3, T4> <: ToString
        where T0 <: ToString, T1 <: ToString, T2 <: ToString, T3 <: ToString, T4 <: ToString {
    @!APILevel[since: "12", atomicservice : true]
    public func toString()
}

/**
* Equatable implementations, behave exactly the same as equals() for tuples
*/
extend<T0, T1> TupleWrapper2<T0, T1> <: Equatable<TupleWrapper2<T0, T1>>
        where T0 <: Equatable<T0>, T1 <: Equatable<T1> {
    @!APILevel[since: "12", atomicservice : true]
    public operator func ==(other: TupleWrapper2<T0, T1>)
    
    @!APILevel[since: "12", atomicservice : true]
    public operator func !=(other: TupleWrapper2<T0, T1>)
}

extend<T0, T1, T2> TupleWrapper3<T0, T1, T2> <: Equatable<TupleWrapper3<T0, T1, T2>>
        where T0 <: Equatable<T0>, T1 <: Equatable<T1>, T2 <: Equatable<T2> {
    @!APILevel[since: "12", atomicservice : true]
    public operator func ==(other: TupleWrapper3<T0, T1, T2>)
    
    @!APILevel[since: "12", atomicservice : true]
    public operator func !=(other: TupleWrapper3<T0, T1, T2>)
}

extend<T0, T1, T2, T3> TupleWrapper4<T0, T1, T2, T3> <: Equatable<TupleWrapper4<T0, T1, T2, T3>>
        where T0 <: Equatable<T0>, T1 <: Equatable<T1>, T2 <: Equatable<T2>, T3 <: Equatable<T3> {
    @!APILevel[since: "12", atomicservice : true]
    public operator func ==(other: TupleWrapper4<T0, T1, T2, T3>)
    
    @!APILevel[since: "12", atomicservice : true]
    public operator func !=(other: TupleWrapper4<T0, T1, T2, T3>)
}

extend<T0, T1, T2, T3, T4> TupleWrapper5<T0, T1, T2, T3, T4> <: Equatable<TupleWrapper5<T0, T1, T2, T3, T4>>
        where T0 <: Equatable<T0>, T1 <: Equatable<T1>, T2 <: Equatable<T2>, T3 <: Equatable<T3>, T4 <: Equatable<T4> {
    @!APILevel[since: "12", atomicservice : true]
    public operator func ==(other: TupleWrapper5<T0, T1, T2, T3, T4>)
    
    @!APILevel[since: "12", atomicservice : true]
    public operator func !=(other: TupleWrapper5<T0, T1, T2, T3, T4>)
}

/**
* Utility interface to get access to elements of tuples by index
* This is needed to print values
*/
@!APILevel[since: "12", atomicservice : true]
public interface IndexAccess {
}

extend<T0, T1> TupleWrapper2<T0, T1> <: IndexAccess {
    @!APILevel[since: "12", atomicservice : true]
    public func getElementAsAny(index: Int64): ?Any
}

extend<T0, T1, T2> TupleWrapper3<T0, T1, T2> <: IndexAccess {
    @!APILevel[since: "12", atomicservice : true]
    public func getElementAsAny(index: Int64): ?Any
}

extend<T0, T1, T2, T3> TupleWrapper4<T0, T1, T2, T3> <: IndexAccess {
    @!APILevel[since: "12", atomicservice : true]
    public func getElementAsAny(index: Int64): ?Any
}

extend<T0, T1, T2, T3, T4> TupleWrapper5<T0, T1, T2, T3, T4> <: IndexAccess {
    @!APILevel[since: "12", atomicservice : true]
    public func getElementAsAny(index: Int64): ?Any
}

