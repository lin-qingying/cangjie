// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.math

import std.core.*

func abs(x: Float64): Float64 { /* compiled code */ }

func intrinsicFabs(x: T): T { /* compiled code */ }

func abs(x: Float32): Float32 { /* compiled code */ }

func abs(x: Float16): Float16 { /* compiled code */ }

func abs(x: Int64): Int64 { /* compiled code */ }

func intrinsicAbs(x: T): T { /* compiled code */ }

func abs(x: Int32): Int32 { /* compiled code */ }

func abs(x: Int16): Int16 { /* compiled code */ }

func abs(x: Int8): Int8 { /* compiled code */ }

func sqrt(x: Float16): Float16 { /* compiled code */ }

func intrinsicSqrt(x: T): T { /* compiled code */ }

func sqrt(x: Float32): Float32 { /* compiled code */ }

func sqrt(x: Float64): Float64 { /* compiled code */ }

func exp(x: Float64): Float64 { /* compiled code */ }

func intrinsicExp(x: T): T { /* compiled code */ }

func exp(x: Float32): Float32 { /* compiled code */ }

func exp(x: Float16): Float16 { /* compiled code */ }

func exp2(x: Float64): Float64 { /* compiled code */ }

func intrinsicExp2(x: T): T { /* compiled code */ }

func exp2(x: Float32): Float32 { /* compiled code */ }

func exp2(x: Float16): Float16 { /* compiled code */ }

func log10(x: Float64): Float64 { /* compiled code */ }

func intrinsicLog10(x: T): T { /* compiled code */ }

func log10(x: Float32): Float32 { /* compiled code */ }

func log10(x: Float16): Float16 { /* compiled code */ }

func log2(x: Float64): Float64 { /* compiled code */ }

func intrinsicLog2(x: T): T { /* compiled code */ }

func log2(x: Float32): Float32 { /* compiled code */ }

func log2(x: Float16): Float16 { /* compiled code */ }

func log(x: Float64): Float64 { /* compiled code */ }

func intrinsicLog(x: T): T { /* compiled code */ }

func log(x: Float32): Float32 { /* compiled code */ }

func log(x: Float16): Float16 { /* compiled code */ }

func floor(x: Float64): Float64 { /* compiled code */ }

func intrinsicFloor(x: T): T { /* compiled code */ }

func floor(x: Float32): Float32 { /* compiled code */ }

func floor(x: Float16): Float16 { /* compiled code */ }

func ceil(x: Float64): Float64 { /* compiled code */ }

func intrinsicCeil(x: T): T { /* compiled code */ }

func ceil(x: Float32): Float32 { /* compiled code */ }

func ceil(x: Float16): Float16 { /* compiled code */ }

func trunc(x: Float64): Float64 { /* compiled code */ }

func intrinsicTrunc(x: T): T { /* compiled code */ }

func trunc(x: Float32): Float32 { /* compiled code */ }

func trunc(x: Float16): Float16 { /* compiled code */ }

func sin(x: Float64): Float64 { /* compiled code */ }

func intrinsicSin(x: T): T { /* compiled code */ }

func sin(x: Float32): Float32 { /* compiled code */ }

func sin(x: Float16): Float16 { /* compiled code */ }

func cos(x: Float64): Float64 { /* compiled code */ }

func intrinsicCos(x: T): T { /* compiled code */ }

func cos(x: Float32): Float32 { /* compiled code */ }

func cos(x: Float16): Float16 { /* compiled code */ }

func pow(base: Float64, exponent: Float64): Float64 { /* compiled code */ }

func intrinsicPow(x: T, y: T): T { /* compiled code */ }

func pow(base: Float32, exponent: Float32): Float32 { /* compiled code */ }

func pow(base: Float64, exponent: Int64): Float64 { /* compiled code */ }

foreign func CJ_CORE_FastPowerDoubleInt64(x: Float64, y: Int64): Float64

func pow(base: Float32, exponent: Int32): Float32 { /* compiled code */ }

func intrinsicPowi(x: Float32, y: Int32): Float32 { /* compiled code */ }

enum RoundingMode <: std.core.Equatable<std.math.RoundingMode> & std.core.ToString {
    | Ceiling

    | Down

    | Floor

    | HalfEven

    | HalfUp

    | Up

    operator func ==(that: std.math.RoundingMode): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

extend Extend <: MathExtension {
    static func GetPI(): Float64 { /* compiled code */ }

    static func GetE(): Float64 { /* compiled code */ }
}

interface MathExtension {
    static func GetPI(): T { /* compiled code */ }

    static func GetE(): T { /* compiled code */ }
}

extend Extend <: MathExtension {
    static func GetPI(): Float32 { /* compiled code */ }

    static func GetE(): Float32 { /* compiled code */ }
}

extend Extend <: MathExtension {
    static func GetPI(): Float16 { /* compiled code */ }

    static func GetE(): Float16 { /* compiled code */ }
}

extend Extend <: MaxMinValue {
    static func getMax(): Int8 { /* compiled code */ }

    static func getMin(): Int8 { /* compiled code */ }
}

interface MaxMinValue {
    static func getMax(): T { /* compiled code */ }

    static func getMin(): T { /* compiled code */ }
}

extend Extend <: MaxMinValue {
    static func getMax(): Int16 { /* compiled code */ }

    static func getMin(): Int16 { /* compiled code */ }
}

extend Extend <: MaxMinValue {
    static func getMax(): Int32 { /* compiled code */ }

    static func getMin(): Int32 { /* compiled code */ }
}

extend Extend <: MaxMinValue {
    static func getMax(): Int64 { /* compiled code */ }

    static func getMin(): Int64 { /* compiled code */ }
}

extend Extend <: MaxMinValue {
    static func getMax(): IntNative { /* compiled code */ }

    static func getMin(): IntNative { /* compiled code */ }
}

extend Extend <: MaxMinValue {
    static func getMax(): UInt8 { /* compiled code */ }

    static func getMin(): UInt8 { /* compiled code */ }
}

extend Extend <: MaxMinValue {
    static func getMax(): UInt16 { /* compiled code */ }

    static func getMin(): UInt16 { /* compiled code */ }
}

extend Extend <: MaxMinValue {
    static func getMax(): UInt32 { /* compiled code */ }

    static func getMin(): UInt32 { /* compiled code */ }
}

extend Extend <: MaxMinValue {
    static func getMax(): UInt64 { /* compiled code */ }

    static func getMin(): UInt64 { /* compiled code */ }
}

extend Extend <: MaxMinValue {
    static func getMax(): UIntNative { /* compiled code */ }

    static func getMin(): UIntNative { /* compiled code */ }
}

extend Extend <: MaxMinValue {
    static func getMax(): Float16 { /* compiled code */ }

    static func getMin(): Float16 { /* compiled code */ }
}

extend Extend <: MaxMinValue {
    static func getMax(): Float32 { /* compiled code */ }

    static func getMin(): Float32 { /* compiled code */ }
}

extend Extend <: MaxMinValue {
    static func getMax(): Float64 { /* compiled code */ }

    static func getMin(): Float64 { /* compiled code */ }
}

extend Extend <: Number {
    operator func *(right: Int8): Int8 { /* compiled code */ }

    operator func +(right: Int8): Int8 { /* compiled code */ }

    operator func -(right: Int8): Int8 { /* compiled code */ }

    operator func -(): Int8 { /* compiled code */ }

    operator func /(right: Int8): Int8 { /* compiled code */ }
}

interface Number {
    operator func +(rhs: T): T { /* compiled code */ }

    operator func -(rhs: T): T { /* compiled code */ }

    operator func *(rhs: T): T { /* compiled code */ }

    operator func /(rhs: T): T { /* compiled code */ }

    operator func -(): T { /* compiled code */ }
}

extend Extend <: Number {
    operator func *(right: Int16): Int16 { /* compiled code */ }

    operator func +(right: Int16): Int16 { /* compiled code */ }

    operator func -(right: Int16): Int16 { /* compiled code */ }

    operator func -(): Int16 { /* compiled code */ }

    operator func /(right: Int16): Int16 { /* compiled code */ }
}

extend Extend <: Number {
    operator func *(right: Int32): Int32 { /* compiled code */ }

    operator func +(right: Int32): Int32 { /* compiled code */ }

    operator func -(right: Int32): Int32 { /* compiled code */ }

    operator func -(): Int32 { /* compiled code */ }

    operator func /(right: Int32): Int32 { /* compiled code */ }
}

extend Extend <: Number {
    operator func *(right: Int64): Int64 { /* compiled code */ }

    operator func +(right: Int64): Int64 { /* compiled code */ }

    operator func -(right: Int64): Int64 { /* compiled code */ }

    operator func -(): Int64 { /* compiled code */ }

    operator func /(right: Int64): Int64 { /* compiled code */ }
}

extend Extend <: Number {
    operator func *(right: IntNative): IntNative { /* compiled code */ }

    operator func +(right: IntNative): IntNative { /* compiled code */ }

    operator func -(right: IntNative): IntNative { /* compiled code */ }

    operator func -(): IntNative { /* compiled code */ }

    operator func /(right: IntNative): IntNative { /* compiled code */ }
}

extend Extend <: Number {
    operator func *(right: UInt8): UInt8 { /* compiled code */ }

    operator func +(right: UInt8): UInt8 { /* compiled code */ }

    operator func -(right: UInt8): UInt8 { /* compiled code */ }

    operator func -(): UInt8 { /* compiled code */ }

    operator func /(right: UInt8): UInt8 { /* compiled code */ }
}

extend Extend <: Number {
    operator func *(right: UInt16): UInt16 { /* compiled code */ }

    operator func +(right: UInt16): UInt16 { /* compiled code */ }

    operator func -(right: UInt16): UInt16 { /* compiled code */ }

    operator func -(): UInt16 { /* compiled code */ }

    operator func /(right: UInt16): UInt16 { /* compiled code */ }
}

extend Extend <: Number {
    operator func *(right: UInt32): UInt32 { /* compiled code */ }

    operator func +(right: UInt32): UInt32 { /* compiled code */ }

    operator func -(right: UInt32): UInt32 { /* compiled code */ }

    operator func -(): UInt32 { /* compiled code */ }

    operator func /(right: UInt32): UInt32 { /* compiled code */ }
}

extend Extend <: Number {
    operator func *(right: UInt64): UInt64 { /* compiled code */ }

    operator func +(right: UInt64): UInt64 { /* compiled code */ }

    operator func -(right: UInt64): UInt64 { /* compiled code */ }

    operator func -(): UInt64 { /* compiled code */ }

    operator func /(right: UInt64): UInt64 { /* compiled code */ }
}

extend Extend <: Number {
    operator func *(right: UIntNative): UIntNative { /* compiled code */ }

    operator func +(right: UIntNative): UIntNative { /* compiled code */ }

    operator func -(right: UIntNative): UIntNative { /* compiled code */ }

    operator func -(): UIntNative { /* compiled code */ }

    operator func /(right: UIntNative): UIntNative { /* compiled code */ }
}

extend Extend <: Number {
    operator func *(right: Float16): Float16 { /* compiled code */ }

    operator func +(right: Float16): Float16 { /* compiled code */ }

    operator func -(right: Float16): Float16 { /* compiled code */ }

    operator func -(): Float16 { /* compiled code */ }

    operator func /(right: Float16): Float16 { /* compiled code */ }
}

extend Extend <: Number {
    operator func *(right: Float32): Float32 { /* compiled code */ }

    operator func +(right: Float32): Float32 { /* compiled code */ }

    operator func -(right: Float32): Float32 { /* compiled code */ }

    operator func -(): Float32 { /* compiled code */ }

    operator func /(right: Float32): Float32 { /* compiled code */ }
}

extend Extend <: Number {
    operator func *(right: Float64): Float64 { /* compiled code */ }

    operator func +(right: Float64): Float64 { /* compiled code */ }

    operator func -(right: Float64): Float64 { /* compiled code */ }

    operator func -(): Float64 { /* compiled code */ }

    operator func /(right: Float64): Float64 { /* compiled code */ }
}

extend Extend <: Integer {
    static func isSigned(): Bool { /* compiled code */ }

    operator func !(): Int8 { /* compiled code */ }

    operator func %(right: Int8): Int8 { /* compiled code */ }

    operator func &(right: Int8): Int8 { /* compiled code */ }

    operator func <<(right: Int64): Int8 { /* compiled code */ }

    operator func >>(right: Int64): Int8 { /* compiled code */ }

    operator func ^(right: Int8): Int8 { /* compiled code */ }

    operator func |(right: Int8): Int8 { /* compiled code */ }
}

interface Integer <: std.math.Number<T> {
    static func isSigned(): Bool { /* compiled code */ }

    operator func %(rhs: T): T { /* compiled code */ }

    operator func &(rhs: T): T { /* compiled code */ }

    operator func |(rhs: T): T { /* compiled code */ }

    operator func ^(rhs: T): T { /* compiled code */ }

    operator func !(): T { /* compiled code */ }

    operator func >>(n: Int64): T { /* compiled code */ }

    operator func <<(n: Int64): T { /* compiled code */ }
}

extend Extend <: Integer {
    static func isSigned(): Bool { /* compiled code */ }

    operator func !(): Int16 { /* compiled code */ }

    operator func %(right: Int16): Int16 { /* compiled code */ }

    operator func &(right: Int16): Int16 { /* compiled code */ }

    operator func <<(right: Int64): Int16 { /* compiled code */ }

    operator func >>(right: Int64): Int16 { /* compiled code */ }

    operator func ^(right: Int16): Int16 { /* compiled code */ }

    operator func |(right: Int16): Int16 { /* compiled code */ }
}

extend Extend <: Integer {
    static func isSigned(): Bool { /* compiled code */ }

    operator func !(): Int32 { /* compiled code */ }

    operator func %(right: Int32): Int32 { /* compiled code */ }

    operator func &(right: Int32): Int32 { /* compiled code */ }

    operator func <<(right: Int64): Int32 { /* compiled code */ }

    operator func >>(right: Int64): Int32 { /* compiled code */ }

    operator func ^(right: Int32): Int32 { /* compiled code */ }

    operator func |(right: Int32): Int32 { /* compiled code */ }
}

extend Extend <: Integer {
    static func isSigned(): Bool { /* compiled code */ }

    operator func !(): Int64 { /* compiled code */ }

    operator func %(right: Int64): Int64 { /* compiled code */ }

    operator func &(right: Int64): Int64 { /* compiled code */ }

    operator func <<(right: Int64): Int64 { /* compiled code */ }

    operator func >>(right: Int64): Int64 { /* compiled code */ }

    operator func ^(right: Int64): Int64 { /* compiled code */ }

    operator func |(right: Int64): Int64 { /* compiled code */ }
}

extend Extend <: Integer {
    static func isSigned(): Bool { /* compiled code */ }

    operator func !(): IntNative { /* compiled code */ }

    operator func %(right: IntNative): IntNative { /* compiled code */ }

    operator func &(right: IntNative): IntNative { /* compiled code */ }

    operator func <<(right: Int64): IntNative { /* compiled code */ }

    operator func >>(right: Int64): IntNative { /* compiled code */ }

    operator func ^(right: IntNative): IntNative { /* compiled code */ }

    operator func |(right: IntNative): IntNative { /* compiled code */ }
}

extend Extend <: Integer {
    static func isSigned(): Bool { /* compiled code */ }

    operator func !(): UInt8 { /* compiled code */ }

    operator func %(right: UInt8): UInt8 { /* compiled code */ }

    operator func &(right: UInt8): UInt8 { /* compiled code */ }

    operator func <<(right: Int64): UInt8 { /* compiled code */ }

    operator func >>(right: Int64): UInt8 { /* compiled code */ }

    operator func ^(right: UInt8): UInt8 { /* compiled code */ }

    operator func |(right: UInt8): UInt8 { /* compiled code */ }
}

extend Extend <: Integer {
    static func isSigned(): Bool { /* compiled code */ }

    operator func !(): UInt16 { /* compiled code */ }

    operator func %(right: UInt16): UInt16 { /* compiled code */ }

    operator func &(right: UInt16): UInt16 { /* compiled code */ }

    operator func <<(right: Int64): UInt16 { /* compiled code */ }

    operator func >>(right: Int64): UInt16 { /* compiled code */ }

    operator func ^(right: UInt16): UInt16 { /* compiled code */ }

    operator func |(right: UInt16): UInt16 { /* compiled code */ }
}

extend Extend <: Integer {
    static func isSigned(): Bool { /* compiled code */ }

    operator func !(): UInt32 { /* compiled code */ }

    operator func %(right: UInt32): UInt32 { /* compiled code */ }

    operator func &(right: UInt32): UInt32 { /* compiled code */ }

    operator func <<(right: Int64): UInt32 { /* compiled code */ }

    operator func >>(right: Int64): UInt32 { /* compiled code */ }

    operator func ^(right: UInt32): UInt32 { /* compiled code */ }

    operator func |(right: UInt32): UInt32 { /* compiled code */ }
}

extend Extend <: Integer {
    static func isSigned(): Bool { /* compiled code */ }

    operator func !(): UInt64 { /* compiled code */ }

    operator func %(right: UInt64): UInt64 { /* compiled code */ }

    operator func &(right: UInt64): UInt64 { /* compiled code */ }

    operator func <<(right: Int64): UInt64 { /* compiled code */ }

    operator func >>(right: Int64): UInt64 { /* compiled code */ }

    operator func ^(right: UInt64): UInt64 { /* compiled code */ }

    operator func |(right: UInt64): UInt64 { /* compiled code */ }
}

extend Extend <: Integer {
    static func isSigned(): Bool { /* compiled code */ }

    operator func !(): UIntNative { /* compiled code */ }

    operator func %(right: UIntNative): UIntNative { /* compiled code */ }

    operator func &(right: UIntNative): UIntNative { /* compiled code */ }

    operator func <<(right: Int64): UIntNative { /* compiled code */ }

    operator func >>(right: Int64): UIntNative { /* compiled code */ }

    operator func ^(right: UIntNative): UIntNative { /* compiled code */ }

    operator func |(right: UIntNative): UIntNative { /* compiled code */ }
}

extend Extend <: FloatingPoint {
    static func getPI(): Float16 { /* compiled code */ }

    static func getE(): Float16 { /* compiled code */ }

    static func getNaN(): Float16 { /* compiled code */ }

    static func getInf(): Float16 { /* compiled code */ }

    static func getMinDenormal(): Float16 { /* compiled code */ }

    static func getMinNormal(): Float16 { /* compiled code */ }
}

interface FloatingPoint <: std.math.Number<T> {
    static func getPI(): T { /* compiled code */ }

    static func getE(): T { /* compiled code */ }

    static func getNaN(): T { /* compiled code */ }

    static func getInf(): T { /* compiled code */ }

    static func getMinDenormal(): T { /* compiled code */ }

    static func getMinNormal(): T { /* compiled code */ }

    func isInf(): Bool { /* compiled code */ }

    func isNaN(): Bool { /* compiled code */ }

    func isNormal(): Bool { /* compiled code */ }
}

extend Extend <: FloatingPoint {
    static func getPI(): Float32 { /* compiled code */ }

    static func getE(): Float32 { /* compiled code */ }

    static func getNaN(): Float32 { /* compiled code */ }

    static func getInf(): Float32 { /* compiled code */ }

    static func getMinDenormal(): Float32 { /* compiled code */ }

    static func getMinNormal(): Float32 { /* compiled code */ }
}

extend Extend <: FloatingPoint {
    static func getPI(): Float64 { /* compiled code */ }

    static func getE(): Float64 { /* compiled code */ }

    static func getNaN(): Float64 { /* compiled code */ }

    static func getInf(): Float64 { /* compiled code */ }

    static func getMinDenormal(): Float64 { /* compiled code */ }

    static func getMinNormal(): Float64 { /* compiled code */ }
}

func cbrt(x: Float64): Float64 { /* compiled code */ }

func cbrt(x: Float32): Float32 { /* compiled code */ }

func cbrt(x: Float16): Float16 { /* compiled code */ }

func logBase(x: Float32, base: Float32): Float32 { /* compiled code */ }

func logBase(x: Float64, base: Float64): Float64 { /* compiled code */ }

func logBase(x: Float16, base: Float16): Float16 { /* compiled code */ }

func gamma(x: Float64): Float64 { /* compiled code */ }

func gamma(x: Float32): Float32 { /* compiled code */ }

func gamma(x: Float16): Float16 { /* compiled code */ }

func erf(x: Float64): Float64 { /* compiled code */ }

func erf(x: Float32): Float32 { /* compiled code */ }

func erf(x: Float16): Float16 { /* compiled code */ }

func tan(x: Float64): Float64 { /* compiled code */ }

func tan(x: Float32): Float32 { /* compiled code */ }

func tan(x: Float16): Float16 { /* compiled code */ }

func asin(x: Float64): Float64 { /* compiled code */ }

func asin(x: Float32): Float32 { /* compiled code */ }

func asin(x: Float16): Float16 { /* compiled code */ }

func acos(x: Float64): Float64 { /* compiled code */ }

func acos(x: Float32): Float32 { /* compiled code */ }

func acos(x: Float16): Float16 { /* compiled code */ }

func atan(x: Float64): Float64 { /* compiled code */ }

func atan(x: Float32): Float32 { /* compiled code */ }

func atan(x: Float16): Float16 { /* compiled code */ }

func sinh(x: Float64): Float64 { /* compiled code */ }

func sinh(x: Float32): Float32 { /* compiled code */ }

func sinh(x: Float16): Float16 { /* compiled code */ }

func cosh(x: Float64): Float64 { /* compiled code */ }

func cosh(x: Float32): Float32 { /* compiled code */ }

func cosh(x: Float16): Float16 { /* compiled code */ }

func tanh(x: Float64): Float64 { /* compiled code */ }

func tanh(x: Float32): Float32 { /* compiled code */ }

func tanh(x: Float16): Float16 { /* compiled code */ }

func asinh(x: Float64): Float64 { /* compiled code */ }

func asinh(x: Float32): Float32 { /* compiled code */ }

func asinh(x: Float16): Float16 { /* compiled code */ }

func acosh(x: Float64): Float64 { /* compiled code */ }

func acosh(x: Float32): Float32 { /* compiled code */ }

func acosh(x: Float16): Float16 { /* compiled code */ }

func atanh(x: Float64): Float64 { /* compiled code */ }

func atanh(x: Float32): Float32 { /* compiled code */ }

func atanh(x: Float16): Float16 { /* compiled code */ }

var uint8len: std.core.Array<Int64>

var poptab: std.core.Array<Int64>

var revtab: std.core.Array<UInt8>

func leadingZeros(x: UInt64): Int64 { /* compiled code */ }

func leadingZeros(x: UInt32): Int64 { /* compiled code */ }

func leadingZeros(x: UInt16): Int64 { /* compiled code */ }

func leadingZeros(x: UInt8): Int64 { /* compiled code */ }

func leadingZeros(x: Int8): Int64 { /* compiled code */ }

func leadingZeros(x: Int16): Int64 { /* compiled code */ }

func leadingZeros(x: Int32): Int64 { /* compiled code */ }

func leadingZeros(x: Int64): Int64 { /* compiled code */ }

func trailingZeros(x: UInt64): Int64 { /* compiled code */ }

func trailingZeros(x: UInt32): Int64 { /* compiled code */ }

func trailingZeros(x: UInt16): Int64 { /* compiled code */ }

func trailingZeros(x: UInt8): Int64 { /* compiled code */ }

func trailingZeros(x: Int8): Int64 { /* compiled code */ }

func trailingZeros(x: Int16): Int64 { /* compiled code */ }

func trailingZeros(x: Int32): Int64 { /* compiled code */ }

func trailingZeros(x: Int64): Int64 { /* compiled code */ }

func countOnes(x: UInt64): Int64 { /* compiled code */ }

func countOnes(x: UInt32): Int64 { /* compiled code */ }

func countOnes(x: UInt16): Int64 { /* compiled code */ }

func countOnes(x: UInt8): Int64 { /* compiled code */ }

func countOnes(x: Int8): Int64 { /* compiled code */ }

func countOnes(x: Int16): Int64 { /* compiled code */ }

func countOnes(x: Int32): Int64 { /* compiled code */ }

func countOnes(x: Int64): Int64 { /* compiled code */ }

func countOne(x: Int8): Int8 { /* compiled code */ }

func countOne(x: Int16): Int8 { /* compiled code */ }

func countOne(x: Int32): Int8 { /* compiled code */ }

func countOne(x: Int64): Int8 { /* compiled code */ }

func countOne(x: UInt8): Int8 { /* compiled code */ }

func countOne(x: UInt16): Int8 { /* compiled code */ }

func countOne(x: UInt32): Int8 { /* compiled code */ }

func countOne(x: UInt64): Int8 { /* compiled code */ }

func reverse(x: UInt64): UInt64 { /* compiled code */ }

func reverse(x: UInt32): UInt32 { /* compiled code */ }

func reverse(x: UInt16): UInt16 { /* compiled code */ }

func reverse(x: UInt8): UInt8 { /* compiled code */ }

func checkedAbs(x: Int8): std.core.Option<Int8> { /* compiled code */ }

func checkedAbs(x: Int16): std.core.Option<Int16> { /* compiled code */ }

func checkedAbs(x: Int32): std.core.Option<Int32> { /* compiled code */ }

func checkedAbs(x: Int64): std.core.Option<Int64> { /* compiled code */ }

func rotate(num: UInt8, d: Int8): UInt8 { /* compiled code */ }

func rotate(num: UInt16, d: Int8): UInt16 { /* compiled code */ }

func rotate(num: UInt32, d: Int8): UInt32 { /* compiled code */ }

func rotate(num: UInt64, d: Int8): UInt64 { /* compiled code */ }

func rotate(num: Int8, d: Int8): Int8 { /* compiled code */ }

func rotate(num: Int16, d: Int8): Int16 { /* compiled code */ }

func rotate(num: Int32, d: Int8): Int32 { /* compiled code */ }

func rotate(num: Int64, d: Int8): Int64 { /* compiled code */ }

func gcd(x: Int8, y: Int8): Int8 { /* compiled code */ }

func gcd(x: Int16, y: Int16): Int16 { /* compiled code */ }

func gcd(x: Int32, y: Int32): Int32 { /* compiled code */ }

func gcd(x: Int64, y: Int64): Int64 { /* compiled code */ }

func gcd(x: UInt8, y: UInt8): UInt8 { /* compiled code */ }

func gcd(x: UInt16, y: UInt16): UInt16 { /* compiled code */ }

func gcd(x: UInt32, y: UInt32): UInt32 { /* compiled code */ }

func gcd(x: UInt64, y: UInt64): UInt64 { /* compiled code */ }

func lcm(x: Int8, y: Int8): Int8 { /* compiled code */ }

func lcm(x: Int16, y: Int16): Int16 { /* compiled code */ }

func lcm(x: Int32, y: Int32): Int32 { /* compiled code */ }

func lcm(x: Int64, y: Int64): Int64 { /* compiled code */ }

func lcm(x: UInt8, y: UInt8): UInt8 { /* compiled code */ }

func lcm(x: UInt16, y: UInt16): UInt16 { /* compiled code */ }

func lcm(x: UInt32, y: UInt32): UInt32 { /* compiled code */ }

func lcm(x: UInt64, y: UInt64): UInt64 { /* compiled code */ }

func clamp(v: Float16, min: Float16, max: Float16): Float16 { /* compiled code */ }

func clamp(v: Float32, min: Float32, max: Float32): Float32 { /* compiled code */ }

func clamp(v: Float64, min: Float64, max: Float64): Float64 { /* compiled code */ }

func round(x: Float64): Float64 { /* compiled code */ }

func round(x: Float32): Float32 { /* compiled code */ }

func round(x: Float16): Float16 { /* compiled code */ }

func atan2(y: Float64, x: Float64): Float64 { /* compiled code */ }

func atan2(y: Float32, x: Float32): Float32 { /* compiled code */ }

func atan2(y: Float16, x: Float16): Float16 { /* compiled code */ }

func fmod(x: Float64, y: Float64): Float64 { /* compiled code */ }

func fmod(x: Float32, y: Float32): Float32 { /* compiled code */ }

func fmod(x: Float16, y: Float16): Float16 { /* compiled code */ }

func isEqual(a: Float16, b: Float16): Bool { /* compiled code */ }

func isEqual(a: Float32, b: Float32): Bool { /* compiled code */ }

func isEqual(a: Float64, b: Float64): Bool { /* compiled code */ }

foreign func CJ_MATH_Tan(x: Float64): Float64

foreign func CJ_MATH_Tanf(x: Float32): Float32

foreign func CJ_MATH_Asin(x: Float64): Float64

foreign func CJ_MATH_Asinf(x: Float32): Float32

foreign func CJ_MATH_Acos(x: Float64): Float64

foreign func CJ_MATH_Acosf(x: Float32): Float32

foreign func CJ_MATH_Atan(x: Float64): Float64

foreign func CJ_MATH_Atanf(x: Float32): Float32

foreign func CJ_MATH_Atan2(y: Float64, x: Float64): Float64

foreign func CJ_MATH_Atan2f(y: Float32, x: Float32): Float32

foreign func CJ_MATH_Sinh(x: Float64): Float64

foreign func CJ_MATH_Sinhf(x: Float32): Float32

foreign func CJ_MATH_Cosh(x: Float64): Float64

foreign func CJ_MATH_Coshf(x: Float32): Float32

foreign func CJ_MATH_Tanh(x: Float64): Float64

foreign func CJ_MATH_Tanhf(x: Float32): Float32

foreign func CJ_MATH_Asinh(x: Float64): Float64

foreign func CJ_MATH_Asinhf(x: Float32): Float32

foreign func CJ_MATH_Acosh(x: Float64): Float64

foreign func CJ_MATH_Acoshf(x: Float32): Float32

foreign func CJ_MATH_Atanh(x: Float64): Float64

foreign func CJ_MATH_Atanhf(x: Float32): Float32

foreign func CJ_MATH_Cbrt(x: Float64): Float64

foreign func CJ_MATH_Cbrtf(x: Float32): Float32

foreign func CJ_MATH_Tgamma(x: Float64): Float64

foreign func CJ_MATH_Tgammaf(x: Float32): Float32

foreign func CJ_MATH_Erf(x: Float64): Float64

foreign func CJ_MATH_Erff(x: Float32): Float32

foreign func CJ_MATH_Fmod(x: Float64, y: Float64): Float64

foreign func CJ_MATH_Fmodf(x: Float32, y: Float32): Float32

func intrinsicRound(x: T): T { /* compiled code */ }
