// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.math.numeric

import std.convert.*
import std.math.*
import std.random.*
import std.core.*

struct BigInt <: std.core.Comparable<std.math.numeric.BigInt> & std.core.Hashable & std.core.ToString {
    prop sign: Int64 {
        get() { /* compiled code */ }
    }

    prop bitLen: Int64 {
        get() { /* compiled code */ }
    }

    static func randomProbablePrime(bitLen: Int64, certainty: UInt64, rand: std.random.Random): std.math.numeric.BigInt { /* compiled code */ }

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

    init(sign: Bool, bitLen: Int64, rand: std.random.Random) { /* compiled code */ }

    init(s: std.core.String, base: Int64) { /* compiled code */ }

    func testBit(index: Int64): Bool { /* compiled code */ }

    func lowestOneBit(): Int64 { /* compiled code */ }

    func setBit(index: Int64): std.math.numeric.BigInt { /* compiled code */ }

    func clearBit(index: Int64): std.math.numeric.BigInt { /* compiled code */ }

    func flipBit(index: Int64): std.math.numeric.BigInt { /* compiled code */ }

    func isProbablePrime(certainty: UInt64): Bool { /* compiled code */ }

    operator func +(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    operator func -(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    operator func *(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    operator func /(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    operator func %(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    func divAndMod(that: std.math.numeric.BigInt): (std.math.numeric.BigInt, std.math.numeric.BigInt) { /* compiled code */ }

    func quo(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    func rem(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    func quoAndRem(that: std.math.numeric.BigInt): (std.math.numeric.BigInt, std.math.numeric.BigInt) { /* compiled code */ }

    func modInverse(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    operator func -(): std.math.numeric.BigInt { /* compiled code */ }

    operator func **(n: UInt64): std.math.numeric.BigInt { /* compiled code */ }

    func modPow(n: std.math.numeric.BigInt, m: std.core.Option<std.math.numeric.BigInt>): std.math.numeric.BigInt { /* compiled code */ }

    operator func &(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    operator func |(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    operator func ^(that: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

    operator func !(): std.math.numeric.BigInt { /* compiled code */ }

    operator func >>(n: Int64): std.math.numeric.BigInt { /* compiled code */ }

    operator func <<(n: Int64): std.math.numeric.BigInt { /* compiled code */ }

    operator func ==(that: std.math.numeric.BigInt): Bool { /* compiled code */ }

    operator func !=(that: std.math.numeric.BigInt): Bool { /* compiled code */ }

    operator func >(that: std.math.numeric.BigInt): Bool { /* compiled code */ }

    operator func >=(that: std.math.numeric.BigInt): Bool { /* compiled code */ }

    operator func <(that: std.math.numeric.BigInt): Bool { /* compiled code */ }

    operator func <=(that: std.math.numeric.BigInt): Bool { /* compiled code */ }

    func compare(that: std.math.numeric.BigInt): std.core.Ordering { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func toBytes(): std.core.Array<UInt8> { /* compiled code */ }

    func toInt8(overflowHandling: std.math.numeric.OverflowStrategy): Int8 { /* compiled code */ }

    func toInt16(overflowHandling: std.math.numeric.OverflowStrategy): Int16 { /* compiled code */ }

    func toInt32(overflowHandling: std.math.numeric.OverflowStrategy): Int32 { /* compiled code */ }

    func toInt64(overflowHandling: std.math.numeric.OverflowStrategy): Int64 { /* compiled code */ }

    func toIntNative(overflowHandling: std.math.numeric.OverflowStrategy): IntNative { /* compiled code */ }

    func toFloat16(): Float16 { /* compiled code */ }

    func toFloat32(): Float32 { /* compiled code */ }

    func toFloat64(): Float64 { /* compiled code */ }

    func toUInt8(overflowHandling: std.math.numeric.OverflowStrategy): UInt8 { /* compiled code */ }

    func toUInt16(overflowHandling: std.math.numeric.OverflowStrategy): UInt16 { /* compiled code */ }

    func toUInt32(overflowHandling: std.math.numeric.OverflowStrategy): UInt32 { /* compiled code */ }

    func toUInt64(overflowHandling: std.math.numeric.OverflowStrategy): UInt64 { /* compiled code */ }

    func toUIntNative(overflowHandling: std.math.numeric.OverflowStrategy): UIntNative { /* compiled code */ }

    let int: UInt64

    let intArr: std.core.Array<UInt32>

    let negSign: Bool
}

enum OverflowStrategy <: std.core.Equatable<std.math.numeric.OverflowStrategy> & std.core.ToString {
    | Saturating

    | Throwing

    | Wrapping

    operator func ==(that: std.math.numeric.OverflowStrategy): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

func abs(i: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

func sqrt(i: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

func gcd(i1: std.math.numeric.BigInt, i2: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

func lcm(i1: std.math.numeric.BigInt, i2: std.math.numeric.BigInt): std.math.numeric.BigInt { /* compiled code */ }

func countOne(i: std.math.numeric.BigInt): Int64 { /* compiled code */ }

func countOnes(i: std.math.numeric.BigInt): Int64 { /* compiled code */ }

func trailingZeros(x: std.math.numeric.BigInt): Int64 { /* compiled code */ }

extend BigInt <: Number {
}

extend BigInt <: Integer {
    static func isSigned(): Bool { /* compiled code */ }
}

extend BigInt <: Parsable {
    static func parse(value: std.core.String): std.math.numeric.BigInt { /* compiled code */ }

    static func tryParse(value: std.core.String): std.core.Option<std.math.numeric.BigInt> { /* compiled code */ }
}

extend BigInt <: RadixConvertible {
    static func parse(value: std.core.String, radix: Int64): std.math.numeric.BigInt { /* compiled code */ }

    static func tryParse(value: std.core.String, radix: Int64): std.core.Option<std.math.numeric.BigInt> { /* compiled code */ }

    func toString(radix: Int64): std.core.String { /* compiled code */ }
}

extend BigInt <: Formattable {
    func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

struct Decimal <: std.core.Comparable<std.math.numeric.Decimal> & std.core.Hashable & std.core.ToString {
    var _scale: Int32

    var _precision: Int64

    var _value: std.math.numeric.BigInt

    var _sign: Int64

    prop scale: Int32 {
        get() { /* compiled code */ }
    }

    prop precision: Int64 {
        get() { /* compiled code */ }
    }

    prop value: std.math.numeric.BigInt {
        get() { /* compiled code */ }
    }

    prop sign: Int64 {
        get() { /* compiled code */ }
    }

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

    operator func +(d: std.math.numeric.Decimal): std.math.numeric.Decimal { /* compiled code */ }

    operator func -(d: std.math.numeric.Decimal): std.math.numeric.Decimal { /* compiled code */ }

    operator func *(d: std.math.numeric.Decimal): std.math.numeric.Decimal { /* compiled code */ }

    operator func /(d: std.math.numeric.Decimal): std.math.numeric.Decimal { /* compiled code */ }

    func divWithPrecision(d: std.math.numeric.Decimal, precision: Int64, roundingMode: std.math.RoundingMode): std.math.numeric.Decimal { /* compiled code */ }

    func divAndRem(d: std.math.numeric.Decimal): (std.math.numeric.BigInt, std.math.numeric.Decimal) { /* compiled code */ }

    func divAndMod(d: std.math.numeric.Decimal): (std.math.numeric.BigInt, std.math.numeric.Decimal) { /* compiled code */ }

    operator func -(): std.math.numeric.Decimal { /* compiled code */ }

    func shiftPoint(n: Int32): std.math.numeric.Decimal { /* compiled code */ }

    func roundWithPrecision(precision: Int64, roundingMode: std.math.RoundingMode): std.math.numeric.Decimal { /* compiled code */ }

    operator func <(d: std.math.numeric.Decimal): Bool { /* compiled code */ }

    operator func >(d: std.math.numeric.Decimal): Bool { /* compiled code */ }

    operator func <=(d: std.math.numeric.Decimal): Bool { /* compiled code */ }

    operator func >=(d: std.math.numeric.Decimal): Bool { /* compiled code */ }

    operator func ==(d: std.math.numeric.Decimal): Bool { /* compiled code */ }

    operator func !=(d: std.math.numeric.Decimal): Bool { /* compiled code */ }

    func compare(d: std.math.numeric.Decimal): std.core.Ordering { /* compiled code */ }

    operator func **(n: Int64): std.math.numeric.Decimal { /* compiled code */ }

    func powWithPrecision(n: Int64, precision: Int64, roundingMode: std.math.RoundingMode): std.math.numeric.Decimal { /* compiled code */ }

    func sqrtWithPrecision(precision: Int64, roundingMode: std.math.RoundingMode): std.math.numeric.Decimal { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func reScale(newScale: Int32, roundingMode: std.math.RoundingMode): std.math.numeric.Decimal { /* compiled code */ }

    func removeTrailingZeros(): std.math.numeric.Decimal { /* compiled code */ }

    func scaleUnit(): std.math.numeric.Decimal { /* compiled code */ }

    func isInteger(): Bool { /* compiled code */ }

    func toInt8(overflowHandling: std.math.numeric.OverflowStrategy): Int8 { /* compiled code */ }

    func toInt16(overflowHandling: std.math.numeric.OverflowStrategy): Int16 { /* compiled code */ }

    func toInt32(overflowHandling: std.math.numeric.OverflowStrategy): Int32 { /* compiled code */ }

    func toInt64(overflowHandling: std.math.numeric.OverflowStrategy): Int64 { /* compiled code */ }

    func toIntNative(overflowHandling: std.math.numeric.OverflowStrategy): IntNative { /* compiled code */ }

    func toUInt8(overflowHandling: std.math.numeric.OverflowStrategy): UInt8 { /* compiled code */ }

    func toUInt16(overflowHandling: std.math.numeric.OverflowStrategy): UInt16 { /* compiled code */ }

    func toUInt32(overflowHandling: std.math.numeric.OverflowStrategy): UInt32 { /* compiled code */ }

    func toUInt64(overflowHandling: std.math.numeric.OverflowStrategy): UInt64 { /* compiled code */ }

    func toUIntNative(overflowHandling: std.math.numeric.OverflowStrategy): UIntNative { /* compiled code */ }

    func toBigInt(): std.math.numeric.BigInt { /* compiled code */ }

    func toFloat16(): Float16 { /* compiled code */ }

    func toFloat32(): Float32 { /* compiled code */ }

    func toFloat64(): Float64 { /* compiled code */ }

    func toEngString(): std.core.String { /* compiled code */ }

    func toSciString(): std.core.String { /* compiled code */ }
}

func abs(d: std.math.numeric.Decimal): std.math.numeric.Decimal { /* compiled code */ }

func round(d: std.math.numeric.Decimal, roundingMode: std.math.RoundingMode): std.math.numeric.Decimal { /* compiled code */ }

func sqrt(d: std.math.numeric.Decimal): std.math.numeric.Decimal { /* compiled code */ }

extend Decimal <: Number {
}

extend Decimal <: Parsable {
    static func parse(value: std.core.String): std.math.numeric.Decimal { /* compiled code */ }

    static func tryParse(value: std.core.String): std.core.Option<std.math.numeric.Decimal> { /* compiled code */ }
}

extend Decimal <: Formattable {
    func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}
