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

interface RandomSource {
    open func nextBool(): Bool { /* compiled code */ }

    open func nextInt8(): Int8 { /* compiled code */ }

    open func nextInt16(): Int16 { /* compiled code */ }

    open func nextInt32(): Int32 { /* compiled code */ }

    open func nextInt64(): Int64 { /* compiled code */ }

    open func nextInt8(max: Int8): Int8 { /* compiled code */ }

    open func nextInt16(max: Int16): Int16 { /* compiled code */ }

    open func nextInt32(max: Int32): Int32 { /* compiled code */ }

    open func nextInt64(max: Int64): Int64 { /* compiled code */ }

    abstract open func nextUInt8(): UInt8

    abstract open func nextUInt16(): UInt16

    abstract open func nextUInt32(): UInt32

    abstract open func nextUInt64(): UInt64

    open func nextUInt8(max: UInt8): UInt8 { /* compiled code */ }

    open func nextUInt16(max: UInt16): UInt16 { /* compiled code */ }

    open func nextUInt32(max: UInt32): UInt32 { /* compiled code */ }

    open func nextUInt64(max: UInt64): UInt64 { /* compiled code */ }

    abstract open func nextFloat16(): Float16

    abstract open func nextFloat32(): Float32

    abstract open func nextFloat64(): Float64

    abstract open func nextGaussianFloat64(mean: Float64, sigma: Float64): Float64

    open func nextIntNative(): IntNative { /* compiled code */ }

    open func nextUIntNative(): UIntNative { /* compiled code */ }

    abstract open func suggestUInt8(): UInt8

    abstract open func suggestUInt16(): UInt16

    abstract open func suggestUInt32(): UInt32

    abstract open func suggestUInt64(): UInt64

    open func suggestUIntNative(): UIntNative { /* compiled code */ }

    abstract open func suggestInt8(): Int8

    abstract open func suggestInt16(): Int16

    abstract open func suggestInt32(): Int32

    abstract open func suggestInt64(): Int64

    open func suggestIntNative(): IntNative { /* compiled code */ }

    abstract open func suggestFloat16(): Float16

    abstract open func suggestFloat32(): Float32

    abstract open func suggestFloat64(): Float64

    open func suggestBool(): Bool { /* compiled code */ }

    open func suggestRune(): Rune { /* compiled code */ }

    open func suggestInt64(l: Int64, r: Int64): Int64 { /* compiled code */ }

    open func suggestUInt64(l: UInt64, r: UInt64): UInt64 { /* compiled code */ }

    open func suggestInt32(l: Int32, r: Int32): Int32 { /* compiled code */ }

    open func suggestUInt32(l: UInt32, r: UInt32): UInt32 { /* compiled code */ }

    open func suggestInt16(l: Int16, r: Int16): Int16 { /* compiled code */ }

    open func suggestUInt16(l: UInt16, r: UInt16): UInt16 { /* compiled code */ }

    open func suggestInt8(l: Int8, r: Int8): Int8 { /* compiled code */ }

    open func suggestUInt8(l: UInt8, r: UInt8): UInt8 { /* compiled code */ }

    open func suggestIntNative(l: IntNative, r: IntNative): IntNative { /* compiled code */ }

    open func suggestUIntNative(l: UIntNative, r: UIntNative): UIntNative { /* compiled code */ }

    open func suggestFloat64(l: Float64, r: Float64): Float64 { /* compiled code */ }

    open func suggestFloat32(l: Float32, r: Float32): Float32 { /* compiled code */ }

    open func suggestFloat16(l: Float16, r: Float16): Float16 { /* compiled code */ }
}

func randomSuggestedRune(random: std.unittest.prop_test.RandomSource): Rune { /* compiled code */ }

func remap(l: Int64, r: Int64, x: Int64): Int64 { /* compiled code */ }

func remap(l: UInt64, r: UInt64, x: UInt64): UInt64 { /* compiled code */ }

func remap(l: Int32, r: Int32, x: Int32): Int32 { /* compiled code */ }

func remap(l: UInt32, r: UInt32, x: UInt32): UInt32 { /* compiled code */ }

func remap(l: Int16, r: Int16, x: Int16): Int16 { /* compiled code */ }

func remap(l: UInt16, r: UInt16, x: UInt16): UInt16 { /* compiled code */ }

func remap(l: Int8, r: Int8, x: Int8): Int8 { /* compiled code */ }

func remap(l: UInt8, r: UInt8, x: UInt8): UInt8 { /* compiled code */ }

func remap(l: IntNative, r: IntNative, x: IntNative): IntNative { /* compiled code */ }

func remap(l: UIntNative, r: UIntNative, x: UIntNative): UIntNative { /* compiled code */ }

func remap(l: Float64, r: Float64, x: Float64): Float64 { /* compiled code */ }

func remap(l: Float32, r: Float32, x: Float32): Float32 { /* compiled code */ }

func remap(l: Float16, r: Float16, x: Float16): Float16 { /* compiled code */ }

func emptyIterable<T>(): std.core.Iterable<T> { /* compiled code */ }

interface Arbitrary<T> {
    abstract static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<T>
}

interface Generator<T> {
    abstract open func next(): T
}

extend Unit <: Arbitrary {
    static func arbitrary(_: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Unit> { /* compiled code */ }
}

extend Bool <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Bool> { /* compiled code */ }
}

extend Int8 <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Int8> { /* compiled code */ }
}

extend Int16 <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Int16> { /* compiled code */ }
}

extend Int32 <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Int32> { /* compiled code */ }
}

extend Int64 <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Int64> { /* compiled code */ }
}

extend IntNative <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<IntNative> { /* compiled code */ }
}

extend UInt8 <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UInt8> { /* compiled code */ }
}

extend UInt16 <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UInt16> { /* compiled code */ }
}

extend UInt32 <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UInt32> { /* compiled code */ }
}

extend UInt64 <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UInt64> { /* compiled code */ }
}

extend UIntNative <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UIntNative> { /* compiled code */ }
}

extend Float16 <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Float16> { /* compiled code */ }
}

extend Float32 <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Float32> { /* compiled code */ }
}

extend Float64 <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Float64> { /* compiled code */ }
}

extend Rune <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Rune> { /* compiled code */ }
}

extend std.core.Array<T> <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.core.Array<T>> { /* compiled code */ }
}

extend std.collection.ArrayList<T> <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.collection.ArrayList<T>> { /* compiled code */ }
}

extend std.collection.HashSet<T> <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.collection.HashSet<T>> { /* compiled code */ }
}

extend std.collection.HashMap<K, V> <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.collection.HashMap<K, V>> { /* compiled code */ }
}

extend std.core.String <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.core.String> { /* compiled code */ }
}

extend std.core.Option<T> <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.core.Option<T>> { /* compiled code */ }
}

extend std.core.Ordering <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.core.Ordering> { /* compiled code */ }
}

extend std.unittest.prop_test.Function0Wrapper<R> <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.Function0Wrapper<R>> { /* compiled code */ }
}

struct Function0Wrapper<R> {
    operator func ()(): R { /* compiled code */ }

    let function: () -> R

    Function0Wrapper(function: () -> R) { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper2<T0, T1> <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.TupleWrapper2<T0, T1>> { /* compiled code */ }
}

struct TupleWrapper2<T0, T1> {
    func apply<R>(f: (T0, T1) -> R): R { /* compiled code */ }

    let tuple: (T0, T1)

    TupleWrapper2(tuple: (T0, T1)) { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper3<T0, T1, T2> <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.TupleWrapper3<T0, T1, T2>> { /* compiled code */ }
}

struct TupleWrapper3<T0, T1, T2> {
    func apply<R>(f: (T0, T1, T2) -> R): R { /* compiled code */ }

    let tuple: (T0, T1, T2)

    TupleWrapper3(tuple: (T0, T1, T2)) { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3> <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3>> { /* compiled code */ }
}

struct TupleWrapper4<T0, T1, T2, T3> {
    func apply<R>(f: (T0, T1, T2, T3) -> R): R { /* compiled code */ }

    let tuple: (T0, T1, T2, T3)

    TupleWrapper4(tuple: (T0, T1, T2, T3)) { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4> <: Arbitrary {
    static func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4>> { /* compiled code */ }
}

struct TupleWrapper5<T0, T1, T2, T3, T4> {
    func apply<R>(f: (T0, T1, T2, T3, T4) -> R): R { /* compiled code */ }

    let tuple: (T0, T1, T2, T3, T4)

    TupleWrapper5(tuple: (T0, T1, T2, T3, T4)) { /* compiled code */ }
}

interface ArbitraryRange<T> {
    abstract static func min(): T

    abstract static func max(): T

    abstract static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: T, max: T): std.unittest.prop_test.Generator<T>
}

extend Int64 <: ArbitraryRange {
    static func min(): Int64 { /* compiled code */ }

    static func max(): Int64 { /* compiled code */ }

    static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Int64, max: Int64): std.unittest.prop_test.Generator<Int64> { /* compiled code */ }
}

extend UInt64 <: ArbitraryRange {
    static func min(): UInt64 { /* compiled code */ }

    static func max(): UInt64 { /* compiled code */ }

    static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UInt64, max: UInt64): std.unittest.prop_test.Generator<UInt64> { /* compiled code */ }
}

extend Int32 <: ArbitraryRange {
    static func min(): Int32 { /* compiled code */ }

    static func max(): Int32 { /* compiled code */ }

    static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Int32, max: Int32): std.unittest.prop_test.Generator<Int32> { /* compiled code */ }
}

extend UInt32 <: ArbitraryRange {
    static func min(): UInt32 { /* compiled code */ }

    static func max(): UInt32 { /* compiled code */ }

    static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UInt32, max: UInt32): std.unittest.prop_test.Generator<UInt32> { /* compiled code */ }
}

extend Int16 <: ArbitraryRange {
    static func min(): Int16 { /* compiled code */ }

    static func max(): Int16 { /* compiled code */ }

    static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Int16, max: Int16): std.unittest.prop_test.Generator<Int16> { /* compiled code */ }
}

extend UInt16 <: ArbitraryRange {
    static func min(): UInt16 { /* compiled code */ }

    static func max(): UInt16 { /* compiled code */ }

    static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UInt16, max: UInt16): std.unittest.prop_test.Generator<UInt16> { /* compiled code */ }
}

extend Int8 <: ArbitraryRange {
    static func min(): Int8 { /* compiled code */ }

    static func max(): Int8 { /* compiled code */ }

    static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Int8, max: Int8): std.unittest.prop_test.Generator<Int8> { /* compiled code */ }
}

extend UInt8 <: ArbitraryRange {
    static func min(): UInt8 { /* compiled code */ }

    static func max(): UInt8 { /* compiled code */ }

    static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UInt8, max: UInt8): std.unittest.prop_test.Generator<UInt8> { /* compiled code */ }
}

extend IntNative <: ArbitraryRange {
    static func min(): IntNative { /* compiled code */ }

    static func max(): IntNative { /* compiled code */ }

    static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: IntNative, max: IntNative): std.unittest.prop_test.Generator<IntNative> { /* compiled code */ }
}

extend UIntNative <: ArbitraryRange {
    static func min(): UIntNative { /* compiled code */ }

    static func max(): UIntNative { /* compiled code */ }

    static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UIntNative, max: UIntNative): std.unittest.prop_test.Generator<UIntNative> { /* compiled code */ }
}

extend Float64 <: ArbitraryRange {
    static func min(): Float64 { /* compiled code */ }

    static func max(): Float64 { /* compiled code */ }

    static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Float64, max: Float64): std.unittest.prop_test.Generator<Float64> { /* compiled code */ }
}

extend Float32 <: ArbitraryRange {
    static func min(): Float32 { /* compiled code */ }

    static func max(): Float32 { /* compiled code */ }

    static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Float32, max: Float32): std.unittest.prop_test.Generator<Float32> { /* compiled code */ }
}

extend Float16 <: ArbitraryRange {
    static func min(): Float16 { /* compiled code */ }

    static func max(): Float16 { /* compiled code */ }

    static func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Float16, max: Float16): std.unittest.prop_test.Generator<Float16> { /* compiled code */ }
}

class Generators {
    static func generate<T>(l: T, r: T, body: (T, T) -> T): std.unittest.prop_test.Generator<T> { /* compiled code */ }

    static func single<T>(value: T): std.unittest.prop_test.Generator<T> { /* compiled code */ }

    static func generate<T>(body: () -> T): std.unittest.prop_test.Generator<T> { /* compiled code */ }

    static func iterable<T>(random: std.unittest.prop_test.RandomSource, collection: std.core.Array<T>): std.unittest.prop_test.Generator<T> { /* compiled code */ }

    static func weighted<T>(random: std.unittest.prop_test.RandomSource, variants: std.core.Array<(UInt64, std.unittest.prop_test.Generator<T>)>): std.unittest.prop_test.Generator<T> { /* compiled code */ }

    static func pick<T>(random: std.unittest.prop_test.RandomSource, variants: std.core.Array<std.unittest.prop_test.Generator<T>>): std.unittest.prop_test.Generator<T> { /* compiled code */ }

    static func lookup<T>(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<T> { /* compiled code */ }

    static func mapped<T, R>(random: std.unittest.prop_test.RandomSource, body: (T) -> R): std.unittest.prop_test.Generator<R> { /* compiled code */ }

    static func mapped<T1, T2, R>(random: std.unittest.prop_test.RandomSource, body: (T1, T2) -> R): std.unittest.prop_test.Generator<R> { /* compiled code */ }

    static func mapped<T1, T2, T3, R>(random: std.unittest.prop_test.RandomSource, body: (T1, T2, T3) -> R): std.unittest.prop_test.Generator<R> { /* compiled code */ }

    static func mapped<T1, T2, T3, T4, R>(random: std.unittest.prop_test.RandomSource, body: (T1, T2, T3, T4) -> R): std.unittest.prop_test.Generator<R> { /* compiled code */ }

    init() { /* compiled code */ }
}

@Annotation
enum Thunk<T> {
    | Uncalculated(() -> std.core.Option<T>)

    | Calculated(T)

    | EmptyThunk
}

struct Lazy<T> {
    var thunk: std.unittest.prop_test.Thunk<T>

    mut func get(): std.core.Option<T> { /* compiled code */ }

    static func flat<T>(body: () -> std.unittest.prop_test.Lazy<T>): std.unittest.prop_test.Lazy<T> { /* compiled code */ }
}

class LazyNode<T> {
    let head: T

    var lazyTail: std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>>

    @Annotation
    prop tail: std.core.Option<std.unittest.prop_test.LazyNode<T>> {
        get() { /* compiled code */ }
    }

    static func mix(l1: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l2: std.core.Option<std.unittest.prop_test.LazyNode<T>>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>> { /* compiled code */ }

    static func mix(l1: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l2: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l3: std.core.Option<std.unittest.prop_test.LazyNode<T>>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>> { /* compiled code */ }

    static func mix(l1: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l2: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l3: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l4: std.core.Option<std.unittest.prop_test.LazyNode<T>>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>> { /* compiled code */ }

    static func mix(l1: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l2: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l3: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l4: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l5: std.core.Option<std.unittest.prop_test.LazyNode<T>>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>> { /* compiled code */ }

    static func ofIterator(iterable: std.core.Iterator<T>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>> { /* compiled code */ }
}

class LazySeq<T> <: std.core.Object & std.core.Iterable<T> {
    static func of(iterable: std.core.Iterable<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    static func of(array: std.core.Array<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    init(element: T) { /* compiled code */ }

    init() { /* compiled code */ }

    func iterator(): std.core.Iterator<T> { /* compiled code */ }

    func concat(other: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    func prepend(element: T): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    func append(element: T): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    func mixWith(other: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    static func mix(l1: std.unittest.prop_test.LazySeq<T>, l2: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    static func mix(l1: std.unittest.prop_test.LazySeq<T>, l2: std.unittest.prop_test.LazySeq<T>, l3: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    static func mix(l1: std.unittest.prop_test.LazySeq<T>, l2: std.unittest.prop_test.LazySeq<T>, l3: std.unittest.prop_test.LazySeq<T>, l4: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    static func mix(l1: std.unittest.prop_test.LazySeq<T>, l2: std.unittest.prop_test.LazySeq<T>, l3: std.unittest.prop_test.LazySeq<T>, l4: std.unittest.prop_test.LazySeq<T>, l5: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T> { /* compiled code */ }

    func map<U>(body: (T) -> U): std.unittest.prop_test.LazySeq<U> { /* compiled code */ }

    var root: std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>>
}

extend std.random.Random <: RandomSource {
    func suggestBool(): Bool { /* compiled code */ }

    func suggestInt8(): Int8 { /* compiled code */ }

    func suggestInt16(): Int16 { /* compiled code */ }

    func suggestInt32(): Int32 { /* compiled code */ }

    func suggestInt64(): Int64 { /* compiled code */ }

    func suggestIntNative(): IntNative { /* compiled code */ }

    func suggestUInt8(): UInt8 { /* compiled code */ }

    func suggestUInt16(): UInt16 { /* compiled code */ }

    func suggestUInt32(): UInt32 { /* compiled code */ }

    func suggestUInt64(): UInt64 { /* compiled code */ }

    func suggestUIntNative(): UIntNative { /* compiled code */ }

    func suggestFloat16(): Float16 { /* compiled code */ }

    func suggestFloat32(): Float32 { /* compiled code */ }

    func suggestFloat64(): Float64 { /* compiled code */ }

    func suggestRune(): Rune { /* compiled code */ }

    func nextIntNative(): IntNative { /* compiled code */ }

    func nextUIntNative(): UIntNative { /* compiled code */ }

    func suggestInt64(l: Int64, r: Int64): Int64 { /* compiled code */ }

    func suggestUInt64(l: UInt64, r: UInt64): UInt64 { /* compiled code */ }

    func suggestInt32(l: Int32, r: Int32): Int32 { /* compiled code */ }

    func suggestUInt32(l: UInt32, r: UInt32): UInt32 { /* compiled code */ }

    func suggestInt16(l: Int16, r: Int16): Int16 { /* compiled code */ }

    func suggestUInt16(l: UInt16, r: UInt16): UInt16 { /* compiled code */ }

    func suggestInt8(l: Int8, r: Int8): Int8 { /* compiled code */ }

    func suggestUInt8(l: UInt8, r: UInt8): UInt8 { /* compiled code */ }

    func suggestIntNative(l: IntNative, r: IntNative): IntNative { /* compiled code */ }

    func suggestUIntNative(l: UIntNative, r: UIntNative): UIntNative { /* compiled code */ }

    func suggestFloat64(l: Float64, r: Float64): Float64 { /* compiled code */ }

    func suggestFloat32(l: Float32, r: Float32): Float32 { /* compiled code */ }

    func suggestFloat16(l: Float16, r: Float16): Float16 { /* compiled code */ }
}

struct KeyRandom <: std.unittest.common.KeyFor<std.unittest.prop_test.RandomSource> {
    @Annotation
    static prop random: std.unittest.prop_test.KeyRandom {
        get() { /* compiled code */ }
    }

    @Annotation
    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

class RandomDataProvider<T> <: std.core.Object & std.unittest.common.DataProvider<T> {
    override func provide(): std.core.Iterable<T> { /* compiled code */ }

    let random: std.unittest.prop_test.RandomSource

    let configuration: std.unittest.common.Configuration

    RandomDataProvider(configuration: std.unittest.common.Configuration) { /* compiled code */ }
}

class RandomDataShrinker<T> <: std.core.Object & std.unittest.common.DataShrinker<T> {
    override func shrink(value: T): std.core.Iterable<T> { /* compiled code */ }

    init() { /* compiled code */ }
}

class RandomDataStrategy<T> <: std.core.Object & std.unittest.common.DataStrategy<T> {
    override func provider(configuration: std.unittest.common.Configuration): std.unittest.prop_test.RandomDataProvider<T> { /* compiled code */ }

    override func shrinker(_: std.unittest.common.Configuration): std.unittest.prop_test.RandomDataShrinker<T> { /* compiled code */ }

    @Annotation
    override prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

func random<T>(): std.unittest.prop_test.RandomDataStrategy<T> { /* compiled code */ }

class RandomDataProviderRange<T> <: std.core.Object & std.unittest.common.DataProvider<T> {
    override func provide(): std.core.Iterable<T> { /* compiled code */ }

    let random: std.unittest.prop_test.RandomSource

    let configuration: std.unittest.common.Configuration

    let min: T

    let max: T

    RandomDataProviderRange(configuration: std.unittest.common.Configuration, min: T, max: T) { /* compiled code */ }
}

class RandomDataShrinkerRange<T> <: std.core.Object & std.unittest.common.DataShrinker<T> {
    override func shrink(value: T): std.core.Iterable<T> { /* compiled code */ }

    let min: T

    let max: T
}

class RandomDataStrategyRange<T> <: std.core.Object & std.unittest.common.DataStrategy<T> {
    override func provider(configuration: std.unittest.common.Configuration): std.unittest.prop_test.RandomDataProviderRange<T> { /* compiled code */ }

    override func shrinker(_: std.unittest.common.Configuration): std.unittest.prop_test.RandomDataShrinkerRange<T> { /* compiled code */ }

    @Annotation
    override prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    let min: T

    let max: T
}

func randomInRange<T>(min: std.core.Option<T>, max: std.core.Option<T>): std.unittest.prop_test.RandomDataStrategyRange<T> { /* compiled code */ }

interface Shrink<T> {
    open func shrink(): std.core.Iterable<T> { /* compiled code */ }
}

extend Unit <: Shrink {
}

extend Bool <: Shrink {
}

extend Int8 <: Shrink {
    func shrink(): std.core.Iterable<Int8> { /* compiled code */ }
}

extend Int16 <: Shrink {
    func shrink(): std.core.Iterable<Int16> { /* compiled code */ }
}

extend Int32 <: Shrink {
    func shrink(): std.core.Iterable<Int32> { /* compiled code */ }
}

extend Int64 <: Shrink {
    func shrink(): std.core.Iterable<Int64> { /* compiled code */ }
}

extend IntNative <: Shrink {
    func shrink(): std.core.Iterable<IntNative> { /* compiled code */ }
}

extend UInt8 <: Shrink {
    func shrink(): std.core.Iterable<UInt8> { /* compiled code */ }
}

extend UInt16 <: Shrink {
    func shrink(): std.core.Iterable<UInt16> { /* compiled code */ }
}

extend UInt32 <: Shrink {
    func shrink(): std.core.Iterable<UInt32> { /* compiled code */ }
}

extend UInt64 <: Shrink {
    func shrink(): std.core.Iterable<UInt64> { /* compiled code */ }
}

extend UIntNative <: Shrink {
    func shrink(): std.core.Iterable<UIntNative> { /* compiled code */ }
}

extend Float16 <: Shrink {
    func shrink(): std.core.Iterable<Float16> { /* compiled code */ }
}

extend Float32 <: Shrink {
    func shrink(): std.core.Iterable<Float32> { /* compiled code */ }
}

extend Float64 <: Shrink {
    func shrink(): std.core.Iterable<Float64> { /* compiled code */ }
}

extend Rune <: Shrink {
    func shrink(): std.core.Iterable<Rune> { /* compiled code */ }
}

extend std.core.Array<T> <: Shrink {
    func shrink(): std.core.Iterable<std.core.Array<T>> { /* compiled code */ }
}

extend std.collection.ArrayList<T> <: Shrink {
    func shrink(): std.core.Iterable<std.collection.ArrayList<T>> { /* compiled code */ }
}

extend std.collection.HashSet<T> <: Shrink {
    func shrink(): std.core.Iterable<std.collection.HashSet<T>> { /* compiled code */ }
}

extend std.collection.HashMap<K, V> <: Shrink {
    func shrink(): std.core.Iterable<std.collection.HashMap<K, V>> { /* compiled code */ }
}

extend std.core.String <: Shrink {
    func shrink(): std.core.Iterable<std.core.String> { /* compiled code */ }
}

extend std.core.Option<T> <: Shrink {
    func shrink(): std.core.Iterable<std.core.Option<T>> { /* compiled code */ }
}

extend std.unittest.prop_test.Function0Wrapper<R> <: Shrink {
}

extend std.unittest.prop_test.TupleWrapper2<T0, T1> <: Shrink {
    func shrink(): std.core.Iterable<std.unittest.prop_test.TupleWrapper2<T0, T1>> { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper3<T0, T1, T2> <: Shrink {
    func shrink(): std.core.Iterable<std.unittest.prop_test.TupleWrapper3<T0, T1, T2>> { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3> <: Shrink {
    func shrink(): std.core.Iterable<std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3>> { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4> <: Shrink {
    func shrink(): std.core.Iterable<std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4>> { /* compiled code */ }
}

class ShrinkHelpers {
    static func shrinkTuple<T0, T1>(tuple: (T0, T1), t0: std.core.Iterable<T0>, t1: std.core.Iterable<T1>): std.core.Iterable<(T0, T1)> { /* compiled code */ }

    static func shrinkTuple<T0, T1, T2>(tuple: (T0, T1, T2), t0: std.core.Iterable<T0>, t1: std.core.Iterable<T1>, t2: std.core.Iterable<T2>): std.core.Iterable<(T0, T1, T2)> { /* compiled code */ }

    static func shrinkTuple<T0, T1, T2, T3>(tuple: (T0, T1, T2, T3), t0: std.core.Iterable<T0>, t1: std.core.Iterable<T1>, t2: std.core.Iterable<T2>, t3: std.core.Iterable<T3>): std.core.Iterable<(T0, T1, T2, T3)> { /* compiled code */ }

    static func shrinkTuple<T0, T1, T2, T3, T4>(tuple: (T0, T1, T2, T3, T4), t0: std.core.Iterable<T0>, t1: std.core.Iterable<T1>, t2: std.core.Iterable<T2>, t3: std.core.Iterable<T3>, t4: std.core.Iterable<T4>): std.core.Iterable<(T0, T1, T2, T3, T4)> { /* compiled code */ }

    static func mix<T>(iterables: std.core.Array<std.core.Iterable<T>>): std.core.Iterable<T> { /* compiled code */ }

    init() { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper2<T0, T1> <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper3<T0, T1, T2> <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3> <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4> <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper2<T0, T1> <: Equatable {
    operator func ==(other: std.unittest.prop_test.TupleWrapper2<T0, T1>): Bool { /* compiled code */ }

    operator func !=(other: std.unittest.prop_test.TupleWrapper2<T0, T1>): Bool { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper3<T0, T1, T2> <: Equatable {
    operator func ==(other: std.unittest.prop_test.TupleWrapper3<T0, T1, T2>): Bool { /* compiled code */ }

    operator func !=(other: std.unittest.prop_test.TupleWrapper3<T0, T1, T2>): Bool { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3> <: Equatable {
    operator func ==(other: std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3>): Bool { /* compiled code */ }

    operator func !=(other: std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3>): Bool { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4> <: Equatable {
    operator func ==(other: std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4>): Bool { /* compiled code */ }

    operator func !=(other: std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4>): Bool { /* compiled code */ }
}

interface IndexAccess {
    abstract open func getElementAsAny(index: Int64): std.core.Option<std.core.Any>
}

extend std.unittest.prop_test.TupleWrapper2<T0, T1> <: IndexAccess {
    func getElementAsAny(index: Int64): std.core.Option<std.core.Any> { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper3<T0, T1, T2> <: IndexAccess {
    func getElementAsAny(index: Int64): std.core.Option<std.core.Any> { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3> <: IndexAccess {
    func getElementAsAny(index: Int64): std.core.Option<std.core.Any> { /* compiled code */ }
}

extend std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4> <: IndexAccess {
    func getElementAsAny(index: Int64): std.core.Option<std.core.Any> { /* compiled code */ }
}
