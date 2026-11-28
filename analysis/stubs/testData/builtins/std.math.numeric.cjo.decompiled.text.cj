// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.math.numeric

import std.convert.*
import std.math.*
import std.random.*
import std.core.*

public struct BigInt <: std.core.Comparable<std.math.numeric.BigInt> & std.core.Hashable & std.core.ToString {
    public prop sign: Int64 {
        get() { /* compiled code */ }
    }

    public prop bitLen: Int64 {
        get() { /* compiled code */ }
    }

    public static func randomProbablePrime(bitLen: Int64, certainty: UInt64, rand!: std.random.Random = COMPILED_CODE /* compiled code */): std.math.numeric.BigInt { /* compiled code */ }

    init(bytes: std.core.Array<UInt8>) { /* compiled code */ }

    init(sign: Bool, magnitude: std.core.Array<UInt8>) { /* compiled code */ }

    init(n: Int8) { /* compiled code */ }

    init(n: Int16) { /* compiled code */ }

    init(n: Int32) { /* compiled code */ }

    init(n: Int64) { /* compiled code */ }

    init(n: UInt8) { /* compiled code */ }

    init(n: UInt16) { /* compiled code */ }

    init(n: UInt32) { /* compiled code */ }

    init(n: UInt64) { /* compiled code */ }

    init(n: UIntNative) { /* compiled code */ }

    init(n: IntNative) { /* compiled code */ }

    init(n: Float16) { /* compiled code */ }

    init(n: Float32) { /* compiled code */ }

    init(n: Float64) { /* compiled code */ }

    init(sign: Bool, bitLen: Int64, rand!: std.random.Random = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    @Deprecated[message: "Use member function `public static func parse(value: String, radix!: Int64): BigInt` instead."]
    init(s: std.core.String, base!: Int64 = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    public func testBit(index: Int64): Bool { /* compiled code */ }

    @Deprecated[message: "Use global function `public func trailingZeros(x: BigInt): Int64` instead."]
    public func lowestOneBit(): Int64 { /* compiled code */ }

    public func setBit(index: Int64): std.math.numeric.BigInt { /* compiled code */ }

    public func clearBit(index: Int64): std.math.numeric.BigInt { /* compiled code */ }

    public func flipBit(index: Int64): std.math.numeric.BigInt { /* compiled code */ }

    public func isProbablePrime(certainty: UInt64): Bool { /* compiled code */ }

    public operator func +(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    public operator func -(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    public operator func *(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    public operator func /(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    public operator func %(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    public func divAndMod(that: std.math.numeric.BigInt): (std.math.numeric.BigInt, std.math.numeric.BigInt) { /* compiled code */ }

    @Deprecated[message: "Use member function `public func divAndMod(that: BigInt): (BigInt, BigInt)` instead."]
    public func quo(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    @Deprecated[message: "Use member function `public func divAndMod(that: BigInt): (BigInt, BigInt)` instead."]
    public func rem(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    @Deprecated[message: "Use member function `public func divAndMod(that: BigInt): (BigInt, BigInt)` instead."]
    public func quoAndRem(that: std.math.numeric.BigInt): (std.math.numeric.BigInt, std.math.numeric.BigInt) { /* compiled code */ }

    public func modInverse(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    public operator func -(): std.math.numeric.BigInt { /* compiled code */ }

    public operator func **(n: UInt64): std.math.numeric.BigInt { /* compiled code */ }

    public func modPow(n: std.math.numeric.BigInt, m!: std.core.Option<std.math.numeric.BigInt> = COMPILED_CODE /* compiled code */): std.math.numeric.BigInt { /* compiled code */ }

    public operator func &(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    public operator func |(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    public operator func ^(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    public operator func !(): std.math.numeric.BigInt { /* compiled code */ }

    public operator func >>(n: Int64): std.math.numeric.BigInt { /* compiled code */ }

    public operator func <<(n: Int64): std.math.numeric.BigInt { /* compiled code */ }

    public operator func ==(that: std.math.numeric.BigInt): Bool { /* compiled code */ }

    public operator func !=(that: std.math.numeric.BigInt): Bool { /* compiled code */ }

    public operator func >(that: std.math.numeric.BigInt): Bool { /* compiled code */ }

    public operator func >=(that: std.math.numeric.BigInt): Bool { /* compiled code */ }

    public operator func <(that: std.math.numeric.BigInt): Bool { /* compiled code */ }

    public operator func <=(that: std.math.numeric.BigInt): Bool { /* compiled code */ }

    public func compare(that: std.math.numeric.BigInt): std.core.Ordering { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func toBytes(): std.core.Array<UInt8> { /* compiled code */ }

    public func toInt8(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): Int8 { /* compiled code */ }

    public func toInt16(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): Int16 { /* compiled code */ }

    public func toInt32(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): Int32 { /* compiled code */ }

    public func toInt64(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): Int64 { /* compiled code */ }

    public func toIntNative(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): IntNative { /* compiled code */ }

    public func toFloat16(): Float16 { /* compiled code */ }

    public func toFloat32(): Float32 { /* compiled code */ }

    public func toFloat64(): Float64 { /* compiled code */ }

    public func toUInt8(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): UInt8 { /* compiled code */ }

    public func toUInt16(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): UInt16 { /* compiled code */ }

    public func toUInt32(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): UInt32 { /* compiled code */ }

    public func toUInt64(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): UInt64 { /* compiled code */ }

    public func toUIntNative(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): UIntNative { /* compiled code */ }

    internal let int: UInt64

    internal let intArr: std.core.Array<UInt32>

    internal let negSign: Bool
}

public enum OverflowStrategy <: std.core.Equatable<std.math.numeric.OverflowStrategy> & std.core.ToString {
    | Saturating

    | Throwing

    | Wrapping

    public operator func ==(that: std.math.numeric.OverflowStrategy): Bool { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

public func abs(i: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

public func sqrt(i: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

public func gcd(i1: std.math.numeric.BigInt, i2: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

public func lcm(i1: std.math.numeric.BigInt, i2: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

@Deprecated[message: "Use global function `public func countOnes(i: BigInt): Int64` instead."]
public func countOne(i: std.math.numeric.BigInt): Int64 { /* compiled code */ }

public func countOnes(i: std.math.numeric.BigInt): Int64 { /* compiled code */ }

public func trailingZeros(x: std.math.numeric.BigInt): Int64 { /* compiled code */ }

extend std.math.numeric.BigInt <: Number {
}

extend std.math.numeric.BigInt <: Integer {
    public static func isSigned(): Bool { /* compiled code */ }
}

extend std.math.numeric.BigInt <: Parsable {
    public static func parse(value: std.core.String): std.math.numeric.BigInt { /* compiled code */ }

    public static func tryParse(value: std.core.String): std.core.Option<std.math.numeric.BigInt> { /* compiled code */ }
}

extend std.math.numeric.BigInt <: RadixConvertible {
    public static func parse(value: std.core.String, radix!: Int64): std.math.numeric.BigInt { /* compiled code */ }

    public static func tryParse(value: std.core.String, radix!: Int64): std.core.Option<std.math.numeric.BigInt> { /* compiled code */ }

    public func toString(radix!: Int64): std.core.String { /* compiled code */ }
}

extend std.math.numeric.BigInt <: Formattable {
    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

public struct Decimal <: std.core.Comparable<std.math.numeric.Decimal> & std.core.Hashable & std.core.ToString {
    internal var _scale: Int32

    internal var _precision: Int64

    internal var _value: std.math.numeric.BigInt

    internal var _sign: Int64

    public prop scale: Int32 {
        get() { /* compiled code */ }
    }

    public prop precision: Int64 {
        get() { /* compiled code */ }
    }

    public prop value: std.math.numeric.BigInt {
        get() { /* compiled code */ }
    }

    public prop sign: Int64 {
        get() { /* compiled code */ }
    }

    @Deprecated[message: "Use member function `public static func parse(value: String): Decimal` instead."]
    init(val: std.core.String) { /* compiled code */ }

    init(val: std.math.numeric.BigInt, scale: Int32) { /* compiled code */ }

    init(val: std.math.numeric.BigInt) { /* compiled code */ }

    init(val: Int8) { /* compiled code */ }

    init(val: Int16) { /* compiled code */ }

    init(val: Int32) { /* compiled code */ }

    init(val: IntNative) { /* compiled code */ }

    init(val: Int64) { /* compiled code */ }

    init(val: UInt8) { /* compiled code */ }

    init(val: UInt16) { /* compiled code */ }

    init(val: UInt32) { /* compiled code */ }

    init(val: UIntNative) { /* compiled code */ }

    init(val: UInt64) { /* compiled code */ }

    init(val: Float16) { /* compiled code */ }

    init(val: Float32) { /* compiled code */ }

    init(val: Float64) { /* compiled code */ }

    public operator func +(d: std.math.numeric.Decimal): std.math.numeric.Decimal { /* compiled code */ }

    public operator func -(d: std.math.numeric.Decimal): std.math.numeric.Decimal { /* compiled code */ }

    public operator func *(d: std.math.numeric.Decimal): std.math.numeric.Decimal { /* compiled code */ }

    public operator func /(d: std.math.numeric.Decimal): std.math.numeric.Decimal { /* compiled code */ }

    public func divWithPrecision(d: std.math.numeric.Decimal, precision: Int64, roundingMode!: std.math.RoundingMode = COMPILED_CODE /* compiled code */): std.math.numeric.Decimal { /* compiled code */ }

    @Deprecated[message: "Use member function `public func divAndMod(d: Decimal): (BigInt, Decimal)` instead."]
    public func divAndRem(d: std.math.numeric.Decimal): (std.math.numeric.BigInt, std.math.numeric.Decimal) { /* compiled code */ }

    public func divAndMod(d: std.math.numeric.Decimal): (std.math.numeric.BigInt, std.math.numeric.Decimal) { /* compiled code */ }

    public operator func -(): std.math.numeric.Decimal { /* compiled code */ }

    public func shiftPoint(n: Int32): std.math.numeric.Decimal { /* compiled code */ }

    public func roundWithPrecision(precision: Int64, roundingMode!: std.math.RoundingMode = COMPILED_CODE /* compiled code */): std.math.numeric.Decimal { /* compiled code */ }

    public operator func <(d: std.math.numeric.Decimal): Bool { /* compiled code */ }

    public operator func >(d: std.math.numeric.Decimal): Bool { /* compiled code */ }

    public operator func <=(d: std.math.numeric.Decimal): Bool { /* compiled code */ }

    public operator func >=(d: std.math.numeric.Decimal): Bool { /* compiled code */ }

    public operator func ==(d: std.math.numeric.Decimal): Bool { /* compiled code */ }

    public operator func !=(d: std.math.numeric.Decimal): Bool { /* compiled code */ }

    public func compare(d: std.math.numeric.Decimal): std.core.Ordering { /* compiled code */ }

    public operator func **(n: Int64): std.math.numeric.Decimal { /* compiled code */ }

    public func powWithPrecision(n: Int64, precision: Int64, roundingMode!: std.math.RoundingMode = COMPILED_CODE /* compiled code */): std.math.numeric.Decimal { /* compiled code */ }

    public func sqrtWithPrecision(precision: Int64, roundingMode!: std.math.RoundingMode = COMPILED_CODE /* compiled code */): std.math.numeric.Decimal { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func reScale(newScale: Int32, roundingMode!: std.math.RoundingMode = COMPILED_CODE /* compiled code */): std.math.numeric.Decimal { /* compiled code */ }

    public func removeTrailingZeros(): std.math.numeric.Decimal { /* compiled code */ }

    public func scaleUnit(): std.math.numeric.Decimal { /* compiled code */ }

    public func isInteger(): Bool { /* compiled code */ }

    public func toInt8(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): Int8 { /* compiled code */ }

    public func toInt16(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): Int16 { /* compiled code */ }

    public func toInt32(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): Int32 { /* compiled code */ }

    public func toInt64(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): Int64 { /* compiled code */ }

    public func toIntNative(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): IntNative { /* compiled code */ }

    public func toUInt8(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): UInt8 { /* compiled code */ }

    public func toUInt16(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): UInt16 { /* compiled code */ }

    public func toUInt32(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): UInt32 { /* compiled code */ }

    public func toUInt64(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): UInt64 { /* compiled code */ }

    public func toUIntNative(overflowHandling!: std.math.numeric.OverflowStrategy = COMPILED_CODE /* compiled code */): UIntNative { /* compiled code */ }

    public func toBigInt(): std.math.numeric.BigInt { /* compiled code */ }

    public func toFloat16(): Float16 { /* compiled code */ }

    public func toFloat32(): Float32 { /* compiled code */ }

    public func toFloat64(): Float64 { /* compiled code */ }

    public func toEngString(): std.core.String { /* compiled code */ }

    public func toSciString(): std.core.String { /* compiled code */ }
}

public func abs(d: std.math.numeric.Decimal): std.math.numeric.Decimal { /* compiled code */ }

public func round(d: std.math.numeric.Decimal, roundingMode!: std.math.RoundingMode = COMPILED_CODE /* compiled code */): std.math.numeric.Decimal { /* compiled code */ }

public func sqrt(d: std.math.numeric.Decimal): std.math.numeric.Decimal { /* compiled code */ }

extend std.math.numeric.Decimal <: Number {
}

extend std.math.numeric.Decimal <: Parsable {
    public static func parse(value: std.core.String): std.math.numeric.Decimal { /* compiled code */ }

    public static func tryParse(value: std.core.String): std.core.Option<std.math.numeric.Decimal> { /* compiled code */ }
}

extend std.math.numeric.Decimal <: Formattable {
    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}
