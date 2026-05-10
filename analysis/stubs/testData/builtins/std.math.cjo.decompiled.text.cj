// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.math

import std.core.*

func abs(x: Float64): Float64

func intrinsicFabs(x: T): T

func abs(x: Float32): Float32

func abs(x: Float16): Float16

func abs(x: Int64): Int64

func intrinsicAbs(x: T): T

func abs(x: Int32): Int32

func abs(x: Int16): Int16

func abs(x: Int8): Int8

func sqrt(x: Float16): Float16

func intrinsicSqrt(x: T): T

func sqrt(x: Float32): Float32

func sqrt(x: Float64): Float64

func exp(x: Float64): Float64

func intrinsicExp(x: T): T

func exp(x: Float32): Float32

func exp(x: Float16): Float16

func exp2(x: Float64): Float64

func intrinsicExp2(x: T): T

func exp2(x: Float32): Float32

func exp2(x: Float16): Float16

func log10(x: Float64): Float64

func intrinsicLog10(x: T): T

func log10(x: Float32): Float32

func log10(x: Float16): Float16

func log2(x: Float64): Float64

func intrinsicLog2(x: T): T

func log2(x: Float32): Float32

func log2(x: Float16): Float16

func log(x: Float64): Float64

func intrinsicLog(x: T): T

func log(x: Float32): Float32

func log(x: Float16): Float16

func floor(x: Float64): Float64

func intrinsicFloor(x: T): T

func floor(x: Float32): Float32

func floor(x: Float16): Float16

func ceil(x: Float64): Float64

func intrinsicCeil(x: T): T

func ceil(x: Float32): Float32

func ceil(x: Float16): Float16

func trunc(x: Float64): Float64

func intrinsicTrunc(x: T): T

func trunc(x: Float32): Float32

func trunc(x: Float16): Float16

func sin(x: Float64): Float64

func intrinsicSin(x: T): T

func sin(x: Float32): Float32

func sin(x: Float16): Float16

func cos(x: Float64): Float64

func intrinsicCos(x: T): T

func cos(x: Float32): Float32

func cos(x: Float16): Float16

func pow(base: Float64, exponent: Float64): Float64

func intrinsicPow(x: T, y: T): T

func pow(base: Float32, exponent: Float32): Float32

func pow(base: Float64, exponent: Int64): Float64

func CJ_CORE_FastPowerDoubleInt64(x: Float64, y: Int64): Float64

func pow(base: Float32, exponent: Int32): Float32

func intrinsicPowi(x: Float32, y: Int32): Float32

enum RoundingMode <: std.core.Equatable<std.math.RoundingMode> & std.core.ToString {
    | Ceiling

    | Down

    | Floor

    | HalfEven

    | HalfUp

    | Up

    operator func ==(that: std.math.RoundingMode): Bool

    func toString: std.core.String
}

extend Extend <: MathExtension {
    func GetPI: Float64

    func GetE: Float64
}

interface MathExtension {
    func GetPI: T

    func GetE: T
}

extend Extend <: MathExtension {
    func GetPI: Float32

    func GetE: Float32
}

extend Extend <: MathExtension {
    func GetPI: Float16

    func GetE: Float16
}

extend Extend <: MaxMinValue {
    func getMax: Int8

    func getMin: Int8
}

interface MaxMinValue {
    func getMax: T

    func getMin: T
}

extend Extend <: MaxMinValue {
    func getMax: Int16

    func getMin: Int16
}

extend Extend <: MaxMinValue {
    func getMax: Int32

    func getMin: Int32
}

extend Extend <: MaxMinValue {
    func getMax: Int64

    func getMin: Int64
}

extend Extend <: MaxMinValue {
    func getMax: IntNative

    func getMin: IntNative
}

extend Extend <: MaxMinValue {
    func getMax: UInt8

    func getMin: UInt8
}

extend Extend <: MaxMinValue {
    func getMax: UInt16

    func getMin: UInt16
}

extend Extend <: MaxMinValue {
    func getMax: UInt32

    func getMin: UInt32
}

extend Extend <: MaxMinValue {
    func getMax: UInt64

    func getMin: UInt64
}

extend Extend <: MaxMinValue {
    func getMax: UIntNative

    func getMin: UIntNative
}

extend Extend <: MaxMinValue {
    func getMax: Float16

    func getMin: Float16
}

extend Extend <: MaxMinValue {
    func getMax: Float32

    func getMin: Float32
}

extend Extend <: MaxMinValue {
    func getMax: Float64

    func getMin: Float64
}

extend Extend <: Number {
    operator func *(right: Int8): Int8

    operator func +(right: Int8): Int8

    operator func -(right: Int8): Int8

    operator func -: Int8

    operator func /(right: Int8): Int8
}

interface Number {
    operator func +(rhs: T): T

    operator func -(rhs: T): T

    operator func *(rhs: T): T

    operator func /(rhs: T): T

    operator func -: T
}

extend Extend <: Number {
    operator func *(right: Int16): Int16

    operator func +(right: Int16): Int16

    operator func -(right: Int16): Int16

    operator func -: Int16

    operator func /(right: Int16): Int16
}

extend Extend <: Number {
    operator func *(right: Int32): Int32

    operator func +(right: Int32): Int32

    operator func -(right: Int32): Int32

    operator func -: Int32

    operator func /(right: Int32): Int32
}

extend Extend <: Number {
    operator func *(right: Int64): Int64

    operator func +(right: Int64): Int64

    operator func -(right: Int64): Int64

    operator func -: Int64

    operator func /(right: Int64): Int64
}

extend Extend <: Number {
    operator func *(right: IntNative): IntNative

    operator func +(right: IntNative): IntNative

    operator func -(right: IntNative): IntNative

    operator func -: IntNative

    operator func /(right: IntNative): IntNative
}

extend Extend <: Number {
    operator func *(right: UInt8): UInt8

    operator func +(right: UInt8): UInt8

    operator func -(right: UInt8): UInt8

    operator func -: UInt8

    operator func /(right: UInt8): UInt8
}

extend Extend <: Number {
    operator func *(right: UInt16): UInt16

    operator func +(right: UInt16): UInt16

    operator func -(right: UInt16): UInt16

    operator func -: UInt16

    operator func /(right: UInt16): UInt16
}

extend Extend <: Number {
    operator func *(right: UInt32): UInt32

    operator func +(right: UInt32): UInt32

    operator func -(right: UInt32): UInt32

    operator func -: UInt32

    operator func /(right: UInt32): UInt32
}

extend Extend <: Number {
    operator func *(right: UInt64): UInt64

    operator func +(right: UInt64): UInt64

    operator func -(right: UInt64): UInt64

    operator func -: UInt64

    operator func /(right: UInt64): UInt64
}

extend Extend <: Number {
    operator func *(right: UIntNative): UIntNative

    operator func +(right: UIntNative): UIntNative

    operator func -(right: UIntNative): UIntNative

    operator func -: UIntNative

    operator func /(right: UIntNative): UIntNative
}

extend Extend <: Number {
    operator func *(right: Float16): Float16

    operator func +(right: Float16): Float16

    operator func -(right: Float16): Float16

    operator func -: Float16

    operator func /(right: Float16): Float16
}

extend Extend <: Number {
    operator func *(right: Float32): Float32

    operator func +(right: Float32): Float32

    operator func -(right: Float32): Float32

    operator func -: Float32

    operator func /(right: Float32): Float32
}

extend Extend <: Number {
    operator func *(right: Float64): Float64

    operator func +(right: Float64): Float64

    operator func -(right: Float64): Float64

    operator func -: Float64

    operator func /(right: Float64): Float64
}

extend Extend <: Integer {
    func isSigned: Bool

    operator func !: Int8

    operator func %(right: Int8): Int8

    operator func &(right: Int8): Int8

    operator func <<(right: Int64): Int8

    operator func >>(right: Int64): Int8

    operator func ^(right: Int8): Int8

    operator func |(right: Int8): Int8
}

interface Integer <: std.math.Number<T> {
    func isSigned: Bool

    operator func %(rhs: T): T

    operator func &(rhs: T): T

    operator func |(rhs: T): T

    operator func ^(rhs: T): T

    operator func !: T

    operator func >>(n: Int64): T

    operator func <<(n: Int64): T
}

extend Extend <: Integer {
    func isSigned: Bool

    operator func !: Int16

    operator func %(right: Int16): Int16

    operator func &(right: Int16): Int16

    operator func <<(right: Int64): Int16

    operator func >>(right: Int64): Int16

    operator func ^(right: Int16): Int16

    operator func |(right: Int16): Int16
}

extend Extend <: Integer {
    func isSigned: Bool

    operator func !: Int32

    operator func %(right: Int32): Int32

    operator func &(right: Int32): Int32

    operator func <<(right: Int64): Int32

    operator func >>(right: Int64): Int32

    operator func ^(right: Int32): Int32

    operator func |(right: Int32): Int32
}

extend Extend <: Integer {
    func isSigned: Bool

    operator func !: Int64

    operator func %(right: Int64): Int64

    operator func &(right: Int64): Int64

    operator func <<(right: Int64): Int64

    operator func >>(right: Int64): Int64

    operator func ^(right: Int64): Int64

    operator func |(right: Int64): Int64
}

extend Extend <: Integer {
    func isSigned: Bool

    operator func !: IntNative

    operator func %(right: IntNative): IntNative

    operator func &(right: IntNative): IntNative

    operator func <<(right: Int64): IntNative

    operator func >>(right: Int64): IntNative

    operator func ^(right: IntNative): IntNative

    operator func |(right: IntNative): IntNative
}

extend Extend <: Integer {
    func isSigned: Bool

    operator func !: UInt8

    operator func %(right: UInt8): UInt8

    operator func &(right: UInt8): UInt8

    operator func <<(right: Int64): UInt8

    operator func >>(right: Int64): UInt8

    operator func ^(right: UInt8): UInt8

    operator func |(right: UInt8): UInt8
}

extend Extend <: Integer {
    func isSigned: Bool

    operator func !: UInt16

    operator func %(right: UInt16): UInt16

    operator func &(right: UInt16): UInt16

    operator func <<(right: Int64): UInt16

    operator func >>(right: Int64): UInt16

    operator func ^(right: UInt16): UInt16

    operator func |(right: UInt16): UInt16
}

extend Extend <: Integer {
    func isSigned: Bool

    operator func !: UInt32

    operator func %(right: UInt32): UInt32

    operator func &(right: UInt32): UInt32

    operator func <<(right: Int64): UInt32

    operator func >>(right: Int64): UInt32

    operator func ^(right: UInt32): UInt32

    operator func |(right: UInt32): UInt32
}

extend Extend <: Integer {
    func isSigned: Bool

    operator func !: UInt64

    operator func %(right: UInt64): UInt64

    operator func &(right: UInt64): UInt64

    operator func <<(right: Int64): UInt64

    operator func >>(right: Int64): UInt64

    operator func ^(right: UInt64): UInt64

    operator func |(right: UInt64): UInt64
}

extend Extend <: Integer {
    func isSigned: Bool

    operator func !: UIntNative

    operator func %(right: UIntNative): UIntNative

    operator func &(right: UIntNative): UIntNative

    operator func <<(right: Int64): UIntNative

    operator func >>(right: Int64): UIntNative

    operator func ^(right: UIntNative): UIntNative

    operator func |(right: UIntNative): UIntNative
}

extend Extend <: FloatingPoint {
    func getPI: Float16

    func getE: Float16

    func getNaN: Float16

    func getInf: Float16

    func getMinDenormal: Float16

    func getMinNormal: Float16
}

interface FloatingPoint <: std.math.Number<T> {
    func getPI: T

    func getE: T

    func getNaN: T

    func getInf: T

    func getMinDenormal: T

    func getMinNormal: T

    func isInf: Bool

    func isNaN: Bool

    func isNormal: Bool
}

extend Extend <: FloatingPoint {
    func getPI: Float32

    func getE: Float32

    func getNaN: Float32

    func getInf: Float32

    func getMinDenormal: Float32

    func getMinNormal: Float32
}

extend Extend <: FloatingPoint {
    func getPI: Float64

    func getE: Float64

    func getNaN: Float64

    func getInf: Float64

    func getMinDenormal: Float64

    func getMinNormal: Float64
}

func cbrt(x: Float64): Float64

func cbrt(x: Float32): Float32

func cbrt(x: Float16): Float16

func logBase(x: Float32, base: Float32): Float32

func logBase(x: Float64, base: Float64): Float64

func logBase(x: Float16, base: Float16): Float16

func gamma(x: Float64): Float64

func gamma(x: Float32): Float32

func gamma(x: Float16): Float16

func erf(x: Float64): Float64

func erf(x: Float32): Float32

func erf(x: Float16): Float16

func tan(x: Float64): Float64

func tan(x: Float32): Float32

func tan(x: Float16): Float16

func asin(x: Float64): Float64

func asin(x: Float32): Float32

func asin(x: Float16): Float16

func acos(x: Float64): Float64

func acos(x: Float32): Float32

func acos(x: Float16): Float16

func atan(x: Float64): Float64

func atan(x: Float32): Float32

func atan(x: Float16): Float16

func sinh(x: Float64): Float64

func sinh(x: Float32): Float32

func sinh(x: Float16): Float16

func cosh(x: Float64): Float64

func cosh(x: Float32): Float32

func cosh(x: Float16): Float16

func tanh(x: Float64): Float64

func tanh(x: Float32): Float32

func tanh(x: Float16): Float16

func asinh(x: Float64): Float64

func asinh(x: Float32): Float32

func asinh(x: Float16): Float16

func acosh(x: Float64): Float64

func acosh(x: Float32): Float32

func acosh(x: Float16): Float16

func atanh(x: Float64): Float64

func atanh(x: Float32): Float32

func atanh(x: Float16): Float16

var uint8len: std.core.Array<Int64>

var poptab: std.core.Array<Int64>

var revtab: std.core.Array<UInt8>

func leadingZeros(x: UInt64): Int64

func leadingZeros(x: UInt32): Int64

func leadingZeros(x: UInt16): Int64

func leadingZeros(x: UInt8): Int64

func leadingZeros(x: Int8): Int64

func leadingZeros(x: Int16): Int64

func leadingZeros(x: Int32): Int64

func leadingZeros(x: Int64): Int64

func trailingZeros(x: UInt64): Int64

func trailingZeros(x: UInt32): Int64

func trailingZeros(x: UInt16): Int64

func trailingZeros(x: UInt8): Int64

func trailingZeros(x: Int8): Int64

func trailingZeros(x: Int16): Int64

func trailingZeros(x: Int32): Int64

func trailingZeros(x: Int64): Int64

func countOnes(x: UInt64): Int64

func countOnes(x: UInt32): Int64

func countOnes(x: UInt16): Int64

func countOnes(x: UInt8): Int64

func countOnes(x: Int8): Int64

func countOnes(x: Int16): Int64

func countOnes(x: Int32): Int64

func countOnes(x: Int64): Int64

func countOne(x: Int8): Int8

func countOne(x: Int16): Int8

func countOne(x: Int32): Int8

func countOne(x: Int64): Int8

func countOne(x: UInt8): Int8

func countOne(x: UInt16): Int8

func countOne(x: UInt32): Int8

func countOne(x: UInt64): Int8

func reverse(x: UInt64): UInt64

func reverse(x: UInt32): UInt32

func reverse(x: UInt16): UInt16

func reverse(x: UInt8): UInt8

func checkedAbs(x: Int8): std.core.Option<Int8>

func checkedAbs(x: Int16): std.core.Option<Int16>

func checkedAbs(x: Int32): std.core.Option<Int32>

func checkedAbs(x: Int64): std.core.Option<Int64>

func rotate(num: UInt8, d: Int8): UInt8

func rotate(num: UInt16, d: Int8): UInt16

func rotate(num: UInt32, d: Int8): UInt32

func rotate(num: UInt64, d: Int8): UInt64

func rotate(num: Int8, d: Int8): Int8

func rotate(num: Int16, d: Int8): Int16

func rotate(num: Int32, d: Int8): Int32

func rotate(num: Int64, d: Int8): Int64

func gcd(x: Int8, y: Int8): Int8

func gcd(x: Int16, y: Int16): Int16

func gcd(x: Int32, y: Int32): Int32

func gcd(x: Int64, y: Int64): Int64

func gcd(x: UInt8, y: UInt8): UInt8

func gcd(x: UInt16, y: UInt16): UInt16

func gcd(x: UInt32, y: UInt32): UInt32

func gcd(x: UInt64, y: UInt64): UInt64

func lcm(x: Int8, y: Int8): Int8

func lcm(x: Int16, y: Int16): Int16

func lcm(x: Int32, y: Int32): Int32

func lcm(x: Int64, y: Int64): Int64

func lcm(x: UInt8, y: UInt8): UInt8

func lcm(x: UInt16, y: UInt16): UInt16

func lcm(x: UInt32, y: UInt32): UInt32

func lcm(x: UInt64, y: UInt64): UInt64

func clamp(v: Float16, min: Float16, max: Float16): Float16

func clamp(v: Float32, min: Float32, max: Float32): Float32

func clamp(v: Float64, min: Float64, max: Float64): Float64

func round(x: Float64): Float64

func round(x: Float32): Float32

func round(x: Float16): Float16

func atan2(y: Float64, x: Float64): Float64

func atan2(y: Float32, x: Float32): Float32

func atan2(y: Float16, x: Float16): Float16

func fmod(x: Float64, y: Float64): Float64

func fmod(x: Float32, y: Float32): Float32

func fmod(x: Float16, y: Float16): Float16

func isEqual(a: Float16, b: Float16): Bool

func isEqual(a: Float32, b: Float32): Bool

func isEqual(a: Float64, b: Float64): Bool

func CJ_MATH_Tan(x: Float64): Float64

func CJ_MATH_Tanf(x: Float32): Float32

func CJ_MATH_Asin(x: Float64): Float64

func CJ_MATH_Asinf(x: Float32): Float32

func CJ_MATH_Acos(x: Float64): Float64

func CJ_MATH_Acosf(x: Float32): Float32

func CJ_MATH_Atan(x: Float64): Float64

func CJ_MATH_Atanf(x: Float32): Float32

func CJ_MATH_Atan2(y: Float64, x: Float64): Float64

func CJ_MATH_Atan2f(y: Float32, x: Float32): Float32

func CJ_MATH_Sinh(x: Float64): Float64

func CJ_MATH_Sinhf(x: Float32): Float32

func CJ_MATH_Cosh(x: Float64): Float64

func CJ_MATH_Coshf(x: Float32): Float32

func CJ_MATH_Tanh(x: Float64): Float64

func CJ_MATH_Tanhf(x: Float32): Float32

func CJ_MATH_Asinh(x: Float64): Float64

func CJ_MATH_Asinhf(x: Float32): Float32

func CJ_MATH_Acosh(x: Float64): Float64

func CJ_MATH_Acoshf(x: Float32): Float32

func CJ_MATH_Atanh(x: Float64): Float64

func CJ_MATH_Atanhf(x: Float32): Float32

func CJ_MATH_Cbrt(x: Float64): Float64

func CJ_MATH_Cbrtf(x: Float32): Float32

func CJ_MATH_Tgamma(x: Float64): Float64

func CJ_MATH_Tgammaf(x: Float32): Float32

func CJ_MATH_Erf(x: Float64): Float64

func CJ_MATH_Erff(x: Float32): Float32

func CJ_MATH_Fmod(x: Float64, y: Float64): Float64

func CJ_MATH_Fmodf(x: Float32, y: Float32): Float32

func intrinsicRound(x: T): T
