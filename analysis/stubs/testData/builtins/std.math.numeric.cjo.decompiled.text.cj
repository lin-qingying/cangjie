// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.math.numeric

import std.convert.*
import std.math.*
import std.random.*
import std.core.*

struct BigInt <: std.core.Comparable<std.math.numeric.BigInt> & std.core.Hashable & std.core.ToString {
    prop sign: Int64

    prop bitLen: Int64

    func randomProbablePrime(bitLen: Int64, certainty: UInt64, rand: std.random.Random): std.math.numeric.BigInt

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

    func testBit(index: Int64): Bool

    func lowestOneBit: Int64

    func setBit(index: Int64): std.math.numeric.BigInt

    func clearBit(index: Int64): std.math.numeric.BigInt

    func flipBit(index: Int64): std.math.numeric.BigInt

    func isProbablePrime(certainty: UInt64): Bool

    operator func +(that: std.math.numeric.BigInt): std.math.numeric.BigInt

    operator func -(that: std.math.numeric.BigInt): std.math.numeric.BigInt

    operator func *(that: std.math.numeric.BigInt): std.math.numeric.BigInt

    operator func /(that: std.math.numeric.BigInt): std.math.numeric.BigInt

    operator func %(that: std.math.numeric.BigInt): std.math.numeric.BigInt

    func divAndMod(that: std.math.numeric.BigInt): (std.math.numeric.BigInt, std.math.numeric.BigInt)

    func quo(that: std.math.numeric.BigInt): std.math.numeric.BigInt

    func rem(that: std.math.numeric.BigInt): std.math.numeric.BigInt

    func quoAndRem(that: std.math.numeric.BigInt): (std.math.numeric.BigInt, std.math.numeric.BigInt)

    func modInverse(that: std.math.numeric.BigInt): std.math.numeric.BigInt

    operator func -: std.math.numeric.BigInt

    operator func **(n: UInt64): std.math.numeric.BigInt

    func modPow(n: std.math.numeric.BigInt, m: std.core.Option<std.math.numeric.BigInt>): std.math.numeric.BigInt

    operator func &(that: std.math.numeric.BigInt): std.math.numeric.BigInt

    operator func |(that: std.math.numeric.BigInt): std.math.numeric.BigInt

    operator func ^(that: std.math.numeric.BigInt): std.math.numeric.BigInt

    operator func !: std.math.numeric.BigInt

    operator func >>(n: Int64): std.math.numeric.BigInt

    operator func <<(n: Int64): std.math.numeric.BigInt

    operator func ==(that: std.math.numeric.BigInt): Bool

    operator func !=(that: std.math.numeric.BigInt): Bool

    operator func >(that: std.math.numeric.BigInt): Bool

    operator func >=(that: std.math.numeric.BigInt): Bool

    operator func <(that: std.math.numeric.BigInt): Bool

    operator func <=(that: std.math.numeric.BigInt): Bool

    func compare(that: std.math.numeric.BigInt): std.core.Ordering

    func hashCode: Int64

    func toString: std.core.String

    func toBytes: std.core.Array<UInt8>

    func toInt8(overflowHandling: std.math.numeric.OverflowStrategy): Int8

    func toInt16(overflowHandling: std.math.numeric.OverflowStrategy): Int16

    func toInt32(overflowHandling: std.math.numeric.OverflowStrategy): Int32

    func toInt64(overflowHandling: std.math.numeric.OverflowStrategy): Int64

    func toIntNative(overflowHandling: std.math.numeric.OverflowStrategy): IntNative

    func toFloat16: Float16

    func toFloat32: Float32

    func toFloat64: Float64

    func toUInt8(overflowHandling: std.math.numeric.OverflowStrategy): UInt8

    func toUInt16(overflowHandling: std.math.numeric.OverflowStrategy): UInt16

    func toUInt32(overflowHandling: std.math.numeric.OverflowStrategy): UInt32

    func toUInt64(overflowHandling: std.math.numeric.OverflowStrategy): UInt64

    func toUIntNative(overflowHandling: std.math.numeric.OverflowStrategy): UIntNative

    let int: UInt64

    let intArr: std.core.Array<UInt32>

    let negSign: Bool
}

enum OverflowStrategy <: std.core.Equatable<std.math.numeric.OverflowStrategy> & std.core.ToString {
    | Saturating

    | Throwing

    | Wrapping

    operator func ==(that: std.math.numeric.OverflowStrategy): Bool

    func toString: std.core.String
}

func abs(i: std.math.numeric.BigInt): std.math.numeric.BigInt

func sqrt(i: std.math.numeric.BigInt): std.math.numeric.BigInt

func gcd(i1: std.math.numeric.BigInt, i2: std.math.numeric.BigInt): std.math.numeric.BigInt

func lcm(i1: std.math.numeric.BigInt, i2: std.math.numeric.BigInt): std.math.numeric.BigInt

func countOne(i: std.math.numeric.BigInt): Int64

func countOnes(i: std.math.numeric.BigInt): Int64

func trailingZeros(x: std.math.numeric.BigInt): Int64

extend BigInt <: Number {
}

extend BigInt <: Integer {
    func isSigned: Bool
}

extend BigInt <: Parsable {
    func parse(value: std.core.String): std.math.numeric.BigInt

    func tryParse(value: std.core.String): std.core.Option<std.math.numeric.BigInt>
}

extend BigInt <: RadixConvertible {
    func parse(value: std.core.String, radix: Int64): std.math.numeric.BigInt

    func tryParse(value: std.core.String, radix: Int64): std.core.Option<std.math.numeric.BigInt>

    func toString(radix: Int64): std.core.String
}

extend BigInt <: Formattable {
    func format(fmt: std.core.String): std.core.String
}

struct Decimal <: std.core.Comparable<std.math.numeric.Decimal> & std.core.Hashable & std.core.ToString {
    var _scale: Int32

    var _precision: Int64

    var _value: std.math.numeric.BigInt

    var _sign: Int64

    prop scale: Int32

    prop precision: Int64

    prop value: std.math.numeric.BigInt

    prop sign: Int64

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

    operator func +(d: std.math.numeric.Decimal): std.math.numeric.Decimal

    operator func -(d: std.math.numeric.Decimal): std.math.numeric.Decimal

    operator func *(d: std.math.numeric.Decimal): std.math.numeric.Decimal

    operator func /(d: std.math.numeric.Decimal): std.math.numeric.Decimal

    func divWithPrecision(d: std.math.numeric.Decimal, precision: Int64, roundingMode: std.math.RoundingMode): std.math.numeric.Decimal

    func divAndRem(d: std.math.numeric.Decimal): (std.math.numeric.BigInt, std.math.numeric.Decimal)

    func divAndMod(d: std.math.numeric.Decimal): (std.math.numeric.BigInt, std.math.numeric.Decimal)

    operator func -: std.math.numeric.Decimal

    func shiftPoint(n: Int32): std.math.numeric.Decimal

    func roundWithPrecision(precision: Int64, roundingMode: std.math.RoundingMode): std.math.numeric.Decimal

    operator func <(d: std.math.numeric.Decimal): Bool

    operator func >(d: std.math.numeric.Decimal): Bool

    operator func <=(d: std.math.numeric.Decimal): Bool

    operator func >=(d: std.math.numeric.Decimal): Bool

    operator func ==(d: std.math.numeric.Decimal): Bool

    operator func !=(d: std.math.numeric.Decimal): Bool

    func compare(d: std.math.numeric.Decimal): std.core.Ordering

    operator func **(n: Int64): std.math.numeric.Decimal

    func powWithPrecision(n: Int64, precision: Int64, roundingMode: std.math.RoundingMode): std.math.numeric.Decimal

    func sqrtWithPrecision(precision: Int64, roundingMode: std.math.RoundingMode): std.math.numeric.Decimal

    func hashCode: Int64

    func toString: std.core.String

    func reScale(newScale: Int32, roundingMode: std.math.RoundingMode): std.math.numeric.Decimal

    func removeTrailingZeros: std.math.numeric.Decimal

    func scaleUnit: std.math.numeric.Decimal

    func isInteger: Bool

    func toInt8(overflowHandling: std.math.numeric.OverflowStrategy): Int8

    func toInt16(overflowHandling: std.math.numeric.OverflowStrategy): Int16

    func toInt32(overflowHandling: std.math.numeric.OverflowStrategy): Int32

    func toInt64(overflowHandling: std.math.numeric.OverflowStrategy): Int64

    func toIntNative(overflowHandling: std.math.numeric.OverflowStrategy): IntNative

    func toUInt8(overflowHandling: std.math.numeric.OverflowStrategy): UInt8

    func toUInt16(overflowHandling: std.math.numeric.OverflowStrategy): UInt16

    func toUInt32(overflowHandling: std.math.numeric.OverflowStrategy): UInt32

    func toUInt64(overflowHandling: std.math.numeric.OverflowStrategy): UInt64

    func toUIntNative(overflowHandling: std.math.numeric.OverflowStrategy): UIntNative

    func toBigInt: std.math.numeric.BigInt

    func toFloat16: Float16

    func toFloat32: Float32

    func toFloat64: Float64

    func toEngString: std.core.String

    func toSciString: std.core.String
}

func abs(d: std.math.numeric.Decimal): std.math.numeric.Decimal

func round(d: std.math.numeric.Decimal, roundingMode: std.math.RoundingMode): std.math.numeric.Decimal

func sqrt(d: std.math.numeric.Decimal): std.math.numeric.Decimal

extend Decimal <: Number {
}

extend Decimal <: Parsable {
    func parse(value: std.core.String): std.math.numeric.Decimal

    func tryParse(value: std.core.String): std.core.Option<std.math.numeric.Decimal>
}

extend Decimal <: Formattable {
    func format(fmt: std.core.String): std.core.String
}
