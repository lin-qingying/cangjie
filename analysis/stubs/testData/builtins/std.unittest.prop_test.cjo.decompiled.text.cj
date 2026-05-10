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
    func nextBool: Bool

    func nextInt8: Int8

    func nextInt16: Int16

    func nextInt32: Int32

    func nextInt64: Int64

    func nextInt8(max: Int8): Int8

    func nextInt16(max: Int16): Int16

    func nextInt32(max: Int32): Int32

    func nextInt64(max: Int64): Int64

    func nextUInt8: UInt8

    func nextUInt16: UInt16

    func nextUInt32: UInt32

    func nextUInt64: UInt64

    func nextUInt8(max: UInt8): UInt8

    func nextUInt16(max: UInt16): UInt16

    func nextUInt32(max: UInt32): UInt32

    func nextUInt64(max: UInt64): UInt64

    func nextFloat16: Float16

    func nextFloat32: Float32

    func nextFloat64: Float64

    func nextGaussianFloat64(mean: Float64, sigma: Float64): Float64

    func nextIntNative: IntNative

    func nextUIntNative: UIntNative

    func suggestUInt8: UInt8

    func suggestUInt16: UInt16

    func suggestUInt32: UInt32

    func suggestUInt64: UInt64

    func suggestUIntNative: UIntNative

    func suggestInt8: Int8

    func suggestInt16: Int16

    func suggestInt32: Int32

    func suggestInt64: Int64

    func suggestIntNative: IntNative

    func suggestFloat16: Float16

    func suggestFloat32: Float32

    func suggestFloat64: Float64

    func suggestBool: Bool

    func suggestRune: Rune

    func suggestInt64(l: Int64, r: Int64): Int64

    func suggestUInt64(l: UInt64, r: UInt64): UInt64

    func suggestInt32(l: Int32, r: Int32): Int32

    func suggestUInt32(l: UInt32, r: UInt32): UInt32

    func suggestInt16(l: Int16, r: Int16): Int16

    func suggestUInt16(l: UInt16, r: UInt16): UInt16

    func suggestInt8(l: Int8, r: Int8): Int8

    func suggestUInt8(l: UInt8, r: UInt8): UInt8

    func suggestIntNative(l: IntNative, r: IntNative): IntNative

    func suggestUIntNative(l: UIntNative, r: UIntNative): UIntNative

    func suggestFloat64(l: Float64, r: Float64): Float64

    func suggestFloat32(l: Float32, r: Float32): Float32

    func suggestFloat16(l: Float16, r: Float16): Float16
}

func randomSuggestedRune(random: std.unittest.prop_test.RandomSource): Rune

func remap(l: Int64, r: Int64, x: Int64): Int64

func remap(l: UInt64, r: UInt64, x: UInt64): UInt64

func remap(l: Int32, r: Int32, x: Int32): Int32

func remap(l: UInt32, r: UInt32, x: UInt32): UInt32

func remap(l: Int16, r: Int16, x: Int16): Int16

func remap(l: UInt16, r: UInt16, x: UInt16): UInt16

func remap(l: Int8, r: Int8, x: Int8): Int8

func remap(l: UInt8, r: UInt8, x: UInt8): UInt8

func remap(l: IntNative, r: IntNative, x: IntNative): IntNative

func remap(l: UIntNative, r: UIntNative, x: UIntNative): UIntNative

func remap(l: Float64, r: Float64, x: Float64): Float64

func remap(l: Float32, r: Float32, x: Float32): Float32

func remap(l: Float16, r: Float16, x: Float16): Float16

func emptyIterable: std.core.Iterable<T>

interface Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<T>
}

interface Generator {
    func next: T
}

extend Extend <: Arbitrary {
    func arbitrary(_: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Unit>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Bool>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Int8>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Int16>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Int32>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Int64>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<IntNative>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UInt8>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UInt16>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UInt32>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UInt64>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<UIntNative>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Float16>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Float32>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Float64>
}

extend Extend <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<Rune>
}

extend Array <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.core.Array<T>>
}

extend ArrayList <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.collection.ArrayList<T>>
}

extend HashSet <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.collection.HashSet<T>>
}

extend HashMap <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.collection.HashMap<K, V>>
}

extend String <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.core.String>
}

extend Option <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.core.Option<T>>
}

extend Ordering <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.core.Ordering>
}

extend Function0Wrapper <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.Function0Wrapper<R>>
}

struct Function0Wrapper {
    operator func (): R

    let function: () -> R

    init(function: () -> R) { /* compiled code */ }
}

extend TupleWrapper2 <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.TupleWrapper2<T0, T1>>
}

struct TupleWrapper2 {
    func apply(f: (T0, T1) -> R): R

    let tuple: (T0, T1)

    init(tuple: (T0, T1)) { /* compiled code */ }
}

extend TupleWrapper3 <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.TupleWrapper3<T0, T1, T2>>
}

struct TupleWrapper3 {
    func apply(f: (T0, T1, T2) -> R): R

    let tuple: (T0, T1, T2)

    init(tuple: (T0, T1, T2)) { /* compiled code */ }
}

extend TupleWrapper4 <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3>>
}

struct TupleWrapper4 {
    func apply(f: (T0, T1, T2, T3) -> R): R

    let tuple: (T0, T1, T2, T3)

    init(tuple: (T0, T1, T2, T3)) { /* compiled code */ }
}

extend TupleWrapper5 <: Arbitrary {
    func arbitrary(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4>>
}

struct TupleWrapper5 {
    func apply(f: (T0, T1, T2, T3, T4) -> R): R

    let tuple: (T0, T1, T2, T3, T4)

    init(tuple: (T0, T1, T2, T3, T4)) { /* compiled code */ }
}

interface ArbitraryRange {
    func min: T

    func max: T

    func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: T, max: T): std.unittest.prop_test.Generator<T>
}

extend Extend <: ArbitraryRange {
    func min: Int64

    func max: Int64

    func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Int64, max: Int64): std.unittest.prop_test.Generator<Int64>
}

extend Extend <: ArbitraryRange {
    func min: UInt64

    func max: UInt64

    func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UInt64, max: UInt64): std.unittest.prop_test.Generator<UInt64>
}

extend Extend <: ArbitraryRange {
    func min: Int32

    func max: Int32

    func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Int32, max: Int32): std.unittest.prop_test.Generator<Int32>
}

extend Extend <: ArbitraryRange {
    func min: UInt32

    func max: UInt32

    func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UInt32, max: UInt32): std.unittest.prop_test.Generator<UInt32>
}

extend Extend <: ArbitraryRange {
    func min: Int16

    func max: Int16

    func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Int16, max: Int16): std.unittest.prop_test.Generator<Int16>
}

extend Extend <: ArbitraryRange {
    func min: UInt16

    func max: UInt16

    func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UInt16, max: UInt16): std.unittest.prop_test.Generator<UInt16>
}

extend Extend <: ArbitraryRange {
    func min: Int8

    func max: Int8

    func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Int8, max: Int8): std.unittest.prop_test.Generator<Int8>
}

extend Extend <: ArbitraryRange {
    func min: UInt8

    func max: UInt8

    func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UInt8, max: UInt8): std.unittest.prop_test.Generator<UInt8>
}

extend Extend <: ArbitraryRange {
    func min: IntNative

    func max: IntNative

    func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: IntNative, max: IntNative): std.unittest.prop_test.Generator<IntNative>
}

extend Extend <: ArbitraryRange {
    func min: UIntNative

    func max: UIntNative

    func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: UIntNative, max: UIntNative): std.unittest.prop_test.Generator<UIntNative>
}

extend Extend <: ArbitraryRange {
    func min: Float64

    func max: Float64

    func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Float64, max: Float64): std.unittest.prop_test.Generator<Float64>
}

extend Extend <: ArbitraryRange {
    func min: Float32

    func max: Float32

    func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Float32, max: Float32): std.unittest.prop_test.Generator<Float32>
}

extend Extend <: ArbitraryRange {
    func min: Float16

    func max: Float16

    func arbitraryRange(random: std.unittest.prop_test.RandomSource, min: Float16, max: Float16): std.unittest.prop_test.Generator<Float16>
}

class Generators {
    func generate(l: T, r: T, body: (T, T) -> T): std.unittest.prop_test.Generator<T>

    func single(value: T): std.unittest.prop_test.Generator<T>

    func generate(body: () -> T): std.unittest.prop_test.Generator<T>

    func iterable(random: std.unittest.prop_test.RandomSource, collection: std.core.Array<T>): std.unittest.prop_test.Generator<T>

    func weighted(random: std.unittest.prop_test.RandomSource, variants: std.core.Array<(UInt64, std.unittest.prop_test.Generator<T>)>): std.unittest.prop_test.Generator<T>

    func pick(random: std.unittest.prop_test.RandomSource, variants: std.core.Array<std.unittest.prop_test.Generator<T>>): std.unittest.prop_test.Generator<T>

    func lookup(random: std.unittest.prop_test.RandomSource): std.unittest.prop_test.Generator<T>

    func mapped(random: std.unittest.prop_test.RandomSource, body: (T) -> R): std.unittest.prop_test.Generator<R>

    func mapped(random: std.unittest.prop_test.RandomSource, body: (T1, T2) -> R): std.unittest.prop_test.Generator<R>

    func mapped(random: std.unittest.prop_test.RandomSource, body: (T1, T2, T3) -> R): std.unittest.prop_test.Generator<R>

    func mapped(random: std.unittest.prop_test.RandomSource, body: (T1, T2, T3, T4) -> R): std.unittest.prop_test.Generator<R>

    init() { /* compiled code */ }
}

enum Thunk {
    | Uncalculated(() -> std.core.Option<T>)

    | Calculated(T)

    | EmptyThunk
}

struct Lazy {
    var thunk: std.unittest.prop_test.Thunk<T>

    func `get`: std.core.Option<T>

    func flat(body: () -> std.unittest.prop_test.Lazy<T>): std.unittest.prop_test.Lazy<T>
}

class LazyNode {
    let head: T

    var lazyTail: std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>>

    prop tail: std.core.Option<std.unittest.prop_test.LazyNode<T>>

    func mix(l1: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l2: std.core.Option<std.unittest.prop_test.LazyNode<T>>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>>

    func mix(l1: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l2: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l3: std.core.Option<std.unittest.prop_test.LazyNode<T>>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>>

    func mix(l1: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l2: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l3: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l4: std.core.Option<std.unittest.prop_test.LazyNode<T>>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>>

    func mix(l1: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l2: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l3: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l4: std.core.Option<std.unittest.prop_test.LazyNode<T>>, l5: std.core.Option<std.unittest.prop_test.LazyNode<T>>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>>

    func ofIterator(iterable: std.core.Iterator<T>): std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>>
}

class LazySeq <: std.core.Object & std.core.Iterable<T> {
    func of(iterable: std.core.Iterable<T>): std.unittest.prop_test.LazySeq<T>

    func of(array: std.core.Array<T>): std.unittest.prop_test.LazySeq<T>

    init(element: T) { /* compiled code */ }

    init() { /* compiled code */ }

    func iterator: std.core.Iterator<T>

    func concat(other: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T>

    func prepend(element: T): std.unittest.prop_test.LazySeq<T>

    func append(element: T): std.unittest.prop_test.LazySeq<T>

    func mixWith(other: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T>

    func mix(l1: std.unittest.prop_test.LazySeq<T>, l2: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T>

    func mix(l1: std.unittest.prop_test.LazySeq<T>, l2: std.unittest.prop_test.LazySeq<T>, l3: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T>

    func mix(l1: std.unittest.prop_test.LazySeq<T>, l2: std.unittest.prop_test.LazySeq<T>, l3: std.unittest.prop_test.LazySeq<T>, l4: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T>

    func mix(l1: std.unittest.prop_test.LazySeq<T>, l2: std.unittest.prop_test.LazySeq<T>, l3: std.unittest.prop_test.LazySeq<T>, l4: std.unittest.prop_test.LazySeq<T>, l5: std.unittest.prop_test.LazySeq<T>): std.unittest.prop_test.LazySeq<T>

    func map(body: (T) -> U): std.unittest.prop_test.LazySeq<U>

    var root: std.unittest.prop_test.Lazy<std.unittest.prop_test.LazyNode<T>>
}

extend Random <: RandomSource {
    func suggestBool: Bool

    func suggestInt8: Int8

    func suggestInt16: Int16

    func suggestInt32: Int32

    func suggestInt64: Int64

    func suggestIntNative: IntNative

    func suggestUInt8: UInt8

    func suggestUInt16: UInt16

    func suggestUInt32: UInt32

    func suggestUInt64: UInt64

    func suggestUIntNative: UIntNative

    func suggestFloat16: Float16

    func suggestFloat32: Float32

    func suggestFloat64: Float64

    func suggestRune: Rune

    func nextIntNative: IntNative

    func nextUIntNative: UIntNative

    func suggestInt64(l: Int64, r: Int64): Int64

    func suggestUInt64(l: UInt64, r: UInt64): UInt64

    func suggestInt32(l: Int32, r: Int32): Int32

    func suggestUInt32(l: UInt32, r: UInt32): UInt32

    func suggestInt16(l: Int16, r: Int16): Int16

    func suggestUInt16(l: UInt16, r: UInt16): UInt16

    func suggestInt8(l: Int8, r: Int8): Int8

    func suggestUInt8(l: UInt8, r: UInt8): UInt8

    func suggestIntNative(l: IntNative, r: IntNative): IntNative

    func suggestUIntNative(l: UIntNative, r: UIntNative): UIntNative

    func suggestFloat64(l: Float64, r: Float64): Float64

    func suggestFloat32(l: Float32, r: Float32): Float32

    func suggestFloat16(l: Float16, r: Float16): Float16
}

struct KeyRandom <: std.unittest.common.KeyFor<std.unittest.prop_test.RandomSource> {
    prop random: std.unittest.prop_test.KeyRandom

    prop name: std.core.String

    init() { /* compiled code */ }
}

class RandomDataProvider <: std.core.Object & std.unittest.common.DataProvider<T> {
    func provide: std.core.Iterable<T>

    let random: std.unittest.prop_test.RandomSource

    let configuration: std.unittest.common.Configuration

    init(configuration: std.unittest.common.Configuration) { /* compiled code */ }
}

class RandomDataShrinker <: std.core.Object & std.unittest.common.DataShrinker<T> {
    func shrink(value: T): std.core.Iterable<T>

    init() { /* compiled code */ }
}

class RandomDataStrategy <: std.core.Object & std.unittest.common.DataStrategy<T> {
    func provider(configuration: std.unittest.common.Configuration): std.unittest.prop_test.RandomDataProvider<T>

    func shrinker(_: std.unittest.common.Configuration): std.unittest.prop_test.RandomDataShrinker<T>

    prop isInfinite: Bool

    init() { /* compiled code */ }
}

func random: std.unittest.prop_test.RandomDataStrategy<T>

class RandomDataProviderRange <: std.core.Object & std.unittest.common.DataProvider<T> {
    func provide: std.core.Iterable<T>

    let random: std.unittest.prop_test.RandomSource

    let configuration: std.unittest.common.Configuration

    let min: T

    let max: T

    init(configuration: std.unittest.common.Configuration, min: T, max: T) { /* compiled code */ }
}

class RandomDataShrinkerRange <: std.core.Object & std.unittest.common.DataShrinker<T> {
    func shrink(value: T): std.core.Iterable<T>

    let min: T

    let max: T
}

class RandomDataStrategyRange <: std.core.Object & std.unittest.common.DataStrategy<T> {
    func provider(configuration: std.unittest.common.Configuration): std.unittest.prop_test.RandomDataProviderRange<T>

    func shrinker(_: std.unittest.common.Configuration): std.unittest.prop_test.RandomDataShrinkerRange<T>

    prop isInfinite: Bool

    let min: T

    let max: T
}

func randomInRange(min: std.core.Option<T>, max: std.core.Option<T>): std.unittest.prop_test.RandomDataStrategyRange<T>

interface Shrink {
    func shrink: std.core.Iterable<T>
}

extend Extend <: Shrink {
}

extend Extend <: Shrink {
}

extend Extend <: Shrink {
    func shrink: std.core.Iterable<Int8>
}

extend Extend <: Shrink {
    func shrink: std.core.Iterable<Int16>
}

extend Extend <: Shrink {
    func shrink: std.core.Iterable<Int32>
}

extend Extend <: Shrink {
    func shrink: std.core.Iterable<Int64>
}

extend Extend <: Shrink {
    func shrink: std.core.Iterable<IntNative>
}

extend Extend <: Shrink {
    func shrink: std.core.Iterable<UInt8>
}

extend Extend <: Shrink {
    func shrink: std.core.Iterable<UInt16>
}

extend Extend <: Shrink {
    func shrink: std.core.Iterable<UInt32>
}

extend Extend <: Shrink {
    func shrink: std.core.Iterable<UInt64>
}

extend Extend <: Shrink {
    func shrink: std.core.Iterable<UIntNative>
}

extend Extend <: Shrink {
    func shrink: std.core.Iterable<Float16>
}

extend Extend <: Shrink {
    func shrink: std.core.Iterable<Float32>
}

extend Extend <: Shrink {
    func shrink: std.core.Iterable<Float64>
}

extend Extend <: Shrink {
    func shrink: std.core.Iterable<Rune>
}

extend Array <: Shrink {
    func shrink: std.core.Iterable<std.core.Array<T>>
}

extend ArrayList <: Shrink {
    func shrink: std.core.Iterable<std.collection.ArrayList<T>>
}

extend HashSet <: Shrink {
    func shrink: std.core.Iterable<std.collection.HashSet<T>>
}

extend HashMap <: Shrink {
    func shrink: std.core.Iterable<std.collection.HashMap<K, V>>
}

extend String <: Shrink {
    func shrink: std.core.Iterable<std.core.String>
}

extend Option <: Shrink {
    func shrink: std.core.Iterable<std.core.Option<T>>
}

extend Function0Wrapper <: Shrink {
}

extend TupleWrapper2 <: Shrink {
    func shrink: std.core.Iterable<std.unittest.prop_test.TupleWrapper2<T0, T1>>
}

extend TupleWrapper3 <: Shrink {
    func shrink: std.core.Iterable<std.unittest.prop_test.TupleWrapper3<T0, T1, T2>>
}

extend TupleWrapper4 <: Shrink {
    func shrink: std.core.Iterable<std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3>>
}

extend TupleWrapper5 <: Shrink {
    func shrink: std.core.Iterable<std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4>>
}

class ShrinkHelpers {
    func shrinkTuple(tuple: (T0, T1), t0: std.core.Iterable<T0>, t1: std.core.Iterable<T1>): std.core.Iterable<(T0, T1)>

    func shrinkTuple(tuple: (T0, T1, T2), t0: std.core.Iterable<T0>, t1: std.core.Iterable<T1>, t2: std.core.Iterable<T2>): std.core.Iterable<(T0, T1, T2)>

    func shrinkTuple(tuple: (T0, T1, T2, T3), t0: std.core.Iterable<T0>, t1: std.core.Iterable<T1>, t2: std.core.Iterable<T2>, t3: std.core.Iterable<T3>): std.core.Iterable<(T0, T1, T2, T3)>

    func shrinkTuple(tuple: (T0, T1, T2, T3, T4), t0: std.core.Iterable<T0>, t1: std.core.Iterable<T1>, t2: std.core.Iterable<T2>, t3: std.core.Iterable<T3>, t4: std.core.Iterable<T4>): std.core.Iterable<(T0, T1, T2, T3, T4)>

    func mix(iterables: std.core.Array<std.core.Iterable<T>>): std.core.Iterable<T>

    init() { /* compiled code */ }
}

extend TupleWrapper2 <: ToString {
    func toString: std.core.String
}

extend TupleWrapper3 <: ToString {
    func toString: std.core.String
}

extend TupleWrapper4 <: ToString {
    func toString: std.core.String
}

extend TupleWrapper5 <: ToString {
    func toString: std.core.String
}

extend TupleWrapper2 <: Equatable {
    operator func ==(other: std.unittest.prop_test.TupleWrapper2<T0, T1>): Bool

    operator func !=(other: std.unittest.prop_test.TupleWrapper2<T0, T1>): Bool
}

extend TupleWrapper3 <: Equatable {
    operator func ==(other: std.unittest.prop_test.TupleWrapper3<T0, T1, T2>): Bool

    operator func !=(other: std.unittest.prop_test.TupleWrapper3<T0, T1, T2>): Bool
}

extend TupleWrapper4 <: Equatable {
    operator func ==(other: std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3>): Bool

    operator func !=(other: std.unittest.prop_test.TupleWrapper4<T0, T1, T2, T3>): Bool
}

extend TupleWrapper5 <: Equatable {
    operator func ==(other: std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4>): Bool

    operator func !=(other: std.unittest.prop_test.TupleWrapper5<T0, T1, T2, T3, T4>): Bool
}

interface IndexAccess {
    func getElementAsAny(index: Int64): std.core.Option<std.core.Any>
}

extend TupleWrapper2 <: IndexAccess {
    func getElementAsAny(index: Int64): std.core.Option<std.core.Any>
}

extend TupleWrapper3 <: IndexAccess {
    func getElementAsAny(index: Int64): std.core.Option<std.core.Any>
}

extend TupleWrapper4 <: IndexAccess {
    func getElementAsAny(index: Int64): std.core.Option<std.core.Any>
}

extend TupleWrapper5 <: IndexAccess {
    func getElementAsAny(index: Int64): std.core.Option<std.core.Any>
}
