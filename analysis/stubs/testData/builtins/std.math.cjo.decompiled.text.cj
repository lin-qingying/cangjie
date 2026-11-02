// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.math

import std.core.*

@Frozen
public func abs(x: Float64): Float64 { /* compiled code */ }

@Intrinsic
internal func intrinsicFabs<T>(x: T): T

@Frozen
public func abs(x: Float32): Float32 { /* compiled code */ }

@Frozen
public func abs(x: Float16): Float16 { /* compiled code */ }

@Frozen
public func abs(x: Int64): Int64 { /* compiled code */ }

@Intrinsic
internal func intrinsicAbs<T>(x: T): T

@Frozen
public func abs(x: Int32): Int32 { /* compiled code */ }

@Frozen
public func abs(x: Int16): Int16 { /* compiled code */ }

@Frozen
public func abs(x: Int8): Int8 { /* compiled code */ }

@Frozen
public func sqrt(x: Float16): Float16 { /* compiled code */ }

@Intrinsic
internal func intrinsicSqrt<T>(x: T): T

@Frozen
public func sqrt(x: Float32): Float32 { /* compiled code */ }

@Frozen
public func sqrt(x: Float64): Float64 { /* compiled code */ }

@Frozen
public func exp(x: Float64): Float64 { /* compiled code */ }

@Intrinsic
internal func intrinsicExp<T>(x: T): T

@Frozen
public func exp(x: Float32): Float32 { /* compiled code */ }

@Frozen
public func exp(x: Float16): Float16 { /* compiled code */ }

@Frozen
public func exp2(x: Float64): Float64 { /* compiled code */ }

@Intrinsic
internal func intrinsicExp2<T>(x: T): T

@Frozen
public func exp2(x: Float32): Float32 { /* compiled code */ }

@Frozen
public func exp2(x: Float16): Float16 { /* compiled code */ }

@Frozen
public func log10(x: Float64): Float64 { /* compiled code */ }

@Intrinsic
internal func intrinsicLog10<T>(x: T): T

@Frozen
public func log10(x: Float32): Float32 { /* compiled code */ }

@Frozen
public func log10(x: Float16): Float16 { /* compiled code */ }

@Frozen
public func log2(x: Float64): Float64 { /* compiled code */ }

@Intrinsic
internal func intrinsicLog2<T>(x: T): T

@Frozen
public func log2(x: Float32): Float32 { /* compiled code */ }

@Frozen
public func log2(x: Float16): Float16 { /* compiled code */ }

@Frozen
public func log(x: Float64): Float64 { /* compiled code */ }

@Intrinsic
internal func intrinsicLog<T>(x: T): T

@Frozen
public func log(x: Float32): Float32 { /* compiled code */ }

@Frozen
public func log(x: Float16): Float16 { /* compiled code */ }

@Frozen
public func floor(x: Float64): Float64 { /* compiled code */ }

@Intrinsic
internal func intrinsicFloor<T>(x: T): T

@Frozen
public func floor(x: Float32): Float32 { /* compiled code */ }

@Frozen
public func floor(x: Float16): Float16 { /* compiled code */ }

@Frozen
public func ceil(x: Float64): Float64 { /* compiled code */ }

@Intrinsic
internal func intrinsicCeil<T>(x: T): T

@Frozen
public func ceil(x: Float32): Float32 { /* compiled code */ }

@Frozen
public func ceil(x: Float16): Float16 { /* compiled code */ }

@Frozen
public func trunc(x: Float64): Float64 { /* compiled code */ }

@Intrinsic
internal func intrinsicTrunc<T>(x: T): T

@Frozen
public func trunc(x: Float32): Float32 { /* compiled code */ }

@Frozen
public func trunc(x: Float16): Float16 { /* compiled code */ }

@Frozen
public func sin(x: Float64): Float64 { /* compiled code */ }

@Intrinsic
internal func intrinsicSin<T>(x: T): T

@Frozen
public func sin(x: Float32): Float32 { /* compiled code */ }

@Frozen
public func sin(x: Float16): Float16 { /* compiled code */ }

@Frozen
public func cos(x: Float64): Float64 { /* compiled code */ }

@Intrinsic
internal func intrinsicCos<T>(x: T): T

@Frozen
public func cos(x: Float32): Float32 { /* compiled code */ }

@Frozen
public func cos(x: Float16): Float16 { /* compiled code */ }

@Frozen
public func pow(base: Float64, exponent: Float64): Float64 { /* compiled code */ }

@Intrinsic
internal func intrinsicPow<T>(x: T, y: T): T

@Frozen
public func pow(base: Float32, exponent: Float32): Float32 { /* compiled code */ }

@Frozen
public func pow(base: Float64, exponent: Int64): Float64 { /* compiled code */ }

internal foreign func CJ_CORE_FastPowerDoubleInt64(x: Float64, y: Int64): Float64

@Frozen
public func pow(base: Float32, exponent: Int32): Float32 { /* compiled code */ }

@Intrinsic
internal func intrinsicPowi(x: Float32, y: Int32): Float32

public enum RoundingMode <: std.core.Equatable<std.math.RoundingMode> & std.core.ToString {
    | Ceiling

    | Down

    | Floor

    | HalfEven

    | HalfUp

    | Up

    public operator func ==(that: std.math.RoundingMode): Bool { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

extend Float64 <: MathExtension {
    @Deprecated[message: "Use `public func getPI()` instead."]
    public static func GetPI(): Float64 { /* compiled code */ }

    @Deprecated[message: "Use `public func getE()` instead."]
    public static func GetE(): Float64 { /* compiled code */ }
}

@Deprecated[message: "Use `public interface FloatingPoint<T>` instead."]
public interface MathExtension<T> {
    public static func GetPI(): T

    public static func GetE(): T
}

extend Float32 <: MathExtension {
    @Deprecated[message: "Use `public func getPI()` instead."]
    public static func GetPI(): Float32 { /* compiled code */ }

    @Deprecated[message: "Use `public func getE()` instead."]
    public static func GetE(): Float32 { /* compiled code */ }
}

extend Float16 <: MathExtension {
    @Deprecated[message: "Use `public func getPI()` instead."]
    public static func GetPI(): Float16 { /* compiled code */ }

    @Deprecated[message: "Use `public func getE()` instead."]
    public static func GetE(): Float16 { /* compiled code */ }
}

extend Int8 <: MaxMinValue {
    public static func getMax(): Int8 { /* compiled code */ }

    public static func getMin(): Int8 { /* compiled code */ }
}

public interface MaxMinValue<T> {
    public static func getMax(): T

    public static func getMin(): T
}

extend Int16 <: MaxMinValue {
    public static func getMax(): Int16 { /* compiled code */ }

    public static func getMin(): Int16 { /* compiled code */ }
}

extend Int32 <: MaxMinValue {
    public static func getMax(): Int32 { /* compiled code */ }

    public static func getMin(): Int32 { /* compiled code */ }
}

extend Int64 <: MaxMinValue {
    public static func getMax(): Int64 { /* compiled code */ }

    public static func getMin(): Int64 { /* compiled code */ }
}

extend IntNative <: MaxMinValue {
    public static func getMax(): IntNative { /* compiled code */ }

    public static func getMin(): IntNative { /* compiled code */ }
}

extend UInt8 <: MaxMinValue {
    public static func getMax(): UInt8 { /* compiled code */ }

    public static func getMin(): UInt8 { /* compiled code */ }
}

extend UInt16 <: MaxMinValue {
    public static func getMax(): UInt16 { /* compiled code */ }

    public static func getMin(): UInt16 { /* compiled code */ }
}

extend UInt32 <: MaxMinValue {
    public static func getMax(): UInt32 { /* compiled code */ }

    public static func getMin(): UInt32 { /* compiled code */ }
}

extend UInt64 <: MaxMinValue {
    public static func getMax(): UInt64 { /* compiled code */ }

    public static func getMin(): UInt64 { /* compiled code */ }
}

extend UIntNative <: MaxMinValue {
    public static func getMax(): UIntNative { /* compiled code */ }

    public static func getMin(): UIntNative { /* compiled code */ }
}

extend Float16 <: MaxMinValue {
    public static func getMax(): Float16 { /* compiled code */ }

    public static func getMin(): Float16 { /* compiled code */ }
}

extend Float32 <: MaxMinValue {
    public static func getMax(): Float32 { /* compiled code */ }

    public static func getMin(): Float32 { /* compiled code */ }
}

extend Float64 <: MaxMinValue {
    public static func getMax(): Float64 { /* compiled code */ }

    public static func getMin(): Float64 { /* compiled code */ }
}

extend Int8 <: Number {
    public operator func *(right: Int8): Int8 { /* compiled code */ }

    public operator func +(right: Int8): Int8 { /* compiled code */ }

    public operator func -(right: Int8): Int8 { /* compiled code */ }

    public operator func -(): Int8 { /* compiled code */ }

    public operator func /(right: Int8): Int8 { /* compiled code */ }
}

public interface Number<T> {
    public open operator func +(rhs: T): T

    public open operator func -(rhs: T): T

    public open operator func *(rhs: T): T

    public open operator func /(rhs: T): T

    public open operator func -(): T
}

extend Int16 <: Number {
    public operator func *(right: Int16): Int16 { /* compiled code */ }

    public operator func +(right: Int16): Int16 { /* compiled code */ }

    public operator func -(right: Int16): Int16 { /* compiled code */ }

    public operator func -(): Int16 { /* compiled code */ }

    public operator func /(right: Int16): Int16 { /* compiled code */ }
}

extend Int32 <: Number {
    public operator func *(right: Int32): Int32 { /* compiled code */ }

    public operator func +(right: Int32): Int32 { /* compiled code */ }

    public operator func -(right: Int32): Int32 { /* compiled code */ }

    public operator func -(): Int32 { /* compiled code */ }

    public operator func /(right: Int32): Int32 { /* compiled code */ }
}

extend Int64 <: Number {
    public operator func *(right: Int64): Int64 { /* compiled code */ }

    public operator func +(right: Int64): Int64 { /* compiled code */ }

    public operator func -(right: Int64): Int64 { /* compiled code */ }

    public operator func -(): Int64 { /* compiled code */ }

    public operator func /(right: Int64): Int64 { /* compiled code */ }
}

extend IntNative <: Number {
    public operator func *(right: IntNative): IntNative { /* compiled code */ }

    public operator func +(right: IntNative): IntNative { /* compiled code */ }

    public operator func -(right: IntNative): IntNative { /* compiled code */ }

    public operator func -(): IntNative { /* compiled code */ }

    public operator func /(right: IntNative): IntNative { /* compiled code */ }
}

extend UInt8 <: Number {
    public operator func *(right: UInt8): UInt8 { /* compiled code */ }

    public operator func +(right: UInt8): UInt8 { /* compiled code */ }

    public operator func -(right: UInt8): UInt8 { /* compiled code */ }

    public operator func -(): UInt8 { /* compiled code */ }

    public operator func /(right: UInt8): UInt8 { /* compiled code */ }
}

extend UInt16 <: Number {
    public operator func *(right: UInt16): UInt16 { /* compiled code */ }

    public operator func +(right: UInt16): UInt16 { /* compiled code */ }

    public operator func -(right: UInt16): UInt16 { /* compiled code */ }

    public operator func -(): UInt16 { /* compiled code */ }

    public operator func /(right: UInt16): UInt16 { /* compiled code */ }
}

extend UInt32 <: Number {
    public operator func *(right: UInt32): UInt32 { /* compiled code */ }

    public operator func +(right: UInt32): UInt32 { /* compiled code */ }

    public operator func -(right: UInt32): UInt32 { /* compiled code */ }

    public operator func -(): UInt32 { /* compiled code */ }

    public operator func /(right: UInt32): UInt32 { /* compiled code */ }
}

extend UInt64 <: Number {
    public operator func *(right: UInt64): UInt64 { /* compiled code */ }

    public operator func +(right: UInt64): UInt64 { /* compiled code */ }

    public operator func -(right: UInt64): UInt64 { /* compiled code */ }

    public operator func -(): UInt64 { /* compiled code */ }

    public operator func /(right: UInt64): UInt64 { /* compiled code */ }
}

extend UIntNative <: Number {
    public operator func *(right: UIntNative): UIntNative { /* compiled code */ }

    public operator func +(right: UIntNative): UIntNative { /* compiled code */ }

    public operator func -(right: UIntNative): UIntNative { /* compiled code */ }

    public operator func -(): UIntNative { /* compiled code */ }

    public operator func /(right: UIntNative): UIntNative { /* compiled code */ }
}

extend Float16 <: Number {
    public operator func *(right: Float16): Float16 { /* compiled code */ }

    public operator func +(right: Float16): Float16 { /* compiled code */ }

    public operator func -(right: Float16): Float16 { /* compiled code */ }

    public operator func -(): Float16 { /* compiled code */ }

    public operator func /(right: Float16): Float16 { /* compiled code */ }
}

extend Float32 <: Number {
    public operator func *(right: Float32): Float32 { /* compiled code */ }

    public operator func +(right: Float32): Float32 { /* compiled code */ }

    public operator func -(right: Float32): Float32 { /* compiled code */ }

    public operator func -(): Float32 { /* compiled code */ }

    public operator func /(right: Float32): Float32 { /* compiled code */ }
}

extend Float64 <: Number {
    public operator func *(right: Float64): Float64 { /* compiled code */ }

    public operator func +(right: Float64): Float64 { /* compiled code */ }

    public operator func -(right: Float64): Float64 { /* compiled code */ }

    public operator func -(): Float64 { /* compiled code */ }

    public operator func /(right: Float64): Float64 { /* compiled code */ }
}

extend Int8 <: Integer {
    public static func isSigned(): Bool { /* compiled code */ }

    public operator func !(): Int8 { /* compiled code */ }

    public operator func %(right: Int8): Int8 { /* compiled code */ }

    public operator func &(right: Int8): Int8 { /* compiled code */ }

    public operator func <<(right: Int64): Int8 { /* compiled code */ }

    public operator func >>(right: Int64): Int8 { /* compiled code */ }

    public operator func ^(right: Int8): Int8 { /* compiled code */ }

    public operator func |(right: Int8): Int8 { /* compiled code */ }
}

public interface Integer<T> <: std.math.Number<T> {
    public static func isSigned(): Bool

    public open operator func %(rhs: T): T

    public open operator func &(rhs: T): T

    public open operator func |(rhs: T): T

    public open operator func ^(rhs: T): T

    public open operator func !(): T

    public open operator func >>(n: Int64): T

    public open operator func <<(n: Int64): T
}

extend Int16 <: Integer {
    public static func isSigned(): Bool { /* compiled code */ }

    public operator func !(): Int16 { /* compiled code */ }

    public operator func %(right: Int16): Int16 { /* compiled code */ }

    public operator func &(right: Int16): Int16 { /* compiled code */ }

    public operator func <<(right: Int64): Int16 { /* compiled code */ }

    public operator func >>(right: Int64): Int16 { /* compiled code */ }

    public operator func ^(right: Int16): Int16 { /* compiled code */ }

    public operator func |(right: Int16): Int16 { /* compiled code */ }
}

extend Int32 <: Integer {
    public static func isSigned(): Bool { /* compiled code */ }

    public operator func !(): Int32 { /* compiled code */ }

    public operator func %(right: Int32): Int32 { /* compiled code */ }

    public operator func &(right: Int32): Int32 { /* compiled code */ }

    public operator func <<(right: Int64): Int32 { /* compiled code */ }

    public operator func >>(right: Int64): Int32 { /* compiled code */ }

    public operator func ^(right: Int32): Int32 { /* compiled code */ }

    public operator func |(right: Int32): Int32 { /* compiled code */ }
}

extend Int64 <: Integer {
    public static func isSigned(): Bool { /* compiled code */ }

    public operator func !(): Int64 { /* compiled code */ }

    public operator func %(right: Int64): Int64 { /* compiled code */ }

    public operator func &(right: Int64): Int64 { /* compiled code */ }

    public operator func <<(right: Int64): Int64 { /* compiled code */ }

    public operator func >>(right: Int64): Int64 { /* compiled code */ }

    public operator func ^(right: Int64): Int64 { /* compiled code */ }

    public operator func |(right: Int64): Int64 { /* compiled code */ }
}

extend IntNative <: Integer {
    public static func isSigned(): Bool { /* compiled code */ }

    public operator func !(): IntNative { /* compiled code */ }

    public operator func %(right: IntNative): IntNative { /* compiled code */ }

    public operator func &(right: IntNative): IntNative { /* compiled code */ }

    public operator func <<(right: Int64): IntNative { /* compiled code */ }

    public operator func >>(right: Int64): IntNative { /* compiled code */ }

    public operator func ^(right: IntNative): IntNative { /* compiled code */ }

    public operator func |(right: IntNative): IntNative { /* compiled code */ }
}

extend UInt8 <: Integer {
    public static func isSigned(): Bool { /* compiled code */ }

    public operator func !(): UInt8 { /* compiled code */ }

    public operator func %(right: UInt8): UInt8 { /* compiled code */ }

    public operator func &(right: UInt8): UInt8 { /* compiled code */ }

    public operator func <<(right: Int64): UInt8 { /* compiled code */ }

    public operator func >>(right: Int64): UInt8 { /* compiled code */ }

    public operator func ^(right: UInt8): UInt8 { /* compiled code */ }

    public operator func |(right: UInt8): UInt8 { /* compiled code */ }
}

extend UInt16 <: Integer {
    public static func isSigned(): Bool { /* compiled code */ }

    public operator func !(): UInt16 { /* compiled code */ }

    public operator func %(right: UInt16): UInt16 { /* compiled code */ }

    public operator func &(right: UInt16): UInt16 { /* compiled code */ }

    public operator func <<(right: Int64): UInt16 { /* compiled code */ }

    public operator func >>(right: Int64): UInt16 { /* compiled code */ }

    public operator func ^(right: UInt16): UInt16 { /* compiled code */ }

    public operator func |(right: UInt16): UInt16 { /* compiled code */ }
}

extend UInt32 <: Integer {
    public static func isSigned(): Bool { /* compiled code */ }

    public operator func !(): UInt32 { /* compiled code */ }

    public operator func %(right: UInt32): UInt32 { /* compiled code */ }

    public operator func &(right: UInt32): UInt32 { /* compiled code */ }

    public operator func <<(right: Int64): UInt32 { /* compiled code */ }

    public operator func >>(right: Int64): UInt32 { /* compiled code */ }

    public operator func ^(right: UInt32): UInt32 { /* compiled code */ }

    public operator func |(right: UInt32): UInt32 { /* compiled code */ }
}

extend UInt64 <: Integer {
    public static func isSigned(): Bool { /* compiled code */ }

    public operator func !(): UInt64 { /* compiled code */ }

    public operator func %(right: UInt64): UInt64 { /* compiled code */ }

    public operator func &(right: UInt64): UInt64 { /* compiled code */ }

    public operator func <<(right: Int64): UInt64 { /* compiled code */ }

    public operator func >>(right: Int64): UInt64 { /* compiled code */ }

    public operator func ^(right: UInt64): UInt64 { /* compiled code */ }

    public operator func |(right: UInt64): UInt64 { /* compiled code */ }
}

extend UIntNative <: Integer {
    public static func isSigned(): Bool { /* compiled code */ }

    public operator func !(): UIntNative { /* compiled code */ }

    public operator func %(right: UIntNative): UIntNative { /* compiled code */ }

    public operator func &(right: UIntNative): UIntNative { /* compiled code */ }

    public operator func <<(right: Int64): UIntNative { /* compiled code */ }

    public operator func >>(right: Int64): UIntNative { /* compiled code */ }

    public operator func ^(right: UIntNative): UIntNative { /* compiled code */ }

    public operator func |(right: UIntNative): UIntNative { /* compiled code */ }
}

extend Float16 <: FloatingPoint {
    public static func getPI(): Float16 { /* compiled code */ }

    public static func getE(): Float16 { /* compiled code */ }

    public static func getNaN(): Float16 { /* compiled code */ }

    public static func getInf(): Float16 { /* compiled code */ }

    public static func getMinDenormal(): Float16 { /* compiled code */ }

    public static func getMinNormal(): Float16 { /* compiled code */ }
}

public interface FloatingPoint<T> <: std.math.Number<T> {
    public static func getPI(): T

    public static func getE(): T

    public static func getNaN(): T

    public static func getInf(): T

    public static func getMinDenormal(): T

    public static func getMinNormal(): T

    public open func isInf(): Bool

    public open func isNaN(): Bool

    public open func isNormal(): Bool
}

extend Float32 <: FloatingPoint {
    public static func getPI(): Float32 { /* compiled code */ }

    public static func getE(): Float32 { /* compiled code */ }

    public static func getNaN(): Float32 { /* compiled code */ }

    public static func getInf(): Float32 { /* compiled code */ }

    public static func getMinDenormal(): Float32 { /* compiled code */ }

    public static func getMinNormal(): Float32 { /* compiled code */ }
}

extend Float64 <: FloatingPoint {
    public static func getPI(): Float64 { /* compiled code */ }

    public static func getE(): Float64 { /* compiled code */ }

    public static func getNaN(): Float64 { /* compiled code */ }

    public static func getInf(): Float64 { /* compiled code */ }

    public static func getMinDenormal(): Float64 { /* compiled code */ }

    public static func getMinNormal(): Float64 { /* compiled code */ }
}

public func cbrt(x: Float64): Float64 { /* compiled code */ }

public func cbrt(x: Float32): Float32 { /* compiled code */ }

public func cbrt(x: Float16): Float16 { /* compiled code */ }

public func logBase(x: Float32, base: Float32): Float32 { /* compiled code */ }

public func logBase(x: Float64, base: Float64): Float64 { /* compiled code */ }

public func logBase(x: Float16, base: Float16): Float16 { /* compiled code */ }

public func gamma(x: Float64): Float64 { /* compiled code */ }

public func gamma(x: Float32): Float32 { /* compiled code */ }

public func gamma(x: Float16): Float16 { /* compiled code */ }

public func erf(x: Float64): Float64 { /* compiled code */ }

public func erf(x: Float32): Float32 { /* compiled code */ }

public func erf(x: Float16): Float16 { /* compiled code */ }

public func tan(x: Float64): Float64 { /* compiled code */ }

public func tan(x: Float32): Float32 { /* compiled code */ }

public func tan(x: Float16): Float16 { /* compiled code */ }

public func asin(x: Float64): Float64 { /* compiled code */ }

public func asin(x: Float32): Float32 { /* compiled code */ }

public func asin(x: Float16): Float16 { /* compiled code */ }

public func acos(x: Float64): Float64 { /* compiled code */ }

public func acos(x: Float32): Float32 { /* compiled code */ }

public func acos(x: Float16): Float16 { /* compiled code */ }

public func atan(x: Float64): Float64 { /* compiled code */ }

public func atan(x: Float32): Float32 { /* compiled code */ }

public func atan(x: Float16): Float16 { /* compiled code */ }

public func sinh(x: Float64): Float64 { /* compiled code */ }

public func sinh(x: Float32): Float32 { /* compiled code */ }

public func sinh(x: Float16): Float16 { /* compiled code */ }

public func cosh(x: Float64): Float64 { /* compiled code */ }

public func cosh(x: Float32): Float32 { /* compiled code */ }

public func cosh(x: Float16): Float16 { /* compiled code */ }

public func tanh(x: Float64): Float64 { /* compiled code */ }

public func tanh(x: Float32): Float32 { /* compiled code */ }

public func tanh(x: Float16): Float16 { /* compiled code */ }

public func asinh(x: Float64): Float64 { /* compiled code */ }

public func asinh(x: Float32): Float32 { /* compiled code */ }

public func asinh(x: Float16): Float16 { /* compiled code */ }

public func acosh(x: Float64): Float64 { /* compiled code */ }

public func acosh(x: Float32): Float32 { /* compiled code */ }

public func acosh(x: Float16): Float16 { /* compiled code */ }

public func atanh(x: Float64): Float64 { /* compiled code */ }

public func atanh(x: Float32): Float32 { /* compiled code */ }

public func atanh(x: Float16): Float16 { /* compiled code */ }

var uint8len: std.core.Array<Int64>

var poptab: std.core.Array<Int64>

var revtab: std.core.Array<UInt8>

public func leadingZeros(x: UInt64): Int64 { /* compiled code */ }

public func leadingZeros(x: UInt32): Int64 { /* compiled code */ }

public func leadingZeros(x: UInt16): Int64 { /* compiled code */ }

public func leadingZeros(x: UInt8): Int64 { /* compiled code */ }

public func leadingZeros(x: Int8): Int64 { /* compiled code */ }

public func leadingZeros(x: Int16): Int64 { /* compiled code */ }

public func leadingZeros(x: Int32): Int64 { /* compiled code */ }

public func leadingZeros(x: Int64): Int64 { /* compiled code */ }

public func trailingZeros(x: UInt64): Int64 { /* compiled code */ }

public func trailingZeros(x: UInt32): Int64 { /* compiled code */ }

public func trailingZeros(x: UInt16): Int64 { /* compiled code */ }

public func trailingZeros(x: UInt8): Int64 { /* compiled code */ }

public func trailingZeros(x: Int8): Int64 { /* compiled code */ }

public func trailingZeros(x: Int16): Int64 { /* compiled code */ }

public func trailingZeros(x: Int32): Int64 { /* compiled code */ }

public func trailingZeros(x: Int64): Int64 { /* compiled code */ }

public func countOnes(x: UInt64): Int64 { /* compiled code */ }

public func countOnes(x: UInt32): Int64 { /* compiled code */ }

public func countOnes(x: UInt16): Int64 { /* compiled code */ }

public func countOnes(x: UInt8): Int64 { /* compiled code */ }

public func countOnes(x: Int8): Int64 { /* compiled code */ }

public func countOnes(x: Int16): Int64 { /* compiled code */ }

public func countOnes(x: Int32): Int64 { /* compiled code */ }

public func countOnes(x: Int64): Int64 { /* compiled code */ }

@Deprecated[message: "Use global function `public func countOnes(x: Int8): Int64` instead."]
public func countOne(x: Int8): Int8 { /* compiled code */ }

@Deprecated[message: "Use global function `public func countOnes(x: Int16): Int64` instead."]
public func countOne(x: Int16): Int8 { /* compiled code */ }

@Deprecated[message: "Use global function `public func countOnes(x: Int32): Int64` instead."]
public func countOne(x: Int32): Int8 { /* compiled code */ }

@Deprecated[message: "Use global function `public func countOnes(x: Int64): Int64` instead."]
public func countOne(x: Int64): Int8 { /* compiled code */ }

@Deprecated[message: "Use global function `public func countOnes(x: UInt8): Int64` instead."]
public func countOne(x: UInt8): Int8 { /* compiled code */ }

@Deprecated[message: "Use global function `public func countOnes(x: UInt16): Int64` instead."]
public func countOne(x: UInt16): Int8 { /* compiled code */ }

@Deprecated[message: "Use global function `public func countOnes(x: UInt32): Int64` instead."]
public func countOne(x: UInt32): Int8 { /* compiled code */ }

@Deprecated[message: "Use global function `public func countOnes(x: UInt64): Int64` instead."]
public func countOne(x: UInt64): Int8 { /* compiled code */ }

public func reverse(x: UInt64): UInt64 { /* compiled code */ }

public func reverse(x: UInt32): UInt32 { /* compiled code */ }

public func reverse(x: UInt16): UInt16 { /* compiled code */ }

public func reverse(x: UInt8): UInt8 { /* compiled code */ }

public func checkedAbs(x: Int8): std.core.Option<Int8> { /* compiled code */ }

public func checkedAbs(x: Int16): std.core.Option<Int16> { /* compiled code */ }

public func checkedAbs(x: Int32): std.core.Option<Int32> { /* compiled code */ }

public func checkedAbs(x: Int64): std.core.Option<Int64> { /* compiled code */ }

public func rotate(num: UInt8, d: Int8): UInt8 { /* compiled code */ }

public func rotate(num: UInt16, d: Int8): UInt16 { /* compiled code */ }

public func rotate(num: UInt32, d: Int8): UInt32 { /* compiled code */ }

public func rotate(num: UInt64, d: Int8): UInt64 { /* compiled code */ }

public func rotate(num: Int8, d: Int8): Int8 { /* compiled code */ }

public func rotate(num: Int16, d: Int8): Int16 { /* compiled code */ }

public func rotate(num: Int32, d: Int8): Int32 { /* compiled code */ }

public func rotate(num: Int64, d: Int8): Int64 { /* compiled code */ }

public func gcd(x: Int8, y: Int8): Int8 { /* compiled code */ }

public func gcd(x: Int16, y: Int16): Int16 { /* compiled code */ }

public func gcd(x: Int32, y: Int32): Int32 { /* compiled code */ }

public func gcd(x: Int64, y: Int64): Int64 { /* compiled code */ }

public func gcd(x: UInt8, y: UInt8): UInt8 { /* compiled code */ }

public func gcd(x: UInt16, y: UInt16): UInt16 { /* compiled code */ }

public func gcd(x: UInt32, y: UInt32): UInt32 { /* compiled code */ }

public func gcd(x: UInt64, y: UInt64): UInt64 { /* compiled code */ }

public func lcm(x: Int8, y: Int8): Int8 { /* compiled code */ }

public func lcm(x: Int16, y: Int16): Int16 { /* compiled code */ }

public func lcm(x: Int32, y: Int32): Int32 { /* compiled code */ }

public func lcm(x: Int64, y: Int64): Int64 { /* compiled code */ }

public func lcm(x: UInt8, y: UInt8): UInt8 { /* compiled code */ }

public func lcm(x: UInt16, y: UInt16): UInt16 { /* compiled code */ }

public func lcm(x: UInt32, y: UInt32): UInt32 { /* compiled code */ }

public func lcm(x: UInt64, y: UInt64): UInt64 { /* compiled code */ }

public func clamp(v: Float16, min: Float16, max: Float16): Float16 { /* compiled code */ }

public func clamp(v: Float32, min: Float32, max: Float32): Float32 { /* compiled code */ }

public func clamp(v: Float64, min: Float64, max: Float64): Float64 { /* compiled code */ }

public func round(x: Float64): Float64 { /* compiled code */ }

public func round(x: Float32): Float32 { /* compiled code */ }

public func round(x: Float16): Float16 { /* compiled code */ }

public func atan2(y: Float64, x: Float64): Float64 { /* compiled code */ }

public func atan2(y: Float32, x: Float32): Float32 { /* compiled code */ }

public func atan2(y: Float16, x: Float16): Float16 { /* compiled code */ }

public func fmod(x: Float64, y: Float64): Float64 { /* compiled code */ }

public func fmod(x: Float32, y: Float32): Float32 { /* compiled code */ }

public func fmod(x: Float16, y: Float16): Float16 { /* compiled code */ }

internal func isEqual(a: Float16, b: Float16): Bool { /* compiled code */ }

internal func isEqual(a: Float32, b: Float32): Bool { /* compiled code */ }

internal func isEqual(a: Float64, b: Float64): Bool { /* compiled code */ }

internal foreign func CJ_MATH_Tan(x: Float64): Float64

internal foreign func CJ_MATH_Tanf(x: Float32): Float32

internal foreign func CJ_MATH_Asin(x: Float64): Float64

internal foreign func CJ_MATH_Asinf(x: Float32): Float32

internal foreign func CJ_MATH_Acos(x: Float64): Float64

internal foreign func CJ_MATH_Acosf(x: Float32): Float32

internal foreign func CJ_MATH_Atan(x: Float64): Float64

internal foreign func CJ_MATH_Atanf(x: Float32): Float32

internal foreign func CJ_MATH_Atan2(y: Float64, x: Float64): Float64

internal foreign func CJ_MATH_Atan2f(y: Float32, x: Float32): Float32

internal foreign func CJ_MATH_Sinh(x: Float64): Float64

internal foreign func CJ_MATH_Sinhf(x: Float32): Float32

internal foreign func CJ_MATH_Cosh(x: Float64): Float64

internal foreign func CJ_MATH_Coshf(x: Float32): Float32

internal foreign func CJ_MATH_Tanh(x: Float64): Float64

internal foreign func CJ_MATH_Tanhf(x: Float32): Float32

internal foreign func CJ_MATH_Asinh(x: Float64): Float64

internal foreign func CJ_MATH_Asinhf(x: Float32): Float32

internal foreign func CJ_MATH_Acosh(x: Float64): Float64

internal foreign func CJ_MATH_Acoshf(x: Float32): Float32

internal foreign func CJ_MATH_Atanh(x: Float64): Float64

internal foreign func CJ_MATH_Atanhf(x: Float32): Float32

internal foreign func CJ_MATH_Cbrt(x: Float64): Float64

internal foreign func CJ_MATH_Cbrtf(x: Float32): Float32

internal foreign func CJ_MATH_Tgamma(x: Float64): Float64

internal foreign func CJ_MATH_Tgammaf(x: Float32): Float32

internal foreign func CJ_MATH_Erf(x: Float64): Float64

internal foreign func CJ_MATH_Erff(x: Float32): Float32

internal foreign func CJ_MATH_Fmod(x: Float64, y: Float64): Float64

internal foreign func CJ_MATH_Fmodf(x: Float32, y: Float32): Float32

@Intrinsic
internal func intrinsicRound<T>(x: T): T
