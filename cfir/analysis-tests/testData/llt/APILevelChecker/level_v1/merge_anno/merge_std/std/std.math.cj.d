// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.







package std.math


@!APILevel[12, atomicservice : true]
public enum RoundingMode <: Equatable<RoundingMode> & ToString {
    @!APILevel[12, atomicservice : true]
    Ceiling |
    @!APILevel[12, atomicservice : true]
    Down |
    @!APILevel[12, atomicservice : true]
    Floor |
    @!APILevel[12, atomicservice : true]
    HalfEven |
    @!APILevel[12, atomicservice : true]
    HalfUp |
    @!APILevel[12, atomicservice : true]
    Up
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: RoundingMode): Bool
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

extend Float64 <: MathExtension<Float64> {
    @Deprecated[message: "Use `public func getPI()` instead."]
    @!APILevel[12, atomicservice : true]
    public static func GetPI(): Float64
    
    @Deprecated[message: "Use `public func getE()` instead."]
    @!APILevel[12, atomicservice : true]
    public static func GetE(): Float64
}

extend Float32 <: MathExtension<Float32> {
    @Deprecated[message: "Use `public func getPI()` instead."]
    @!APILevel[12, atomicservice : true]
    public static func GetPI(): Float32
    
    @Deprecated[message: "Use `public func getE()` instead."]
    @!APILevel[12, atomicservice : true]
    public static func GetE(): Float32
}

extend Float16 <: MathExtension<Float16> {
    @Deprecated[message: "Use `public func getPI()` instead."]
    @!APILevel[12, atomicservice : true]
    public static func GetPI(): Float16
    
    @Deprecated[message: "Use `public func getE()` instead."]
    @!APILevel[12, atomicservice : true]
    public static func GetE(): Float16
}

/******************************** extend MaxMinValue<T> *********************************/
extend Int8 <: MaxMinValue<Int8> {
    @!APILevel[12, atomicservice : true]
    public static func getMax(): Int8
    
    @!APILevel[12, atomicservice : true]
    public static func getMin(): Int8
}

extend Int16 <: MaxMinValue<Int16> {
    @!APILevel[12, atomicservice : true]
    public static func getMax(): Int16
    
    @!APILevel[12, atomicservice : true]
    public static func getMin(): Int16
}

extend Int32 <: MaxMinValue<Int32> {
    @!APILevel[12, atomicservice : true]
    public static func getMax(): Int32
    
    @!APILevel[12, atomicservice : true]
    public static func getMin(): Int32
}

extend Int64 <: MaxMinValue<Int64> {
    @!APILevel[12, atomicservice : true]
    public static func getMax(): Int64
    
    @!APILevel[12, atomicservice : true]
    public static func getMin(): Int64
}

extend IntNative <: MaxMinValue<IntNative> {
    @!APILevel[12, atomicservice : true]
    public static func getMax(): IntNative
    
    @!APILevel[12, atomicservice : true]
    public static func getMin(): IntNative
}

extend UInt8 <: MaxMinValue<UInt8> {
    @!APILevel[12, atomicservice : true]
    public static func getMax(): UInt8
    
    @!APILevel[12, atomicservice : true]
    public static func getMin(): UInt8
}

extend UInt16 <: MaxMinValue<UInt16> {
    @!APILevel[12, atomicservice : true]
    public static func getMax(): UInt16
    
    @!APILevel[12, atomicservice : true]
    public static func getMin(): UInt16
}

extend UInt32 <: MaxMinValue<UInt32> {
    @!APILevel[12, atomicservice : true]
    public static func getMax(): UInt32
    
    @!APILevel[12, atomicservice : true]
    public static func getMin(): UInt32
}

extend UInt64 <: MaxMinValue<UInt64> {
    @!APILevel[12, atomicservice : true]
    public static func getMax(): UInt64
    
    @!APILevel[12, atomicservice : true]
    public static func getMin(): UInt64
}

extend UIntNative <: MaxMinValue<UIntNative> {
    @!APILevel[12, atomicservice : true]
    public static func getMax(): UIntNative
    
    @!APILevel[12, atomicservice : true]
    public static func getMin(): UIntNative
}

extend Float16 <: MaxMinValue<Float16> {
    @!APILevel[12, atomicservice : true]
    public static func getMax(): Float16
    
    @!APILevel[12, atomicservice : true]
    public static func getMin(): Float16
}

extend Float32 <: MaxMinValue<Float32> {
    @!APILevel[12, atomicservice : true]
    public static func getMax(): Float32
    
    @!APILevel[12, atomicservice : true]
    public static func getMin(): Float32
}

extend Float64 <: MaxMinValue<Float64> {
    @!APILevel[12, atomicservice : true]
    public static func getMax(): Float64
    
    @!APILevel[12, atomicservice : true]
    public static func getMin(): Float64
}

/******************************** extend Number<T> *********************************/
extend Int8 <: Number<Int8> {
}

extend Int16 <: Number<Int16> {
}

extend Int32 <: Number<Int32> {
}

extend Int64 <: Number<Int64> {
}

extend IntNative <: Number<IntNative> {
}

extend UInt8 <: Number<UInt8> {
}

extend UInt16 <: Number<UInt16> {
}

extend UInt32 <: Number<UInt32> {
}

extend UInt64 <: Number<UInt64> {
}

extend UIntNative <: Number<UIntNative> {
}

extend Float16 <: Number<Float16> {
}

extend Float32 <: Number<Float32> {
}

extend Float64 <: Number<Float64> {
}

/******************************* extend Integer<T> *********************************/
extend Int8 <: Integer<Int8> {
    @!APILevel[12, atomicservice : true]
    public static func isSigned(): Bool
}

extend Int16 <: Integer<Int16> {
    @!APILevel[12, atomicservice : true]
    public static func isSigned(): Bool
}

extend Int32 <: Integer<Int32> {
    @!APILevel[12, atomicservice : true]
    public static func isSigned(): Bool
}

extend Int64 <: Integer<Int64> {
    @!APILevel[12, atomicservice : true]
    public static func isSigned(): Bool
}

extend IntNative <: Integer<IntNative> {
    @!APILevel[12, atomicservice : true]
    public static func isSigned(): Bool
}

extend UInt8 <: Integer<UInt8> {
    @!APILevel[12, atomicservice : true]
    public static func isSigned(): Bool
}

extend UInt16 <: Integer<UInt16> {
    @!APILevel[12, atomicservice : true]
    public static func isSigned(): Bool
}

extend UInt32 <: Integer<UInt32> {
    @!APILevel[12, atomicservice : true]
    public static func isSigned(): Bool
}

extend UInt64 <: Integer<UInt64> {
    @!APILevel[12, atomicservice : true]
    public static func isSigned(): Bool
}

extend UIntNative <: Integer<UIntNative> {
    @!APILevel[12, atomicservice : true]
    public static func isSigned(): Bool
}

/******************************** extend FloatingPoint<T> *********************************/
extend Float16 <: FloatingPoint<Float16> {
    @!APILevel[12, atomicservice : true]
    public static func getPI(): Float16
    
    @!APILevel[12, atomicservice : true]
    public static func getE(): Float16
    
    @!APILevel[12, atomicservice : true]
    public static func getNaN(): Float16
    
    @!APILevel[12, atomicservice : true]
    public static func getInf(): Float16
    
    @!APILevel[12, atomicservice : true]
    public static func getMinDenormal(): Float16
    
    @!APILevel[12, atomicservice : true]
    public static func getMinNormal(): Float16
}

extend Float32 <: FloatingPoint<Float32> {
    @!APILevel[12, atomicservice : true]
    public static func getPI(): Float32
    
    @!APILevel[12, atomicservice : true]
    public static func getE(): Float32
    
    @!APILevel[12, atomicservice : true]
    public static func getNaN(): Float32
    
    @!APILevel[12, atomicservice : true]
    public static func getInf(): Float32
    
    @!APILevel[12, atomicservice : true]
    public static func getMinDenormal(): Float32
    
    @!APILevel[12, atomicservice : true]
    public static func getMinNormal(): Float32
}

extend Float64 <: FloatingPoint<Float64> {
    @!APILevel[12, atomicservice : true]
    public static func getPI(): Float64
    
    @!APILevel[12, atomicservice : true]
    public static func getE(): Float64
    
    @!APILevel[12, atomicservice : true]
    public static func getNaN(): Float64
    
    @!APILevel[12, atomicservice : true]
    public static func getInf(): Float64
    
    @!APILevel[12, atomicservice : true]
    public static func getMinDenormal(): Float64
    
    @!APILevel[12, atomicservice : true]
    public static func getMinNormal(): Float64
}

/**
* Returns the cube root of the value.
* The cube root of a positive value is the positive number
* of the cube root of the size of the value.
* That is, the cube root of a negative value is the negative number
* of the cube root of the size of the value.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the argument is infinite, the result is infinity.
*
* @param x some value.
* @return the cube root of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func cbrt(x: Float64): Float64

/**
* Returns the cube root of the value.
* The cube root of a positive value is the positive number
* of the cube root of the size of the value.
* That is, the cube root of a negative value is the negative number
* of the cube root of the size of the value.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the argument is infinite, the result is infinity.
*
* @param x some value.
* @return the cube root of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func cbrt(x: Float32): Float32

/**
* Returns the cube root of the value.
* The cube root of a positive value is the positive number
* of the cube root of the size of the value.
* That is, the cube root of a negative value is the negative number
* of the cube root of the size of the value.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the argument is infinite, the result is infinity.
*
* @param x some value.
* @return the cube root of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func cbrt(x: Float16): Float16

/**
* Returns the x logarithm of the base of a value.
* special cases that arise:
* If the base is 2.
* @see func log2(x)
*
* If the base is 10.
* @see func log10(x)
*
* If the base is e.
* @see func log(x)
*
* @param x some value.
* @param base base value.
* @return \f$ \log_{base} {x} \f$
*
* @throws IllegalArgumentException if the argument `x` or `base` is not positive, or the `base` is 1.0
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func logBase(x: Float32, base: Float32): Float32

/**
* Returns the x logarithm of the base of a value.
* special cases that arise:
* If the base is 2.
* @see func log2(x)
*
* If the base is 10.
* @see func log10(x)
*
* If the base is e.
* @see func log(x)
*
* @param x some value.
* @param base base value.
* @return \f$ \log_{base} {x} \f$
*
* @throws IllegalArgumentException if the argument `x` or `base` is not positive, or the `base` is 1.0
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func logBase(x: Float64, base: Float64): Float64

/**
* Returns the x logarithm of the base of a value.
* special cases that arise:
* If the base is 2.
* @see func log2(x)
*
* If the base is 10.
* @see func log10(x)
*
* If the base is e.
* @see func log(x)
*
* @param x some value.
* @param base base value.
* @return \f$ \log_{base} {x} \f$
*
* @throws IllegalArgumentException if the argument `x` or `base` is not positive, or the `base` is 1.0
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func logBase(x: Float16, base: Float16): Float16

/**
* Returns the gamma function value of @p x.
*
* @param x some value.
* @return the gamma function value of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func gamma(x: Float64): Float64

/**
* Returns the gamma function value of @p x.
*
* @param x some value.
* @return the gamma function value of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func gamma(x: Float32): Float32

/**
* Returns the gamma function value of @p x.
*
* @param x some value.
* @return the gamma function value of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func gamma(x: Float16): Float16

/**
* Returns the Gauss error function of @p x.
* The related definition is: \f$ {erf} x={\frac {2}{\sqrt {\pi }}}\int _{0}^{x}e^{-t^{2}}\,dt \f$
*
* @param x some value.
* @return the Gauss error function of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func erf(x: Float64): Float64

/**
* Returns the Gauss error function of @p x.
* The related definition is: \f$ {erf} x={\frac {2}{\sqrt {\pi }}}\int _{0}^{x}e^{-t^{2}}\,dt \f$
*
* @param x some value.
* @return the Gauss error function of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func erf(x: Float32): Float32

/**
* Returns the Gauss error function of @p x.
* The related definition is: \f$ {erf} x={\frac {2}{\sqrt {\pi }}}\int _{0}^{x}e^{-t^{2}}\,dt \f$
*
* @param x some value.
* @return the Gauss error function of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func erf(x: Float16): Float16

/********************* Trigonometric functions *********************//**
* Returns the trigonometric tangent of an angle.
*
* @param x an angle, in radians.
* @return the tangent of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func tan(x: Float64): Float64

/**
* Returns the trigonometric tangent of an angle.
*
* @param x an angle, in radians.
* @return the tangent of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func tan(x: Float32): Float32

/**
* Returns the trigonometric tangent of an angle.
*
* @param x an angle, in radians.
* @return the tangent of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func tan(x: Float16): Float16

/**
* Returns the arc sine of a value, the returned angle is in the range -π/2 through π/2.
*
* @param x some value.
* @return the arc sine of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is larger than 1.0 or less than -1.0
*/
@!APILevel[12, atomicservice : true]
public func asin(x: Float64): Float64

/**
* Returns the arc sine of a value, the returned angle is in the range -π/2 through π/2.
*
* @param x some value.
* @return the arc sine of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is larger than 1.0 or less than -1.0
*/
@!APILevel[12, atomicservice : true]
public func asin(x: Float32): Float32

/**
* Returns the arc sine of a value, the returned angle is in the range -π/2 through π/2.
*
* @param x some value.
* @return the arc sine of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is larger than 1.0 or less than -1.0
*/
@!APILevel[12, atomicservice : true]
public func asin(x: Float16): Float16

/**
* Returns the arc cosine of a value, the returned angle is in the range 0.0 through π.
*
* @param x some value.
* @return the arc cosine of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is larger than 1.0 or less than -1.0
*/
@!APILevel[12, atomicservice : true]
public func acos(x: Float64): Float64

/**
* Returns the arc cosine of a value, the returned angle is in the range 0.0 through π.
*
* @param x some value.
* @return the arc cosine of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is larger than 1.0 or less than -1.0
*/
@!APILevel[12, atomicservice : true]
public func acos(x: Float32): Float32

/**
* Returns the arc cosine of a value; the returned angle is in the range 0.
*
* @param Float16 of x.
* @return Float16 type of number.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is larger than 1.0 or less than -1.0
*/
@!APILevel[12, atomicservice : true]
public func acos(x: Float16): Float16

/**
* Returns the arc tangent of a value, the returned angle is in the range -π/2 through π/2.
*
* @param x some value.
* @return the arc tangent of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func atan(x: Float64): Float64

/**
* Returns the arc tangent of a value, the returned angle is in the range -π/2 through π/2.
*
* @param x some value.
* @return the arc tangent of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func atan(x: Float32): Float32

/**
* Returns the arc tangent of a value, the returned angle is in the range -π/2 through π/2.
*
* @param x some value.
* @return the arc tangent of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func atan(x: Float16): Float16

/********************* Hiberbolic functions *********************//**
* Returns the hyperbolic sine of a float64 value.
*
* @param x some value.
* @return the hyperbolic sine of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func sinh(x: Float64): Float64

/**
* Returns the hyperbolic sine of a float32 value.
*
* @param x some value.
* @return the hyperbolic sine of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func sinh(x: Float32): Float32

/**
* Returns the hyperbolic sine of a float16 value.
*
* @param x some value.
* @return the hyperbolic sine of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func sinh(x: Float16): Float16

/**
* Returns the hyperbolic cosine of a float64 value.
*
* @param x some value.
* @return the hyperbolic cosine of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func cosh(x: Float64): Float64

/**
* Returns the hyperbolic cosine of a float32 value.
*
* @param x some value.
* @return the hyperbolic cosine of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func cosh(x: Float32): Float32

/**
* Returns the hyperbolic cosine of a float16 value.
*
* @param x some value.
* @return the hyperbolic cosine of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func cosh(x: Float16): Float16

/**
* Returns the hyperbolic tangent of a float64 value.
*
* @param x some value.
* @return the hyperbolic tangent of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func tanh(x: Float64): Float64

/**
* Returns the hyperbolic tangent of a float32 value.
*
* @param x some value.
* @return the hyperbolic tangent of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func tanh(x: Float32): Float32

/**
* Returns the hyperbolic tangent of a float16 value.
*
* @param x some value.
* @return the hyperbolic tangent of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func tanh(x: Float16): Float16

/**
* Returns the inverse hyperbolic sine of a float64 value.
*
* @param x some value.
* @return the inverse hyperbolic sine of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func asinh(x: Float64): Float64

/**
* Returns the inverse hyperbolic sine of a float32 value.
*
* @param x some value.
* @return the inverse hyperbolic sine of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func asinh(x: Float32): Float32

/**
* Returns the inverse hyperbolic sine of a float16 value.
*
* @param x some value.
* @return the inverse hyperbolic sine of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func asinh(x: Float16): Float16

/**
* Returns the inverse hyperbolic cosine of a float64 value.
*
* @param x some value.
* @return the inverse hyperbolic cosine of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is less than 1.0
*/
@!APILevel[12, atomicservice : true]
public func acosh(x: Float64): Float64

/**
* Returns the inverse hyperbolic cosine of a float32 value.
*
* @param x some value.
* @return the inverse hyperbolic cosine of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is less than 1.0
*/
@!APILevel[12, atomicservice : true]
public func acosh(x: Float32): Float32

/**
* Returns the inverse hyperbolic cosine of a float16 value.
*
* @param x some value.
* @return the inverse hyperbolic cosine of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is less than 1.0
*/
@!APILevel[12, atomicservice : true]
public func acosh(x: Float16): Float16

/**
* Returns the inverse hyperbolic tangent of a float64 value.
*
* @param x some value.
* @return the inverse hyperbolic tangent of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is not larger than -1.0 and less than 1.0
*/
@!APILevel[12, atomicservice : true]
public func atanh(x: Float64): Float64

/**
* Returns the inverse hyperbolic tangent of a float32 value.
*
* @param x some value.
* @return the inverse hyperbolic tangent of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is not larger than -1.0 and less than 1.0
*/
@!APILevel[12, atomicservice : true]
public func atanh(x: Float32): Float32

/**
* Returns the inverse hyperbolic tangent of a float16 value.
*
* @param x some value.
* @return the inverse hyperbolic tangent of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is not in range (-1.0, 1.0)
*/
@!APILevel[12, atomicservice : true]
public func atanh(x: Float16): Float16

/**
* Number of zero bits returned before the most significant (leftmost) bit.
*
* @param xx some value.
* @return number of zero bits before the most significant bit of @p xx.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func leadingZeros(x: UInt64): Int64

/**
* Number of zero bits returned before the most significant (leftmost) bit.
*
* @param xx some value.
* @return number of zero bits before the most significant bit of @p xx.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func leadingZeros(x: UInt32): Int64

/**
* Number of zero bits returned before the most significant (leftmost) bit.
*
* @param xx some value.
* @return number of zero bits before the most significant bit of @p xx.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func leadingZeros(x: UInt16): Int64

/**
* Number of zero bits returned before the most significant (leftmost) bit.
*
* @param x some value.
* @return number of zero bits before the most significant bit of @p xx.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func leadingZeros(x: UInt8): Int64

/**
* Number of zero bits returned before the most significant (leftmost) bit.
*
* @param x some value.
* @return number of zero bits before the most significant bit of @p x.
*
* @since 0.18.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func leadingZeros(x: Int8): Int64

/**
* Number of zero bits returned before the most significant (leftmost) bit.
*
* @param x some value.
* @return number of zero bits before the most significant bit of @p x.
*
* @since 0.18.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func leadingZeros(x: Int16): Int64

/**
* Number of zero bits returned before the most significant (leftmost) bit.
*
* @param x some value.
* @return number of zero bits before the most significant bit of @p x.
*
* @since 0.18.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func leadingZeros(x: Int32): Int64

/**
* Number of zero bits returned before the most significant (leftmost) bit.
*
* @param x some value.
* @return number of zero bits before the most significant bit of @p x.
*
* @since 0.18.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func leadingZeros(x: Int64): Int64

/**
* Obtains the number of consecutive 0s starting from the least significant bits.
*
* @param x some value.
* @return number of consecutive 0s starting from the least significant bit of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func trailingZeros(x: UInt64): Int64

/**
* Obtains the number of consecutive 0s starting from the least significant bits.
*
* @param x some value.
* @return number of consecutive 0s starting from the least significant bit of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func trailingZeros(x: UInt32): Int64

/**
* Obtains the number of consecutive 0s starting from the least significant bits.
*
* @param x some value.
* @return number of consecutive 0s starting from the least significant bit of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func trailingZeros(x: UInt16): Int64

/**
* Obtains the number of consecutive 0s starting from the least significant bits.
*
* @param x some value.
* @return number of consecutive 0s starting from the least significant bit of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func trailingZeros(x: UInt8): Int64

/**
* Obtains the number of consecutive 0s starting from the least significant bits.
*
* @param x some value.
* @return number of consecutive 0s starting from the least significant bit of @p x.
*
* @since 0.18.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func trailingZeros(x: Int8): Int64

/**
* Obtains the number of consecutive 0s starting from the least significant bits.
*
* @param x some value.
* @return number of consecutive 0s starting from the least significant bit of @p x.
*
* @since 0.18.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func trailingZeros(x: Int16): Int64

/**
* Obtains the number of consecutive 0s starting from the least significant bits.
*
* @param x some value.
* @return number of consecutive 0s starting from the least significant bit of @p x.
*
* @since 0.18.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func trailingZeros(x: Int32): Int64

/**
* Obtains the number of consecutive 0s starting from the least significant bits.
*
* @param x some value.
* @return number of consecutive 0s starting from the least significant bit of @p x.
*
* @since 0.18.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func trailingZeros(x: Int64): Int64

/**
* Calculate the number of one.
*
* @param x some value.
* @return the number of one in @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func countOnes(x: UInt64): Int64

/**
* Calculate the number of one.
*
* @param x some value.
* @return the number of one in @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func countOnes(x: UInt32): Int64

/**
* Calculate the number of one.
*
* @param x some value.
* @return the number of one in @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func countOnes(x: UInt16): Int64

/**
* Calculate the number of one.
*
* @param x some value.
* @return the number of one in @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func countOnes(x: UInt8): Int64

/**
* Calculate the number of one.
*
* @param x some value.
* @return the number of one in @p x.
*
* @since 0.18.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func countOnes(x: Int8): Int64

/**
* Calculate the number of one.
*
* @param x some value.
* @return the number of one in @p x.
*
* @since 0.18.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func countOnes(x: Int16): Int64

/**
* Calculate the number of one.
*
* @param x some value.
* @return the number of one in @p x.
*
* @since 0.18.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func countOnes(x: Int32): Int64

/**
* Calculate the number of one.
*
* @param x some value.
* @return the number of one in @p x.
*
* @since 0.18.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func countOnes(x: Int64): Int64

@Deprecated[message: "Use global funtion `public func countOnes(x: Int8): Int64` instead."]
@!APILevel[12, atomicservice : true]
public func countOne(x: Int8): Int8

@Deprecated[message: "Use global funtion `public func countOnes(x: Int16): Int64` instead."]
@!APILevel[12, atomicservice : true]
public func countOne(x: Int16): Int8

@Deprecated[message: "Use global funtion `public func countOnes(x: Int32): Int64` instead."]
@!APILevel[12, atomicservice : true]
public func countOne(x: Int32): Int8

@Deprecated[message: "Use global funtion `public func countOnes(x: Int64): Int64` instead."]
@!APILevel[12, atomicservice : true]
public func countOne(x: Int64): Int8

@Deprecated[message: "Use global funtion `public func countOnes(x: UInt8): Int64` instead."]
@!APILevel[12, atomicservice : true]
public func countOne(x: UInt8): Int8

@Deprecated[message: "Use global funtion `public func countOnes(x: UInt16): Int64` instead."]
@!APILevel[12, atomicservice : true]
public func countOne(x: UInt16): Int8

@Deprecated[message: "Use global funtion `public func countOnes(x: UInt32): Int64` instead."]
@!APILevel[12, atomicservice : true]
public func countOne(x: UInt32): Int8

@Deprecated[message: "Use global funtion `public func countOnes(x: UInt64): Int64` instead."]
@!APILevel[12, atomicservice : true]
public func countOne(x: UInt64): Int8

/**
* Reverse bit pattern.
*
* @param x some value.
* @return reverse bits of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func reverse(x: UInt64): UInt64

/**
* Reverse bit pattern.
*
* @param x some value.
* @return reverse bits of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func reverse(x: UInt32): UInt32

/**
* Reverse bit pattern.
*
* @param x some value.
* @return reverse bits of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func reverse(x: UInt16): UInt16

/**
* Reverse bit pattern.
*
* @param x some value.
* @return reverse bits of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func reverse(x: UInt8): UInt8

/**
* Determine the absolute value of the Int8 type.
*
* @param x some value.
* @return absolute value of @p x or None.
*
* @throws OverflowException if the argument `x` is the minimum value of Int8
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func checkedAbs(x: Int8): Option<Int8>

/**
* Determine the absolute value of the Int16 type.
*
* @param x some value.
* @return absolute value of @p x or None.
*
* @throws OverflowException if the argument `x` is the min value of Int16
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func checkedAbs(x: Int16): Option<Int16>

/**
* Determine the absolute value of the Int32 type.
*
* @param x some value.
* @return absolute value of @p x or None.
*
* @throws OverflowException if the argument `x` is the minimum value of Int32
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func checkedAbs(x: Int32): Option<Int32>

/**
* Determine the absolute value of the Int64 type.
*
* @param x some value.
* @return absolute value of @p x or None.
*
* @throws OverflowException if the argument `x` is the minimum value of Int64
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func checkedAbs(x: Int64): Option<Int64>

/**
* Rotation UInt8 type calculation
* If the length of the number of digits is exceeded,
* 0 of the same number of digits is returned.
* Returns the result of rotating the number of the first operand by one amount specified by the second operand.
* The second operand must be an integer range in - precision to precision.
* The absolute operand of seconds gives the number of positions to rotate.
* If the second operand is positive, rotate to the left; Otherwise, rotate to the right.
* The coefficient of the first operand is padded to the left with 0 to length precision,
* if necessary. The sign and exponent of the first operand remain unchanged.
*
* @param num an UInt8 argument.
* @param d number of Rotated Bits.
* @return rotated Value.
*
* @since 0.17.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func rotate(num: UInt8, d: Int8): UInt8

/**
* Rotation UInt16 type calculation
* If the length of the number of digits is exceeded,
* 0 of the same number of digits is returned.
* Returns the result of rotating the number of the first operand by one amount specified by the second operand.
* The second operand must be an integer range in - precision to precision.
* The absolute operand of seconds gives the number of positions to rotate.
* If the second operand is positive, rotate to the left; Otherwise, rotate to the right.
* The coefficient of the first operand is padded to the left with 0 to length precision,
* if necessary. The sign and exponent of the first operand remain unchanged.
*
* @param num an UInt16 argument.
* @param d number of Rotated Bits.
* @return rotated Value.
*
* @since 0.17.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func rotate(num: UInt16, d: Int8): UInt16

/**
* Rotation UInt32 type calculation
* If the length of the number of digits is exceeded,
* 0 of the same number of digits is returned.
* Returns the result of rotating the number of the first operand by one amount specified by the second operand.
* The second operand must be an integer range in - precision to precision.
* The absolute operand of seconds gives the number of positions to rotate.
* If the second operand is positive, rotate to the left; Otherwise, rotate to the right.
* The coefficient of the first operand is padded to the left with 0 to length precision,
* if necessary. The sign and exponent of the first operand remain unchanged.
*
* @param num an UInt32 argument.
* @param d number of Rotated Bits.
* @return rotated Value.
*
* @since 0.17.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func rotate(num: UInt32, d: Int8): UInt32

/**
* Rotation UInt64 type calculation
* If the length of the number of digits is exceeded,
* 0 of the same number of digits is returned.
* Returns the result of rotating the number of the first operand by one amount specified by the second operand.
* The second operand must be an integer range in - precision to precision.
* The absolute operand of seconds gives the number of positions to rotate.
* If the second operand is positive, rotate to the left; Otherwise, rotate to the right.
* The coefficient of the first operand is padded to the left with 0 to length precision,
* if necessary. The sign and exponent of the first operand remain unchanged.
*
* @param num an UInt64 argument.
* @param d number of Rotated Bits.
* @return rotated Value.
*
* @since 0.17.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func rotate(num: UInt64, d: Int8): UInt64

/**
* Rotate Int8 type calculation.
* Returns the result of rotating the number of the first operand by one amount specified by the second operand.
* The second operand must be an integer range in - precision to precision.
* The absolute operand of seconds gives the number of positions to rotate.
* If the second operand is positive, rotate to the left; Otherwise, rotate to the right.
* The coefficient of the first operand is padded to the left with 0 to length precision,
* if necessary. The sign and exponent of the first operand remain unchanged.
* Note: Binary negative numbers are taken as complements.
*
* @param num an Int8 argument.
* @param d number of Rotated Bits.
* @return rotated Value.
*
* @since 0.17.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func rotate(num: Int8, d: Int8): Int8

/**
* Rotate Int16 type calculation.
* Returns the result of rotating the number of the first operand by one amount specified by the second operand.
* The second operand must be an integer range in - precision to precision.
* The absolute operand of seconds gives the number of positions to rotate.
* If the second operand is positive, rotate to the left; Otherwise, rotate to the right.
* The coefficient of the first operand is padded to the left with 0 to length precision,
* if necessary. The sign and exponent of the first operand remain unchanged.
* Note: Binary negative numbers are taken as complements.
*
* @param num an Int16 argument.
* @param d number of Rotated Bits.
* @return rotated Value.
*
* @since 0.17.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func rotate(num: Int16, d: Int8): Int16

/**
* Rotate Int32 type calculation.
* Returns the result of rotating the number of the first operand by one amount specified by the second operand.
* The second operand must be an integer range in - precision to precision.
* The absolute operand of seconds gives the number of positions to rotate.
* If the second operand is positive, rotate to the left; Otherwise, rotate to the right.
* The coefficient of the first operand is padded to the left with 0 to length precision,
* if necessary. The sign and exponent of the first operand remain unchanged.
* Note: Binary negative numbers are taken as complements.
*
* @param num an Int32 argument.
* @param d number of Rotated Bits.
* @return rotated Value.
*
* @since 0.17.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func rotate(num: Int32, d: Int8): Int32

/**
* Rotate Int64 type calculation.
* Returns the result of rotating the number of the first operand by one amount specified by the second operand.
* The second operand must be an integer range in - precision to precision.
* The absolute operand of seconds gives the number of positions to rotate.
* If the second operand is positive, rotate to the left; Otherwise, rotate to the right.
* The coefficient of the first operand is padded to the left with 0 to length precision,
* if necessary. The sign and exponent of the first operand remain unchanged.
* Note: Binary negative numbers are taken as complements.
*
* @param num an Int64 argument.
* @param d number of Rotated Bits.
* @return rotated Value.
*
* @since 0.17.4
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func rotate(num: Int64, d: Int8): Int64

/**
* Returns the largest of the commonfactors of two Int8 numbers.
* Using a modified version of the Euclidean algorithm: Stein.
*
* @param x an argument.
* @param y another argument.
* @return a maximum common divisor.
*
* @throws OverflowException if the argument `x` or `y` is the minimum value of Int8
*
* @since 0.17.4
*
* @throws IllegalArgumentException if argument `x` and `y` are minimum values of Int8,
* or if one argument is the minimum value of Int8 and the other is 0
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func gcd(x: Int8, y: Int8): Int8

/**
* Returns the largest of the commonfactors of two Int16 numbers.
* Using a modified version of the Euclidean algorithm: Stein.
*
* @param x an argument.
* @param y another argument.
* @return a maximum common divisor.
*
* @throws OverflowException if the argument `x` or `y` is the minimum value of Int16
* @throws IllegalArgumentException if argument `x` and `y` are minimum values of Int16,
* or if one argument is the minimum value of Int16 and the other is 0
*
* @since 0.17.4
*
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func gcd(x: Int16, y: Int16): Int16

/**
* Returns the largest of the commonfactors of two Int32 numbers.
* Using a modified version of the Euclidean algorithm: Stein.
*
* @param x an argument.
* @param y another argument.
* @return a maximum common divisor.
*
* @throws OverflowException if the argument `x` or `y` is the minimum value of Int32
* @throws IllegalArgumentException if argument `x` and `y` are minimum values of Int32,
* or if one argument is the minimum value of Int32 and the other is 0
*
* @since 0.17.4
*
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func gcd(x: Int32, y: Int32): Int32

/**
* Returns the largest of the commonfactors of two Int64 numbers.
* Using a modified version of the Euclidean algorithm: Stein.
*
* @param x an argument.
* @param y another argument.
* @return a maximum common divisor.
*
* @throws OverflowException if the argument `x` or `y` is the minimum value of Int64
* @throws IllegalArgumentException if argument `x` and `y` are minimum values of Int64,
* or if one argument is the minimum value of Int64 and the other is 0
*
* @since 0.17.4
*
*/
@OverflowWrapping
@!APILevel[12, atomicservice : true]
public func gcd(x: Int64, y: Int64): Int64

/**
* Returns the largest of the commonfactors of two UInt8 numbers.
* Using a modified version of the Euclidean algorithm: Stein.
*
* @param x an argument.
* @param y another argument.
* @return a maximum common divisor.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func gcd(x: UInt8, y: UInt8): UInt8

/**
* Returns the largest of the commonfactors of two UInt16 numbers.
* Using a modified version of the Euclidean algorithm: Stein.
*
* @param x an argument.
* @param y another argument.
* @return a maximum common divisor.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func gcd(x: UInt16, y: UInt16): UInt16

/**
* Returns the largest of the commonfactors of two UInt32 numbers.
* Using a modified version of the Euclidean algorithm: Stein.
*
* @param x an argument.
* @param y another argument.
* @return a maximum common divisor.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func gcd(x: UInt32, y: UInt32): UInt32

/**
* Returns the largest of the commonfactors of two UInt64 numbers.
* Using a modified version of the Euclidean algorithm: Stein.
*
* @param x an argument.
* @param y another argument.
* @return a maximum common divisor.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func gcd(x: UInt64, y: UInt64): UInt64

/**
* Calculation of the least common multiple of the Int8 type.
*
* @param x some value.
* @param y another value.
* @return the least common multiple value of @p x and @p y.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if argument `x` and `y` are minimum values of Int8,
* or if one argument is the minimum value of Int8 and the other is 0.
* @throws IllegalArgumentException if the result is out of the Int8 range.
*/
@!APILevel[12, atomicservice : true]
public func lcm(x: Int8, y: Int8): Int8

/**
* Calculation of the least common multiple of the Int16 type.
*
* @param x some value.
* @param y another value.
* @return the least common multiple value of @p x and @p y.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if argument `x` and `y` are minimum values of Int16,
* or if one argument is the minimum value of Int16 and the other is 0.
* @throws IllegalArgumentException if the result is out of the Int16 range.
*/
@!APILevel[12, atomicservice : true]
public func lcm(x: Int16, y: Int16): Int16

/**
* Calculation of the least common multiple of the Int32 type.
*
* @param x some value.
* @param y another value.
* @return the least common multiple value of @p x and @p y.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if argument `x` and `y` are minimum values of Int32,
* or if one argument is the minimum value of Int32 and the other is 0.
* @throws IllegalArgumentException if the result is out of the Int32 range.
*/
@!APILevel[12, atomicservice : true]
public func lcm(x: Int32, y: Int32): Int32

/**
* Calculation of the least common multiple of the Int64 type.
*
* @param x some value.
* @param y another value.
* @return the least common multiple value of @p x and @p y.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if argument `x` and `y` are minimum values of Int64,
* or if one argument is the minimum value of Int64 and the other is 0.
* @throws IllegalArgumentException if the result is out of the Int64 range.
*/
@!APILevel[12, atomicservice : true]
public func lcm(x: Int64, y: Int64): Int64

/**
* Calculation of the least common multiple of the UInt8 type.
*
* @param x some value.
* @param y another value.
* @return the least common multiple value of @p x and @p y.
* @throws IllegalArgumentException if the result is out of the UInt8 range
*
* @since 0.17.4
*
*/
@!APILevel[12, atomicservice : true]
public func lcm(x: UInt8, y: UInt8): UInt8

/**
* Calculation of the least common multiple of the UInt16 type.
*
* @param x some value.
* @param y another value.
* @return the least common multiple value of @p x and @p y.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the result is out of the UInt16 range
*/
@!APILevel[12, atomicservice : true]
public func lcm(x: UInt16, y: UInt16): UInt16

/**
* Calculation of the least common multiple of the UInt32 type.
*
* @param x some value.
* @param y another value.
* @return the least common multiple value of @p x and @p y.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the result is out of the UInt32 range
*/
@!APILevel[12, atomicservice : true]
public func lcm(x: UInt32, y: UInt32): UInt32

/**
* Calculation of the least common multiple of the UInt64 type.
*
* @param x some value.
* @param y another value.
* @return the least common multiple value of @p x and @p y.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the result is out of the UInt64 range
*/
@!APILevel[12, atomicservice : true]
public func lcm(x: UInt64, y: UInt64): UInt64

/**
* Panics if min > max, min is NaN, or max is NaN.
* Restrict a value to a certain Interval unless it is NaN.
* Returns max if self is greater than max, and min if self is less than min.
* Otherwise this returns self. Note that this function returns NaN if the initial value was NaN as well.
*
* @brief  Returns a number between the maximum and minimum values.
*
* @param   v   an argument.
* @param   min   a minimum argument.
* @param   max   a maximum argument.
* @return  restricted value between @p min and @p max.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `min` is larger than `max`, or `min` or `max` is NaN
*/
@!APILevel[12, atomicservice : true]
public func clamp(v: Float16, min: Float16, max: Float16): Float16

/**
* Panics if min > max, min is NaN, or max is NaN.
* Restrict a value to a certain Interval unless it is NaN.
* Returns max if self is greater than max, and min if self is less than min.
* Otherwise this returns self. Note that this function returns NaN if the initial value was NaN as well.
*
* @brief  Returns a number between the maximum and minimum values.
*
* @param   v   an argument.
* @param   min   a minimum argument.
* @param   max   a maximum argument.
* @return  restricted value between @p min and @p max.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `min` is larger than `max`, or `min` or `max` is NaN
*/
@!APILevel[12, atomicservice : true]
public func clamp(v: Float32, min: Float32, max: Float32): Float32

/**
* Panics if min > max, min is NaN, or max is NaN.
* Restrict a value to a certain Interval unless it is NaN.
* Returns max if self is greater than max, and min if self is less than min.
* Otherwise this returns self. Note that this function returns NaN if the initial value was NaN as well.
* @brief  Returns a number between the maximum and minimum values.
*
* @param   v   an argument.
* @param   min   a minimum argument.
* @param   max   a maximum argument.
* @return  restricted value between @p min and @p max.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `min` is larger than `max`, or `min` or `max` is NaN
*/
@!APILevel[12, atomicservice : true]
public func clamp(v: Float64, min: Float64, max: Float64): Float64

/**
* The banker algorithm is an IEEE-754 specification.
* If the value of the rounding bit is less than 5, the rounding bit is directly performed.
* If the value of the rounding bit is greater than 5, the value of the rounding bit is incremented by 1.
* When the number of rounded bits is equal to 5, there are two cases:
*    If 5 is followed by other non-zero digits (that is, 5 is not the last digit), the carry value increases by 1.
*    If there is only 0 after 5 (that is, 5 is the last digit), the system determines whether the first digit is an odd number.
*    If the first digit is an odd number, the system adds 1 to the carry value. If the first digit is an even number, the system rounds it off.
*
* @param x Floating point value
* @return Float64 Rounded value of @x
*
* @since 0.25.2
*/
@!APILevel[12, atomicservice : true]
public func round(x: Float64): Float64

/**
* The banker algorithm is an IEEE-754 specification.
* If the value of the rounding bit is less than 5, the rounding bit is directly performed.
* If the value of the rounding bit is greater than 5, the value of the rounding bit is incremented by 1.
* When the number of rounded bits is equal to 5, there are two cases:
*    If 5 is followed by other non-zero digits (that is, 5 is not the last digit), the carry value increases by 1.
*    If there is only 0 after 5 (that is, 5 is the last digit), the system determines whether the first digit is an odd number.
*    If the first digit is an odd number, the system adds 1 to the carry value. If the first digit is an even number, the system rounds it off.
*
* @param x Floating point value
* @return Float64 Rounded value of @x
*
* @since 0.25.2
*/
@!APILevel[12, atomicservice : true]
public func round(x: Float32): Float32

/**
* The banker algorithm is an IEEE-754 specification.
* If the value of the rounding bit is less than 5, the rounding bit is directly performed.
* If the value of the rounding bit is greater than 5, the value of the rounding bit is incremented by 1.
* When the number of rounded bits is equal to 5, there are two cases:
*    If 5 is followed by other non-zero digits (that is, 5 is not the last digit), the carry value increases by 1.
*    If there is only 0 after 5 (that is, 5 is the last digit), the system determines whether the first digit is an odd number.
*    If the first digit is an odd number, the system adds 1 to the carry value. If the first digit is an even number, the system rounds it off.
*
* @param x Floating point value
* @return Float64 Rounded value of @x
*
* @since 0.25.2
*/
@!APILevel[12, atomicservice : true]
public func round(x: Float16): Float16

/**
* These functions shall compute the principal value of the arc tangent of y/x, using the signs of both arguments to determine the quadrant of the return value.
*
* ref: https://pubs.opengroup.org/onlinepubs/9799919799/functions/atan2.html
*/
@!APILevel[12, atomicservice : true]
public func atan2(y: Float64, x: Float64): Float64

/**
* These functions shall compute the principal value of the arc tangent of y/x, using the signs of both arguments to determine the quadrant of the return value.
*
* ref: https://pubs.opengroup.org/onlinepubs/9799919799/functions/atan2.html
*/
@!APILevel[12, atomicservice : true]
public func atan2(y: Float32, x: Float32): Float32

/**
* These functions shall compute the principal value of the arc tangent of y/x, using the signs of both arguments to determine the quadrant of the return value.
*
* ref: https://pubs.opengroup.org/onlinepubs/9799919799/functions/atan2.html
*/
@!APILevel[12, atomicservice : true]
public func atan2(y: Float16, x: Float16): Float16

/**
* These functions shall return the floating-point remainder of the division of x by y.
* These functions shall return the value x- i* y, for some integer i such that, if y is non-zero, the result has the same sign as x and magnitude less than the magnitude of y.
*
* ref: https://pubs.opengroup.org/onlinepubs/9699919799/functions/fmod.html
*/
@!APILevel[12, atomicservice : true]
public func fmod(x: Float64, y: Float64): Float64

/**
* These functions shall return the floating-point remainder of the division of x by y.
* These functions shall return the value x- i* y, for some integer i such that, if y is non-zero, the result has the same sign as x and magnitude less than the magnitude of y.
*
* ref: https://pubs.opengroup.org/onlinepubs/9699919799/functions/fmod.html
*/
@!APILevel[12, atomicservice : true]
public func fmod(x: Float32, y: Float32): Float32

/**
* These functions shall return the floating-point remainder of the division of x by y.
* These functions shall return the value x- i* y, for some integer i such that, if y is non-zero, the result has the same sign as x and magnitude less than the magnitude of y.
*
* ref: https://pubs.opengroup.org/onlinepubs/9699919799/functions/fmod.html
*/
@!APILevel[12, atomicservice : true]
public func fmod(x: Float16, y: Float16): Float16

/********************* utility functions *********************//**
* Returns the absolute value of a floating-point number.
* If the parameter is not a negative number, the parameter is returned.
* If the argument is negative, the negative number of the argument is returned.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the argument is infinite, the result is positive infinity.
*
* @param x some value.
* @return the absolute value of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func abs(x: Float64): Float64

/**
* Returns the absolute value of a floating-point number.
* If the parameter is not a negative number, the parameter is returned.
* If the argument is negative, the negative number of the argument is returned.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the argument is infinite, the result is positive infinity.
*
* @param x some value.
* @return the absolute value of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func abs(x: Float32): Float32

/**
* Returns the absolute value of a floating-point number.
* If the parameter is not a negative number, the parameter is returned.
* If the argument is negative, the negative number of the argument is returned.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the argument is infinite, the result is positive infinity.
*
* @param x some value.
* @return the absolute value of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func abs(x: Float16): Float16

/**
* Returns the absolute value of a number.
* If the parameter is not a negative number, the parameter is returned.
* If the argument is negative, the negative number of the argument is returned.
* special cases that arise:
* An OverflowException will be thrown if the argument is MIN_VALUE.
*
* @param x some value.
* @return the absolute value of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func abs(x: Int64): Int64

/**
* Returns the absolute value of a number.
* If the parameter is not a negative number, the parameter is returned.
* If the argument is negative, the negative number of the argument is returned.
* special cases that arise:
* An OverflowException will be thrown if the argument is MIN_VALUE.
*
* @param x some value.
* @return the absolute value of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func abs(x: Int32): Int32

/**
* Returns the absolute value of a number.
* If the parameter is not a negative number, the parameter is returned.
* If the argument is negative, the negative number of the argument is returned.
* special cases that arise:
* An OverflowException will be thrown if the argument is MIN_VALUE.
*
* @param x some value.
* @return the absolute value of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func abs(x: Int16): Int16

/**
* Returns the absolute value of a number.
* If the parameter is not a negative number, the parameter is returned.
* If the argument is negative, the negative number of the argument is returned.
* special cases that arise:
* An OverflowException will be thrown if the argument is MIN_VALUE.
*
* @param x some value.
* @return the absolute value of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func abs(x: Int8): Int8

/**
* Returns the square root of the value.
* The square root of a positive value is the positive number
* of the square root of the size of the value.
* special cases that arise:
* If the parameter is negative, an invalid parameter exception is thrown.
* If the argument is NaN, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
*
* @param x some value.
* @return the square root of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func sqrt(x: Float16): Float16

/**
* Returns the square root of the value.
* The square root of a positive value is the positive number
* of the square root of the size of the value.
* special cases that arise:
* If the parameter is negative, an invalid parameter exception is thrown.
* If the argument is NaN, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
*
* @param x some value.
* @return the square root of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func sqrt(x: Float32): Float32

/**
* Returns Euler's number raised to the power of a value.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the parameter is negative, an invalid parameter exception is thrown.
* If the argument is positive infinite, the result is positive infinity.
* If the argument is negative infinity, then the result is zero
*
* @param x some value.
* @return \f$ e^{x} \f$.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func exp(x: Float32): Float32

/**
* Returns 2's number raised to the power of a value.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the parameter is negative, an invalid parameter exception is thrown.
* If the argument is positive infinite, the result is positive infinity.
* If the argument is negative infinity, then the result is zero
*
* @param x some value.
* @return \f$ 2^{x} \f$.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func exp2(x: Float64): Float64

/**
* Returns 2's number raised to the power of a value.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the parameter is negative, an invalid parameter exception is thrown.
* If the argument is positive infinite, the result is positive infinity.
* If the argument is negative infinity, then the result is zero
*
* @param x some value.
* @return \f$ 2^{x} \f$.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func exp2(x: Float32): Float32

/**
* Returns 2's number raised to the power of a value.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the parameter is negative, an invalid parameter exception is thrown.
* If the argument is positive infinite, the result is positive infinity.
* If the argument is negative infinity, then the result is zero
*
* @param x some value.
* @return \f$ 2^{x} \f$.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func exp2(x: Float16): Float16

/**
* Returns the base 10 logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
* If the argument is equal to a 10*n integer, the result is n.
*
* @param x some value.
* @return base 10 logarithm of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func log10(x: Float64): Float64

/**
* Returns the base 10 logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
* If the argument is equal to a 10*n integer, the result is n.
*
* @param x some value.
* @return base 10 logarithm of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func log10(x: Float32): Float32

/**
* Returns the base 10 logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
* If the argument is equal to a 10*n integer, the result is n.
*
* @param x some value.
* @return base 10 logarithm of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func log10(x: Float16): Float16

/**
* Returns the base 2 logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
* If the argument is equal to a 2*n integer, the result is n.
*
* @param x some value.
* @return base 2 logarithm of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func log2(x: Float64): Float64

/**
* Returns the base 2 logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
* If the argument is equal to a 2*n integer, the result is n.
*
* @param x some value.
* @return base 2 logarithm of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func log2(x: Float32): Float32

/**
* Returns the base 2 logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
* If the argument is equal to a 2*n integer, the result is n.
*
* @param x some value.
* @return base 2 logarithm of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func log2(x: Float16): Float16

/**
* Return the natural logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
*
* @param x some value.
* @return \f$ \ln{x} \f$.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func log(x: Float64): Float64

/**
* Return the natural logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
*
* @param x some value.
* @return \f$ \ln{x} \f$.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func log(x: Float32): Float32

/**
* Return the natural logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
*
* @param x some value.
* @return \f$ \ln{x} \f$.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func log(x: Float16): Float16

/**
* Return the largest (closest to positive infinity) floating-point value that
* less than or equal to the argument and is equal to a mathematical integer.
*
* @param x some value.
* @return the largest (closest to positive infinity) floating-point value that
* less than or equal to the argument and is equal to a mathematical integer.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func floor(x: Float64): Float64

/**
* Return the largest (closest to positive infinity) floating-point value that
* less than or equal to the argument and is equal to a mathematical integer.
*
* @param x some value.
* @return the largest (closest to positive infinity) floating-point value that
* less than or equal to the argument and is equal to a mathematical integer.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func floor(x: Float32): Float32

/**
* Return the largest (closest to positive infinity) floating-point value that
* less than or equal to the argument and is equal to a mathematical integer.
*
* @param x some value.
* @return the largest (closest to positive infinity) floating-point value that
* less than or equal to the argument and is equal to a mathematical integer.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func floor(x: Float16): Float16

/**
* Return the smallest (closest to negative infinity) floating-point value that
* is greater than or equal to the argument and is equal to a mathematical integer.
*
* @param x some value.
* @return the smallest (closest to negative infinity) floating-point value that
* is greater than or equal to the argument and is equal to a mathematical integer.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func ceil(x: Float64): Float64

/**
* Return the smallest (closest to negative infinity) floating-point value that
* is greater than or equal to the argument and is equal to a mathematical integer.
*
* @param x some value.
* @return the smallest (closest to negative infinity) floating-point value that
* is greater than or equal to the argument and is equal to a mathematical integer.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func ceil(x: Float32): Float32

/**
* Truncates the values after the decimal point.
*
* @param x some value.
* @return the values after the decimal point is 0.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func trunc(x: Float64): Float64

/**
* Truncates the values after the decimal point.
*
* @param x some value.
* @return the values after the decimal point is 0.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func trunc(x: Float32): Float32

/**
* Truncates the values after the decimal point.
*
* @param x some value.
* @return the values after the decimal point is 0.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func trunc(x: Float16): Float16

/********************* Trigonometric functions *********************//**
* Returns the trigonometric sine of an angle.
*
* @param x an angle, in radians.
* @return the sine of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func sin(x: Float64): Float64

/**
* Returns the trigonometric sine of an angle.
*
* @param x an angle, in radians.
* @return the sine of @p x.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func sin(x: Float16): Float16

/**
* Returns the value of the first argument raised to the power of the second argument.
* @param base the base.
* @param exponent the exponent.
* @return \f$ {base}^{exponent} \f$.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func pow(base: Float64, exponent: Float64): Float64

/**
* Returns the value of the first argument raised to the power of the second argument.
* special cases that arise:
* @param base the base.
* @param exponent the exponent.
* @return \f$ {base}^{exponent} \f$.
*
* @since 0.17.4
*/
@!APILevel[12, atomicservice : true]
public func pow(base: Float64, exponent: Int64): Float64

/**
* Returns the absolute value of a number.
* If the parameter is not a negative number, the parameter is returned.
* If the argument is negative, the negative number of the argument is returned.
* special cases that arise:
* An OverflowException will be thrown if the argument is MIN_VALUE.
*
* @param x some value.
* @return the absolute value of @p x.
*
* @since 0.17.4
*
* @throws OverflowException if the argument `x` is the minimum value of Int8
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func abs(x: Int8): Int8

/**
* Returns the square root of the value.
* The square root of a positive value is the positive number
* of the square root of the size of the value.
* special cases that arise:
* If the parameter is negative, an invalid parameter exception is thrown.
* If the argument is NaN, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
*
* @param x some value.
* @return the square root of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is negative
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func sqrt(x: Float16): Float16

/**
* Returns the square root of the value.
* The square root of a positive value is the positive number
* of the square root of the size of the value.
* special cases that arise:
* If the parameter is negative, an invalid parameter exception is thrown.
* If the argument is NaN, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
*
* @param x some value.
* @return the square root of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is negative
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func sqrt(x: Float32): Float32

/**
* Returns the square root of the value.
* The square root of a positive value is the positive number
* of the square root of the size of the value.
* special cases that arise:
* If the parameter is negative, an invalid parameter exception is thrown.
* If the argument is NaN, the result is NaN.
* If the argument is positive infinite, the result is positive infinity.
*
* @param x some value.
* @return the square root of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is negative
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func sqrt(x: Float64): Float64

/**
* Returns Euler's number raised to the power of a value.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the parameter is negative, an invalid parameter exception is thrown.
* If the argument is positive infinite, the result is positive infinity.
* If the argument is negative infinity, then the result is zero
*
* @param x some value.
* @return \f$ e^{x} \f$.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func exp(x: Float64): Float64

/**
* Returns Euler's number raised to the power of a value.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the parameter is negative, an invalid parameter exception is thrown.
* If the argument is positive infinite, the result is positive infinity.
* If the argument is negative infinity, then the result is zero
*
* @param x some value.
* @return \f$ e^{x} \f$.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func exp(x: Float16): Float16

/**
* Returns 2's number raised to the power of a value.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the parameter is negative, an invalid parameter exception is thrown.
* If the argument is positive infinite, the result is positive infinity.
* If the argument is negative infinity, then the result is zero
*
* @param x some value.
* @return \f$ 2^{x} \f$.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func exp2(x: Float64): Float64

/**
* Returns 2's number raised to the power of a value.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the parameter is negative, an invalid parameter exception is thrown.
* If the argument is positive infinite, the result is positive infinity.
* If the argument is negative infinity, then the result is zero
*
* @param x some value.
* @return \f$ 2^{x} \f$.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func exp2(x: Float32): Float32

/**
* Returns 2's number raised to the power of a value.
* special cases that arise:
* If the argument is NaN, the result is NaN.
* If the parameter is negative, an invalid parameter exception is thrown.
* If the argument is positive infinite, the result is positive infinity.
* If the argument is negative infinity, then the result is zero
*
* @param x some value.
* @return \f$ 2^{x} \f$.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func exp2(x: Float16): Float16

/**
* Returns the base 10 logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
* If the argument is equal to a 10*n integer, the result is n.
*
* @param x some value.
* @return base 10 logarithm of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is not positive
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func log10(x: Float64): Float64

/**
* Returns the base 10 logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
* If the argument is equal to a 10*n integer, the result is n.
*
* @param x some value.
* @return base 10 logarithm of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is not positive
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func log10(x: Float32): Float32

/**
* Returns the base 10 logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
* If the argument is equal to a 10*n integer, the result is n.
*
* @param x some value.
* @return base 10 logarithm of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is not positive
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func log10(x: Float16): Float16

/**
* Returns the base 2 logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
* If the argument is equal to a 2*n integer, the result is n.
*
* @param x some value.
* @return base 2 logarithm of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is not positive
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func log2(x: Float64): Float64

/**
* Returns the base 2 logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
* If the argument is equal to a 2*n integer, the result is n.
*
* @param x some value.
* @return base 2 logarithm of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is not positive
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func log2(x: Float32): Float32

/**
* Returns the base 2 logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
* If the argument is equal to a 2*n integer, the result is n.
*
* @param x some value.
* @return base 2 logarithm of @p x.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is not positive
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func log2(x: Float16): Float16

/**
* Return the natural logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
*
* @param x some value.
* @return \f$ \ln{x} \f$.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is not positive
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func log(x: Float64): Float64

/**
* Return the natural logarithm of a value.
* special cases that arise:
* If the parameter is NaN or less than zero, the result is NaN.
* If the argument is positive infinity, the result is positive infinity.
* If the argument is positive zero or negative zero, the result is negative infinity.
*
* @param x some value.
* @return \f$ \ln{x} \f$.
*
* @since 0.17.4
*
* @throws IllegalArgumentException if the argument `x` is not positive
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func log(x: Float32): Float32

/**
* Return the largest (closest to positive infinity) floating-point value that
* less than or equal to the argument and is equal to a mathematical integer.
*
* @param x some value.
* @return the largest (closest to positive infinity) floating-point value that
* less than or equal to the argument and is equal to a mathematical integer.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func floor(x: Float64): Float64

/**
* Return the largest (closest to positive infinity) floating-point value that
* less than or equal to the argument and is equal to a mathematical integer.
*
* @param x some value.
* @return the largest (closest to positive infinity) floating-point value that
* less than or equal to the argument and is equal to a mathematical integer.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func floor(x: Float32): Float32

/**
* Return the largest (closest to positive infinity) floating-point value that
* less than or equal to the argument and is equal to a mathematical integer.
*
* @param x some value.
* @return the largest (closest to positive infinity) floating-point value that
* less than or equal to the argument and is equal to a mathematical integer.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func floor(x: Float16): Float16

/**
* Return the smallest (closest to negative infinity) floating-point value that
* is greater than or equal to the argument and is equal to a mathematical integer.
*
* @param x some value.
* @return the smallest (closest to negative infinity) floating-point value that
* is greater than or equal to the argument and is equal to a mathematical integer.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func ceil(x: Float64): Float64

/**
* Return the smallest (closest to negative infinity) floating-point value that
* is greater than or equal to the argument and is equal to a mathematical integer.
*
* @param x some value.
* @return the smallest (closest to negative infinity) floating-point value that
* is greater than or equal to the argument and is equal to a mathematical integer.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func ceil(x: Float32): Float32

/**
* Return the smallest (closest to negative infinity) floating-point value that
* is greater than or equal to the argument and is equal to a mathematical integer.
*
* @param x some value.
* @return the smallest (closest to negative infinity) floating-point value that
* is greater than or equal to the argument and is equal to a mathematical integer.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func ceil(x: Float16): Float16

/**
* Truncates the values after the decimal point.
*
* @param x some value.
* @return the values after the decimal point is 0.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func trunc(x: Float64): Float64

/**
* Truncates the values after the decimal point.
*
* @param x some value.
* @return the values after the decimal point is 0.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func trunc(x: Float16): Float16

/**
* Returns the trigonometric sine of an angle.
*
* @param x an angle, in radians.
* @return the sine of @p x.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func sin(x: Float32): Float32

/**
* Returns the trigonometric sine of an angle.
*
* @param x an angle, in radians.
* @return the sine of @p x.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func sin(x: Float16): Float16

/**
* Returns the trigonometric cosine of an angle.
*
* @param x an angle, in radians.
* @return the cosine of @p x.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func cos(x: Float32): Float32

/**
* Returns the trigonometric cosine of an angle.
*
* @param x an angle, in radians.
* @return the cosine of @p x.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func cos(x: Float16): Float16

/**
* Returns the value of the first argument raised to the power of the second argument.
* @param base the base.
* @param exponent the exponent.
* @return \f$ {base}^{exponent} \f$.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func pow(base: Float32, exponent: Float32): Float32

/**
* Returns the value of the first argument raised to the power of the second argument.
* @param base the base.
* @param exponent the exponent.
* @return \f$ {base}^{exponent} \f$.
*
* @since 0.17.4
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func pow(base: Float32, exponent: Int32): Float32

@Deprecated[message: "Use `public interface FloatingPoint<T>` instead."]
@!APILevel[12, atomicservice : true]
public interface MathExtension<T> {
}

@!APILevel[12, atomicservice : true]
public interface MaxMinValue<T> {
}

@!APILevel[12, atomicservice : true]
public interface Number<T> {
}

@!APILevel[12, atomicservice : true]
public interface Integer<T> <: Number<T> {
}

@!APILevel[12, atomicservice : true]
public interface FloatingPoint<T> <: Number<T> {
}

