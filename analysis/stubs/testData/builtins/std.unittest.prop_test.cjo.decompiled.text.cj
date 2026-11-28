// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.unittest.prop_test

import std.collection.*
import std.math.abs
import std.core.*
import std.random.Random
import std.math.*
import std.collection.filter
import std.unittest.common.*

public interface RandomSource {
    public open func nextBool(): Bool { /* compiled code */ }

    public open func nextInt8(): Int8 { /* compiled code */ }

    public open func nextInt16(): Int16 { /* compiled code */ }

    public open func nextInt32(): Int32 { /* compiled code */ }

    public open func nextInt64(): Int64 { /* compiled code */ }

    public open func nextInt8(max: Int8): Int8 { /* compiled code */ }

    public open func nextInt16(max: Int16): Int16 { /* compiled code */ }

    public open func nextInt32(max: Int32): Int32 { /* compiled code */ }

    public open func nextInt64(max: Int64): Int64 { /* compiled code */ }

    public open func nextUInt8(): UInt8

    public open func nextUInt16(): UInt16

    public open func nextUInt32(): UInt32

    public open func nextUInt64(): UInt64

    public open func nextUInt8(max: UInt8): UInt8 { /* compiled code */ }

    public open func nextUInt16(max: UInt16): UInt16 { /* compiled code */ }

    public open func nextUInt32(max: UInt32): UInt32 { /* compiled code */ }

    public open func nextUInt64(max: UInt64): UInt64 { /* compiled code */ }

    public open func nextFloat16(): Float16

    public open func nextFloat32(): Float32

    public open func nextFloat64(): Float64

    public open func nextGaussianFloat64(mean!: Float64, sigma!: Float64): Float64

    public open func nextIntNative(): IntNative { /* compiled code */ }

    public open func nextUIntNative(): UIntNative { /* compiled code */ }

    public open func suggestUInt8(): UInt8

    public open func suggestUInt16(): UInt16

    public open func suggestUInt32(): UInt32

    public open func suggestUInt64(): UInt64

    public open func suggestUIntNative(): UIntNative { /* compiled code */ }

    public open func suggestInt8(): Int8

    public open func suggestInt16(): Int16

    public open func suggestInt32(): Int32

    public open func suggestInt64(): Int64

    public open func suggestIntNative(): IntNative { /* compiled code */ }

    public open func suggestFloat16(): Float16

    public open func suggestFloat32(): Float32

    public open func suggestFloat64(): Float64

    public open func suggestBool(): Bool { /* compiled code */ }

    public open func suggestRune(): Rune { /* compiled code */ }

    public open func suggestInt64(l: Int64, r: Int64): Int64 { /* compiled code */ }

    public open func suggestUInt64(l: UInt64, r: UInt64): UInt64 { /* compiled code */ }

    public open func suggestInt32(l: Int32, r: Int32): Int32 { /* compiled code */ }

    public open func suggestUInt32(l: UInt32, r: UInt32): UInt32 { /* compiled code */ }

    public open func suggestInt16(l: Int16, r: Int16): Int16 { /* compiled code */ }

    public open func suggestUInt16(l: UInt16, r: UInt16): UInt16 { /* compiled code */ }

    public open func suggestInt8(l: Int8, r: Int8): Int8 { /* compiled code */ }

    public open func suggestUInt8(l: UInt8, r: UInt8): UInt8 { /* compiled code */ }

    public open func suggestIntNative(l: IntNative, r: IntNative): IntNative { /* compiled code */ }

    public open func suggestUIntNative(l: UIntNative, r: UIntNative): UIntNative { /* compiled code */ }

    public open func suggestFloat64(l: Float64, r: Float64): Float64 { /* compiled code */ }

    public open func suggestFloat32(l: Float32, r: Float32): Float32 { /* compiled code */ }

    public open func suggestFloat16(l: Float16, r: Float16): Float16 { /* compiled code */ }
}

internal func randomSuggestedRune(random: std.unittest.prop_test.RandomSource): Rune { /* compiled code */ }

internal func remap(l: Int64, r: Int64, x: Int64): Int64 { /* compiled code */ }

internal func remap(l: UInt64, r: UInt64, x: UInt64): UInt64 { /* compiled code */ }

internal func remap(l: Int32, r: Int32, x: Int32): Int32 { /* compiled code */ }

internal func remap(l: UInt32, r: UInt32, x: UInt32): UInt32 { /* compiled code */ }

internal func remap(l: Int16, r: Int16, x: Int16): Int16 { /* compiled code */ }

internal func remap(l: UInt16, r: UInt16, x: UInt16): UInt16 { /* compiled code */ }

internal func remap(l: Int8, r: Int8, x: Int8): Int8 { /* compiled code */ }

internal func remap(l: UInt8, r: UInt8, x: UInt8): UInt8 { /* compiled code */ }

internal func remap(l: IntNative, r: IntNative, x: IntNative): IntNative { /* compiled code */ }

internal func remap(l: UIntNative, r: UIntNative, x: UIntNative): UIntNative { /* compiled code */ }

internal func remap(l: Float64, r: Float64, x: Float64): Float64 { /* compiled code */ }

internal func remap(l: Float32, r: Float32, x: Float32): Float32 { /* compiled code */ }

internal func remap(l: Float16, r: Float16, x: Float16): Float16 { /* compiled code */ }

public func emptyIterable<T>(): std.core.Iterable<T> { /* compiled code */ }

public interface Arbitrary<T> {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<T>
}

public interface Generator<T> {
    public open func next(): T
}

extend Unit <: Arbitrary {
    public static func arbitrary(_: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Unit> { /* compiled code */ }
}

extend Bool <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Bool> { /* compiled code */ }
}

extend Int8 <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Int8> { /* compiled code */ }
}

extend Int16 <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Int16> { /* compiled code */ }
}

extend Int32 <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Int32> { /* compiled code */ }
}

extend Int64 <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Int64> { /* compiled code */ }
}

extend IntNative <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<IntNative> { /* compiled code */ }
}

extend UInt8 <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UInt8> { /* compiled code */ }
}

extend UInt16 <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UInt16> { /* compiled code */ }
}

extend UInt32 <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UInt32> { /* compiled code */ }
}

extend UInt64 <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UInt64> { /* compiled code */ }
}

extend UIntNative <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UIntNative> { /* compiled code */ }
}

extend Float16 <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Float16> { /* compiled code */ }
}

extend Float32 <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Float32> { /* compiled code */ }
}

extend Float64 <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Float64> { /* compiled code */ }
}

extend Rune <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Rune> { /* compiled code */ }
}

extend std.core.Array<T> <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.core.Array<T>> { /* compiled code */ }
}

extend std.collection.ArrayList<T> <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.collection.ArrayList<T>> { /* compiled code */ }
}

extend std.collection.HashSet<T> <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.collection.HashSet<T>> { /* compiled code */ }
}

extend std.collection.HashMap<K, V> <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.collection.HashMap<K, V>> { /* compiled code */ }
}

extend std.core.String <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.core.String> { /* compiled code */ }
}

extend std.core.Option<T> <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.core.Option<T>> { /* compiled code */ }
}

extend std.core.Ordering <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.core.Ordering> { /* compiled code */ }
}

extend std.unittest.prop_test.Function0Wrapper<R> <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.Function0Wrapper<R>> { /* compiled code */ }
}

public struct Function0Wrapper<R> {
    public operator func ()(): R { /* compiled code */ }

    public let function: () -> R

    Function0Wrapper(function: () -> R) { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper2<T0, T1> <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.TupleWrapper2<T0, T1>> { /* compiled code */ }
}

public struct TupleWrapper2<T0, T1> {
    public func apply<R>(f: (T0, T1) -> R): R { /* compiled code */ }

    public let tuple: (T0, T1)

    TupleWrapper2(tuple: (T0, T1)) { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper3<T0, T1, T2> <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.TupleWrapper3<T0, T1, T2>> { /* compiled code */ }
}

public struct TupleWrapper3<T0, T1, T2> {
    public func apply<R>(f: (T0, T1, T2) -> R): R { /* compiled code */ }

    public let tuple: (T0, T1, T2)

    TupleWrapper3(tuple: (T0, T1, T2)) { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3> <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3>> { /* compiled code */ }
}

public struct TupleWrapper4<T0, T1, T2, T3> {
    public func apply<R>(f: (T0, T1, T2, T3) -> R): R { /* compiled code */ }

    public let tuple: (T0, T1, T2, T3)

    TupleWrapper4(tuple: (T0, T1, T2, T3)) { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4> <: Arbitrary {
    public static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4>> { /* compiled code */ }
}

public struct TupleWrapper5<T0, T1, T2, T3, T4> {
    public func apply<R>(f: (T0, T1, T2, T3, T4) -> R): R { /* compiled code */ }

    public let tuple: (T0, T1, T2, T3, T4)

    TupleWrapper5(tuple: (T0, T1, T2, T3, T4)) { /* compiled code */ }
}

public interface ArbitraryRange<T> {
    public static func min(): T

    public static func max(): T

    public static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: T, max: T): std.unittest.prop_test.Generator<T>
}

extend Int64 <: ArbitraryRange {
    public static func min(): Int64 { /* compiled code */ }

    public static func max(): Int64 { /* compiled code */ }

    public static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Int64, max: Int64): std.unittest.prop_test.Generator<Int64> { /* compiled code */ }
}

extend UInt64 <: ArbitraryRange {
    public static func min(): UInt64 { /* compiled code */ }

    public static func max(): UInt64 { /* compiled code */ }

    public static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UInt64, max: UInt64): std.unittest.prop_test.Generator<UInt64> { /* compiled code */ }
}

extend Int32 <: ArbitraryRange {
    public static func min(): Int32 { /* compiled code */ }

    public static func max(): Int32 { /* compiled code */ }

    public static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Int32, max: Int32): std.unittest.prop_test.Generator<Int32> { /* compiled code */ }
}

extend UInt32 <: ArbitraryRange {
    public static func min(): UInt32 { /* compiled code */ }

    public static func max(): UInt32 { /* compiled code */ }

    public static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UInt32, max: UInt32): std.unittest.prop_test.Generator<UInt32> { /* compiled code */ }
}

extend Int16 <: ArbitraryRange {
    public static func min(): Int16 { /* compiled code */ }

    public static func max(): Int16 { /* compiled code */ }

    public static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Int16, max: Int16): std.unittest.prop_test.Generator<Int16> { /* compiled code */ }
}

extend UInt16 <: ArbitraryRange {
    public static func min(): UInt16 { /* compiled code */ }

    public static func max(): UInt16 { /* compiled code */ }

    public static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UInt16, max: UInt16): std.unittest.prop_test.Generator<UInt16> { /* compiled code */ }
}

extend Int8 <: ArbitraryRange {
    public static func min(): Int8 { /* compiled code */ }

    public static func max(): Int8 { /* compiled code */ }

    public static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Int8, max: Int8): std.unittest.prop_test.Generator<Int8> { /* compiled code */ }
}

extend UInt8 <: ArbitraryRange {
    public static func min(): UInt8 { /* compiled code */ }

    public static func max(): UInt8 { /* compiled code */ }

    public static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UInt8, max: UInt8): std.unittest.prop_test.Generator<UInt8> { /* compiled code */ }
}

extend IntNative <: ArbitraryRange {
    public static func min(): IntNative { /* compiled code */ }

    public static func max(): IntNative { /* compiled code */ }

    public static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: IntNative, max: IntNative): std.unittest.prop_test.Generator<IntNative> { /* compiled code */ }
}

extend UIntNative <: ArbitraryRange {
    public static func min(): UIntNative { /* compiled code */ }

    public static func max(): UIntNative { /* compiled code */ }

    public static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UIntNative, max: UIntNative): std.unittest.prop_test.Generator<UIntNative> { /* compiled code */ }
}

extend Float64 <: ArbitraryRange {
    public static func min(): Float64 { /* compiled code */ }

    public static func max(): Float64 { /* compiled code */ }

    public static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Float64, max: Float64): std.unittest.prop_test.Generator<Float64> { /* compiled code */ }
}

extend Float32 <: ArbitraryRange {
    public static func min(): Float32 { /* compiled code */ }

    public static func max(): Float32 { /* compiled code */ }

    public static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Float32, max: Float32): std.unittest.prop_test.Generator<Float32> { /* compiled code */ }
}

extend Float16 <: ArbitraryRange {
    public static func min(): Float16 { /* compiled code */ }

    public static func max(): Float16 { /* compiled code */ }

    public static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Float16, max: Float16): std.unittest.prop_test.Generator<Float16> { /* compiled code */ }
}

public class Generators {
    public static func generate<T>(l: T, r: T, body: (T, T) -> T): std.unittest.prop_test.Generator<T> { /* compiled code */ }

    public static func single<T>(value: T): std.unittest.prop_test.Generator<T> { /* compiled code */ }

    public static func generate<T>(body: () -> T): std.unittest.prop_test.Generator<T> { /* compiled code */ }

    public static func iterable<T>(random: std.unittest.prop_test.RandomSource, collection: std.core.Array<T>): std.unittest.prop_test.Generator<T> { /* compiled code */ }

    public static func weighted<T>(random: std.unittest.prop_test.RandomSource, variants: std.core.Array<(UInt64, std.unittest.prop_test.Generator<T>)>): std.unittest.prop_test.Generator<T> { /* compiled code */ }

    public static func pick<T>(random: std.unittest.prop_test.RandomSource, variants: std.core.Array<std.unittest.prop_test.Generator<T>>): std.unittest.prop_test.Generator<T> { /* compiled code */ }

    public static func lookup<T>(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<T> { /* compiled code */ }

    public static func mapped<T, R>(random: std.unittest.prop_test.RandomSource, body: (T) -> R): std.unittest.prop_test.Generator<R> { /* compiled code */ }

    public static func mapped<T1, T2, R>(random: std.unittest.prop_test.RandomSource, body: (T1, T2) -> R): std.unittest.prop_test.Generator<R> { /* compiled code */ }

    public static func mapped<T1, T2, T3, R>(random: std.unittest.prop_test.RandomSource, body: (T1, T2, T3) -> R): std.unittest.prop_test.Generator<R> { /* compiled code */ }

    public static func mapped<T1, T2, T3, T4, R>(random: std.unittest.prop_test.RandomSource, body: (T1, T2, T3, T4) -> R): std.unittest.prop_test.Generator<R> { /* compiled code */ }

    init() { /* compiled code */ }
}

internal enum Thunk<T> {
    | Uncalculated(() -> std.core.Option<T>)

    | Calculated(T)

    | EmptyThunk
}

internal struct Lazy<T> {
    private var thunk: std.unittest.prop_test.Thunk<T>

    public mut func get(): std.core.Option<T> { /* compiled code */ }

    internal static func flat<T>(body: () -> std.unittest.prop_test.Lazy<T>): std.unittest.prop_test.Lazy<T> { /* compiled code */ }
}

internal class LazyNode<T> {
    public let head: T

    internal var lazyTail: std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>>

    public prop tail: std.core.Option<std.unittest.prop_test.LazyNode<T>> {
        get() { /* compiled code */ }
    }

    internal static func mix(l1: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l2: std.core.Option<std.unittest.prop_test.LazyNode<T>>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>> { /* compiled code */ }

    internal static func mix(l1: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l2: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l3: std.core.Option<std.unittest.prop_test.LazyNode<T>>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>> { /* compiled code */ }

    internal static func mix(l1: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l2: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l3: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l4: std.core.Option<std.unittest.prop_test.LazyNode<T>>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>> { /* compiled code */ }

    internal static func mix(l1: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l2: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l3: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l4: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l5: std.core.Option<std.unittest.prop_test.LazyNode<T>>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>> { /* compiled code */ }

    public static func ofIterator(iterable: std.core.Iterator<T>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>> { /* compiled code */ }
}

public class LazySeq<T> <: std.core.Object & std.core.Iterable<T> {
    public static func of(iterable: std.core.Iterable<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    public static func of(array: std.core.Array<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    init(element: T) { /* compiled code */ }

    init() { /* compiled code */ }

    public func iterator(): std.core.Iterator<T> { /* compiled code */ }

    public func concat(other: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    public func prepend(element: T): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    public func append(element: T): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    public func mixWith(other: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    public static func mix(l1: std.unittest.prop_test.LazySeq<T>, l2: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    public static func mix(l1: std.unittest.prop_test.LazySeq<T>, l2: std.unittest.prop_test.LazySeq<T>, l3: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    public static func mix(l1: std.unittest.prop_test.LazySeq<T>, l2: std.unittest.prop_test.LazySeq<T>, l3: std.unittest.prop_test.LazySeq<T>, l4: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    public static func mix(l1: std.unittest.prop_test.LazySeq<T>, l2: std.unittest.prop_test.LazySeq<T>, l3: std.unittest.prop_test.LazySeq<T>, l4: std.unittest.prop_test.LazySeq<T>, l5: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    public func map<U>(body: (T) -> U): std.unittest.prop_test.LazySeq<U> { /* compiled code */ }

    private var root: std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>>
}

extend std.random.Random <: RandomSource {
    public func suggestBool(): Bool { /* compiled code */ }

    public func suggestInt8(): Int8 { /* compiled code */ }

    public func suggestInt16(): Int16 { /* compiled code */ }

    public func suggestInt32(): Int32 { /* compiled code */ }

    public func suggestInt64(): Int64 { /* compiled code */ }

    public func suggestIntNative(): IntNative { /* compiled code */ }

    public func suggestUInt8(): UInt8 { /* compiled code */ }

    public func suggestUInt16(): UInt16 { /* compiled code */ }

    public func suggestUInt32(): UInt32 { /* compiled code */ }

    public func suggestUInt64(): UInt64 { /* compiled code */ }

    public func suggestUIntNative(): UIntNative { /* compiled code */ }

    public func suggestFloat16(): Float16 { /* compiled code */ }

    public func suggestFloat32(): Float32 { /* compiled code */ }

    public func suggestFloat64(): Float64 { /* compiled code */ }

    public func suggestRune(): Rune { /* compiled code */ }

    public func nextIntNative(): IntNative { /* compiled code */ }

    public func nextUIntNative(): UIntNative { /* compiled code */ }

    public func suggestInt64(l: Int64, r: Int64): Int64 { /* compiled code */ }

    public func suggestUInt64(l: UInt64, r: UInt64): UInt64 { /* compiled code */ }

    public func suggestInt32(l: Int32, r: Int32): Int32 { /* compiled code */ }

    public func suggestUInt32(l: UInt32, r: UInt32): UInt32 { /* compiled code */ }

    public func suggestInt16(l: Int16, r: Int16): Int16 { /* compiled code */ }

    public func suggestUInt16(l: UInt16, r: UInt16): UInt16 { /* compiled code */ }

    public func suggestInt8(l: Int8, r: Int8): Int8 { /* compiled code */ }

    public func suggestUInt8(l: UInt8, r: UInt8): UInt8 { /* compiled code */ }

    public func suggestIntNative(l: IntNative, r: IntNative): IntNative { /* compiled code */ }

    public func suggestUIntNative(l: UIntNative, r: UIntNative): UIntNative { /* compiled code */ }

    public func suggestFloat64(l: Float64, r: Float64): Float64 { /* compiled code */ }

    public func suggestFloat32(l: Float32, r: Float32): Float32 { /* compiled code */ }

    public func suggestFloat16(l: Float16, r: Float16): Float16 { /* compiled code */ }
}

public struct KeyRandom <: std.unittest.common.KeyFor<std.unittest.prop_test.RandomSource> {
    public static prop random: std.unittest.prop_test.KeyRandom {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public class RandomDataProvider<T> <: std.core.Object & std.unittest.common.DataProvider<T> {
    public override func provide(): std.core.Iterable<T> { /* compiled code */ }

    private let random: std.unittest.prop_test.RandomSource

    private let configuration: std.unittest.common.Configuration

    RandomDataProvider(configuration: std.unittest.common.Configuration) { /* compiled code */ }
}

public class RandomDataShrinker<T> <: std.core.Object & std.unittest.common.DataShrinker<T> {
    public override func shrink(value: T): std.core.Iterable<T> { /* compiled code */ }

    init() { /* compiled code */ }
}

public class RandomDataStrategy<T> <: std.core.Object & std.unittest.common.DataStrategy<T> {
    public override func provider(configuration: std.unittest.common.Configuration): std.unittest.prop_test.RandomDataProvider<T> { /* compiled code */ }

    public override func shrinker(_: std.unittest.common.Configuration): std.unittest.prop_test.RandomDataShrinker<T> { /* compiled code */ }

    public override prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public func random<T>(): std.unittest.prop_test.RandomDataStrategy<T> { /* compiled code */ }

public class RandomDataProviderRange<T> <: std.core.Object & std.unittest.common.DataProvider<T> {
    public override func provide(): std.core.Iterable<T> { /* compiled code */ }

    private let random: std.unittest.prop_test.RandomSource

    private let configuration: std.unittest.common.Configuration

    private let min: T

    private let max: T

    RandomDataProviderRange(configuration: std.unittest.common.Configuration, min: T, max: T) { /* compiled code */ }
}

public class RandomDataShrinkerRange<T> <: std.core.Object & std.unittest.common.DataShrinker<T> {
    public override func shrink(value: T): std.core.Iterable<T> { /* compiled code */ }

    private let min: T

    private let max: T
}

public class RandomDataStrategyRange<T> <: std.core.Object & std.unittest.common.DataStrategy<T> {
    public override func provider(configuration: std.unittest.common.Configuration): std.unittest.prop_test.RandomDataProviderRange<T> { /* compiled code */ }

    public override func shrinker(_: std.unittest.common.Configuration): std.unittest.prop_test.RandomDataShrinkerRange<T> { /* compiled code */ }

    public override prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    private let min: T

    private let max: T
}

public func randomInRange<T>(min!: std.core.Option<T> = COMPILED_CODE /* compiled code */, max!: std.core.Option<T> = COMPILED_CODE /* compiled code */): std.unittest.prop_test.RandomDataStrategyRange<T> { /* compiled code */ }

public interface Shrink<T> {
    public open func shrink(): std.core.Iterable<T> { /* compiled code */ }
}

extend Unit <: Shrink {
}

extend Bool <: Shrink {
}

extend Int8 <: Shrink {
    public func shrink(): std.core.Iterable<Int8> { /* compiled code */ }
}

extend Int16 <: Shrink {
    public func shrink(): std.core.Iterable<Int16> { /* compiled code */ }
}

extend Int32 <: Shrink {
    public func shrink(): std.core.Iterable<Int32> { /* compiled code */ }
}

extend Int64 <: Shrink {
    public func shrink(): std.core.Iterable<Int64> { /* compiled code */ }
}

extend IntNative <: Shrink {
    public func shrink(): std.core.Iterable<IntNative> { /* compiled code */ }
}

extend UInt8 <: Shrink {
    public func shrink(): std.core.Iterable<UInt8> { /* compiled code */ }
}

extend UInt16 <: Shrink {
    public func shrink(): std.core.Iterable<UInt16> { /* compiled code */ }
}

extend UInt32 <: Shrink {
    public func shrink(): std.core.Iterable<UInt32> { /* compiled code */ }
}

extend UInt64 <: Shrink {
    public func shrink(): std.core.Iterable<UInt64> { /* compiled code */ }
}

extend UIntNative <: Shrink {
    public func shrink(): std.core.Iterable<UIntNative> { /* compiled code */ }
}

extend Float16 <: Shrink {
    public func shrink(): std.core.Iterable<Float16> { /* compiled code */ }
}

extend Float32 <: Shrink {
    public func shrink(): std.core.Iterable<Float32> { /* compiled code */ }
}

extend Float64 <: Shrink {
    public func shrink(): std.core.Iterable<Float64> { /* compiled code */ }
}

extend Rune <: Shrink {
    public func shrink(): std.core.Iterable<Rune> { /* compiled code */ }
}

extend std.core.Array<T> <: Shrink {
    public func shrink(): std.core.Iterable<std.core.Array<T>> { /* compiled code */ }
}

extend std.collection.ArrayList<T> <: Shrink {
    public func shrink(): std.core.Iterable<std.collection.ArrayList<T>> { /* compiled code */ }
}

extend std.collection.HashSet<T> <: Shrink {
    public func shrink(): std.core.Iterable<std.collection.HashSet<T>> { /* compiled code */ }
}

extend std.collection.HashMap<K, V> <: Shrink {
    public func shrink(): std.core.Iterable<std.collection.HashMap<K, V>> { /* compiled code */ }
}

extend std.core.String <: Shrink {
    public func shrink(): std.core.Iterable<std.core.String> { /* compiled code */ }
}

extend std.core.Option<T> <: Shrink {
    public func shrink(): std.core.Iterable<std.core.Option<T>> { /* compiled code */ }
}

extend std.unittest.prop_test.Function0Wrapper<R> <: Shrink {
}

extend std.unittest.prop_test.TupleWrapper2<T0, T1> <: Shrink {
    public func shrink(): std.core.Iterable<std.unittest.prop_test.TupleWrapper2<T0, T1>> { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper3<T0, T1, T2> <: Shrink {
    public func shrink(): std.core.Iterable<std.unittest.prop_test.TupleWrapper3<T0, T1, T2>> { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3> <: Shrink {
    public func shrink(): std.core.Iterable<std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3>> { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4> <: Shrink {
    public func shrink(): std.core.Iterable<std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4>> { /* compiled code */ }
}

public class ShrinkHelpers {
    public static func shrinkTuple<T0, T1>(tuple: (T0, T1), t0: std.core.Iterable<T0>, t1: std.core.Iterable<T1>): std.core.Iterable<(T0, T1)> { /* compiled code */ }

    public static func shrinkTuple<T0, T1, T2>(tuple: (T0, T1, T2), t0: std.core.Iterable<T0>, t1: std.core.Iterable<T1>, t2: std.core.Iterable<T2>): std.core.Iterable<(T0, T1, T2)> { /* compiled code */ }

    public static func shrinkTuple<T0, T1, T2, T3>(tuple: (T0, T1, T2, T3), t0: std.core.Iterable<T0>, t1: std.core.Iterable<T1>, t2: std.core.Iterable<T2>, t3: std.core.Iterable<T3>): std.core.Iterable<(T0, T1, T2, T3)> { /* compiled code */ }

    public static func shrinkTuple<T0, T1, T2, T3, T4>(tuple: (T0, T1, T2, T3, T4), t0: std.core.Iterable<T0>, t1: std.core.Iterable<T1>, t2: std.core.Iterable<T2>, t3: std.core.Iterable<T3>, t4: std.core.Iterable<T4>): std.core.Iterable<(T0, T1, T2, T3, T4)> { /* compiled code */ }

    public static func mix<T>(iterables: std.core.Array<std.core.Iterable<T>>): std.core.Iterable<T> { /* compiled code */ }

    init() { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper2<T0, T1> <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper3<T0, T1, T2> <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3> <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4> <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper2<T0, T1> <: Equatable {
    public operator func ==(other: std.unittest.prop_test.TupleWrapper2<T0, T1>): Bool { /* compiled code */ }

    public operator func !=(other: std.unittest.prop_test.TupleWrapper2<T0, T1>): Bool { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper3<T0, T1, T2> <: Equatable {
    public operator func ==(other: std.unittest.prop_test.TupleWrapper3<T0, T1, T2>): Bool { /* compiled code */ }

    public operator func !=(other: std.unittest.prop_test.TupleWrapper3<T0, T1, T2>): Bool { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3> <: Equatable {
    public operator func ==(other: std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3>): Bool { /* compiled code */ }

    public operator func !=(other: std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3>): Bool { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4> <: Equatable {
    public operator func ==(other: std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4>): Bool { /* compiled code */ }

    public operator func !=(other: std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4>): Bool { /* compiled code */ }
}

public interface IndexAccess {
    public open func getElementAsAny(index: Int64): std.core.Option<std.core.Any>
}

extend std.unittest.prop_test.TupleWrapper2<T0, T1> <: IndexAccess {
    public func getElementAsAny(index: Int64): std.core.Option<std.core.Any> { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper3<T0, T1, T2> <: IndexAccess {
    public func getElementAsAny(index: Int64): std.core.Option<std.core.Any> { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3> <: IndexAccess {
    public func getElementAsAny(index: Int64): std.core.Option<std.core.Any> { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4> <: IndexAccess {
    public func getElementAsAny(index: Int64): std.core.Option<std.core.Any> { /* compiled code */ }
}
