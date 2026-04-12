// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package std.math.numeric
import std.convert.*
import std.math.*
import std.random.*


@!APILevel[12, atomicservice : true]
public struct BigInt <: Comparable<BigInt> & Hashable & ToString {
    /**
    * Return the signum function of this BigInt.
    * 
    * @return: 
    *  -1: if this BigInt is negative;
    *   0: if this BigInt equals to zero;
    *   1: if this BigInt is positive. 
    */
    @!APILevel[12, atomicservice : true]
    public prop sign: Int64
    
    /**
    * Return the minmum of the number of bits that can present this BigInt.
    * For example, using the fewest bits to present decimal -3 is 101, 
    * so, if a BigInt's value is decimal -3, it bitLen is 3. 
    * 
    * @return: the minmum of the number of bits to present this BigInt.
    */
    @!APILevel[12, atomicservice : true]
    public prop bitLen: Int64
    
    /**
    * Constructs a prime of a BigInt, which is in the range of [0, 2 ^ {bitLen} - 1].
    * The probability that the BigInt constructed is not a prime is at most (1 / 4 ^ {@p certainty}).
    * 
    * @param bitLen: the number of bits to construct the BigInt;
    * @param certainty: a measure of a probability that the result is not a prime;
    * @param rand: source of randomness to construct the BigInt.
    *
    * @throw IllegalArgumentException, if @p bitLen <= 1. 
    */
    @!APILevel[12, atomicservice : true]
    public static func randomProbablePrime(bitLen: Int64, certainty: UInt64, rand!: Random = Random()): BigInt
    
    /**
    * Translates a byte array @p bytes containing the two's-complement binary into a BigInt. 
    * @p bytes is in big-endian byte-order, which means that 
    * the most significant byte is @p bytes[0].
    * 
    * @param bytes: a binary number to construct the BigInt.
    * 
    * @throw IllegalArgumentException if the size of @p bytes is zero.
    */
    @!APILevel[12, atomicservice : true]
    public init(bytes: Array<Byte>)
    
    /**
    * Translates a sign-magnitude, whose sign is @p sign and magnitude is @p magnitude, into a BigInt. 
    * @p magnitude is in big-endian byte-order.
    * Note that if the size of @p bytes is zero, translating magnitude = 0 to the BigInt.
    * 
    * @param sign: signum of the number, false for negitive and true for non-negtive;
    * @param magnitude: big-endian binary representation of the magnitude of the number
    * 
    * @throw IllegalArgumentException if the value of @p magnitude is 0 and @p sign = false.
    */
    @!APILevel[12, atomicservice : true]
    public init(sign: Bool, magnitude: Array<Byte>)
    
    /**
    * Translate an Int8 type value @p val into a BigInt.
    * 
    * @param n: a value translated to a BigInt.
    */
    @!APILevel[12, atomicservice : true]
    public init(n: Int8)
    
    /**
    * Translate an Int16 type value @p val into a BigInt.
    * 
    * @param n: a value translated to a BigInt.
    */
    @!APILevel[12, atomicservice : true]
    public init(n: Int16)
    
    /**
    * Translate an Int32 type value @p val into a BigInt.
    * 
    * @param n: a value translated to a BigInt.
    */
    @!APILevel[12, atomicservice : true]
    public init(n: Int32)
    
    /**
    * Translate an Int64 type value @p val into a BigInt.
    * 
    * @param n: a value translated to a BigInt.
    */
    @!APILevel[12, atomicservice : true]
    public init(n: Int64)
    
    /**
    * Translate an UInt8 type value @p val into a BigInt.
    * 
    * @param n: a value translated to a BigInt.
    */
    @!APILevel[12, atomicservice : true]
    public init(n: UInt8)
    
    /**
    * Translate an UInt16 type value @p val into a BigInt.
    * 
    * @param n: a value translated to a BigInt.
    */
    @!APILevel[12, atomicservice : true]
    public init(n: UInt16)
    
    /**
    * Translate an UInt32 type value @p val into a BigInt.
    * 
    * @param n: a value translated to a BigInt.
    */
    @!APILevel[12, atomicservice : true]
    public init(n: UInt32)
    
    /**
    * Translate an UInt64 type value @p val into a BigInt.
    * 
    * @param n: a value translated to a BigInt.
    */
    @!APILevel[12, atomicservice : true]
    public init(n: UInt64)
    
    /**
    * Translate an UIntNative type value @p val into a BigInt.
    * 
    * @param n: a value translated to a BigInt.
    */
    @!APILevel[12, atomicservice : true]
    public init(n: UIntNative)
    
    /**
    * Translate an IntNative type value @p val into a BigInt.
    * 
    * @param n: a value translated to a BigInt.
    */
    @!APILevel[12, atomicservice : true]
    public init(n: IntNative)
    
    /**
    * Translate an Float16 type value @p n into a BigInt.
    * Any fractional part of this Float will be discarded.
    * 
    * @param n: a value translated to a BigInt.
    */
    @!APILevel[12, atomicservice : true]
    public init(n: Float16)
    
    /**
    * Translate an Float32 type value @p n into a BigInt.
    * Any fractional part of this Float will be discarded.
    * 
    * @param n: a value translated to a BigInt.
    */
    @!APILevel[12, atomicservice : true]
    public init(n: Float32)
    
    /**
    * Translate an Float64 type value @p n into a BigInt.
    * Any fractional part of this Float will be discarded.
    * 
    * @param n: a value translated to a BigInt.
    */
    @!APILevel[12, atomicservice : true]
    public init(n: Float64)
    
    /**
    * Construct a sign-magnitude, whose sign is @p sign and 
    * magnitude is generated randomly in the range of [0, 2 ^ {bitLen} - 1]
    *
    * @param sign: signum of the number, false for negitive and true for non-negtive;
    * @param bitLen: the maximum number of bits in magnitude;
    * @param rand: source of randomness to construct @p magnitude.
    *
    * @throw IllegalArgumentException, if @p bitLen <= 0. 
    */
    @!APILevel[12, atomicservice : true]
    public init(sign: Bool, bitLen: Int64, rand!: Random = Random())
    
    /**
    * Translate a value presented by string @p value into a BigInt.
    * Here, @p value is a string form of a @p base base number.
    * The form of @p value string follows the BNF 'IntegerString' shown below.
    *
    * IntegerString
    *     : (SignString)? ValueString
    *
    * SignString
    *     : + | -
    *
    * ValueString
    *     : digit digits
    *
    * digit
    *     : r'0' ~ r'9' | r'A' ~ r'Z' | r'a' ~ r'z'
    *
    * where the digit follows the restriction: 
    * - if digit belongs to 0 ~ 9, then (digit - r'0') < base; 
    * - if digit belongs to r'A' ~ r'Z', then (digit - r'A') + 10 < base;
    * - if digit belongs to r'a' ~ r'z', then (digit - r'A') + 10 < base.
    * 
    * @param value: the string form of the specific base number to translate to the BigInt;
    * @param base: the base of the number in translation.
    * 
    * @throws IllegalArgumentException 
    * - if @p val does match the required string format, or
    * - if @p base does not in the range of [2, 36].
    */
    @Deprecated[message: "Use member function `public static func parse(value: String, radix!: Int): BigInt` instead."]
    @!APILevel[12, atomicservice : true]
    public init(s: String, base!: Int64 = 10)
    
    /**
    * Return the value of the bit at the specified index in this BigInt.
    *  
    * @param index: index of bit to test.
    * 
    * @return: true, if (this & (1 << @p index) != 0); otherwise, false.
    * 
    * @throw IllegalArgumentException, if @p index < 0. 
    */
    @!APILevel[12, atomicservice : true]
    public func testBit(index: Int64): Bool
    
    /**
    * Search the index of the rightmost (lowest-order) one bit in this BigInt.
    * If there is no one bit in this BigInt, return -1.
    *
    * @return: 
    * - index of the rightmost one bit in this BigInt, if exists;
    * - otherwise, -1.
    */
    @Deprecated[message: "Use global function `public func trailingZeros(x: BigInt): Int64` instead."]
    @!APILevel[12, atomicservice : true]
    public func lowestOneBit(): Int64
    
    /**
    * Construct a new BigInt by modifying the bit at @p index to one in this BigInt.
    * 
    * @param index: index of bit to set.
    * 
    * @return: this | (1 << @p index)
    * 
    * @throw IllegalArgumentException, if @p index < 0.
    */
    @!APILevel[12, atomicservice : true]
    public func setBit(index: Int64): BigInt
    
    /**
    * Construct a new BigInt by modifying the bit at @p index to zero in this BigInt.
    * 
    * @param index: index of bit to clear.
    * 
    * @return: this & ~ (1 << @p index)
    * 
    * @throw IllegalArgumentException, if @p index < 0.
    */
    @!APILevel[12, atomicservice : true]
    public func clearBit(index: Int64): BigInt
    
    /**
    * Construct a new BigInt by flipping the bit at @p index in this BigInt.
    * 
    * @param index: index of bit to flip.
    * 
    * @return: this ^ (1 << @p index)
    * 
    * @throw IllegalArgumentException, if @p index < 0.
    */
    @!APILevel[12, atomicservice : true]
    public func flipBit(index: Int64): BigInt
    
    /**
    * Test whether this BigInt is a prime.
    * The probability that such method views a composite as a prime is at most (1 / 4 ^ {@p certainty}).
    * 
    * @param certainty: a measure of a probability that such method views a composite as a prime;
    */
    @!APILevel[12, atomicservice : true]
    public func isProbablePrime(certainty: UInt64): Bool
    
    /**
    * Return a BigInt, whose value is (this + @p that).
    *  
    * @param that: the value to be added to this BigInt;
    * 
    * @return: the result of (this + @p that).  
    */
    @!APILevel[12, atomicservice : true]
    public operator func +(that: BigInt): BigInt
    
    /**
    * Return a BigInt, whose value is (this - @p that).
    *  
    * @param that: the value to be substracted from this BigInt;
    * 
    * @return: the result of (this - @p that).  
    */
    @!APILevel[12, atomicservice : true]
    public operator func -(that: BigInt): BigInt
    
    /**
    * Return a BigInt, whose value is (this * @p that).
    *  
    * @param that: the value to be multiplied by this BigInt;
    * 
    * @return: the result of (this * @p that).  
    */
    @!APILevel[12, atomicservice : true]
    public operator func *(that: BigInt): BigInt
    
    /**
    * Return a BigInt, whose value is (this / @p that).
    * The result of the quotient is rounding towards zero.
    * For exampke, -7 divided by 2 is (-3.5), so the result is -3.
    *  
    * @param that: the value that this BigInt is to be divided;
    * 
    * @return: the result of (this / @p that).  
    * 
    * @throws ArithmeticException if @p that == 0.
    */
    @!APILevel[12, atomicservice : true]
    public operator func /(that: BigInt): BigInt
    
    /**
    * Return a BigInt, whose value is (this % @p that).
    * The result satisfies the following restrictions: 
    * - If this BigInt is a positive, the result is non-negative; 
    * - If this BigInt is a negative, the result is negative or zero.
    * For example, -7 / 2 = -3 --- (-1), so the result is -1.
    *  
    * @param that: the value that this BigInt is to be divided;
    * 
    * @return: the result of (this % @p that).  
    * 
    * @throws ArithmeticException if @p that == 0.
    */
    @!APILevel[12, atomicservice : true]
    public operator func %(that: BigInt): BigInt
    
    /**
    * Return a pair containing (this / @p that) and (this % @p that).
    * The result of the quotient is rounding towards zero.
    *  
    * @param that: the value that this BigInt is to be divided;
    * 
    * @return: the pair containing (this / @p that) and (this % @p that).  
    * 
    * @throws ArithmeticException if @p that == 0.
    */
    @!APILevel[12, atomicservice : true]
    public func divAndMod(that: BigInt): (BigInt, BigInt)
    
    /**
    * Return a BigInt, whose value equals to this BigInt divided by @p that.
    * The caculation follows Euclidean division.
    *  
    * @param that: the value that this BigInt is to be divided;
    * 
    * @return: the quotient of this BigInt divided by @p that.  
    * 
    * @throws ArithmeticException if @p that == 0.
    */
    @Deprecated[message: "Use member function `public func divAndMod(that: BigInt): (BigInt, BigInt)` instead."]
    @!APILevel[12, atomicservice : true]
    public func quo(that: BigInt): BigInt
    
    /**
    * Return a BigInt, whose value equals to the reminder of this BigInt divided by @p that.
    * The result is always non-negative.
    *  
    * @param that: the value that this BigInt is to be divided;
    * 
    * @return: the reminder of this BigInt divided by @p that.  
    * 
    * @throws ArithmeticException if @p that == 0.
    */
    @Deprecated[message: "Use member function `public func divAndMod(that: BigInt): (BigInt, BigInt)` instead."]
    @!APILevel[12, atomicservice : true]
    public func rem(that: BigInt): BigInt
    
    /**
    * Return a BigInt, which is a pair of the quotient and the reminder of this BigInt divided by @p that.
    * The caculation follows Eculidean division.
    *  
    * @param that: the value that this BigInt is to be divided;
    * 
    * @return: the reminder of this BigInt divided by @p that.  
    * 
    * @throws ArithmeticException if @p that == 0.
    */
    @Deprecated[message: "Use member function `public func divAndMod(that: BigInt): (BigInt, BigInt)` instead."]
    @!APILevel[12, atomicservice : true]
    public func quoAndRem(that: BigInt): (BigInt, BigInt)
    
    /**
    * Return a BigInt v, where ((this * v) % (@p that) == 1).
    * 
    * @param that: the modulus.
    * 
    * @return: a modular multiplicative inverse of this BigInt.
    * 
    * @throws IllegalArgumentException if this BigInt and @p that are not relatively prime.
    */
    @!APILevel[12, atomicservice : true]
    public func modInverse(that: BigInt): BigInt
    
    /**
    * Return a BigInt, whose value is (- this).
    * 
    * @return: the result of (- this).
    */
    @!APILevel[12, atomicservice : true]
    public operator func -(): BigInt
    
    /**
    * Return a BigInt, whose value is (this ** (@p n)).
    *  
    * @param n: power to raise this BigInt to.
    * 
    * @return: the result of (this ** (@p n)). 
    */
    @!APILevel[12, atomicservice : true]
    public operator func **(n: UInt64): BigInt
    
    /**
    * Return a BigInt, whose value is ((this ** (@p n)) % (@p m)).
    * The result is rounding towards to zero.
    *  
    * @param n: power to raise this BigInt to.
    * @param m: the modulus, None means no modular operation.
    * 
    * @return: the result of ((this ** (@p n)) % (@p m). 
    * 
    * @throw IllegalArgumentException if @p n < 0;
    * @throw ArithmeticException if @p m equals to zero. 
    */
    @!APILevel[12, atomicservice : true]
    public func modPow(n: BigInt, m!: ?BigInt = None): BigInt
    
    /**
    * Return a BigInt, whose value is (this & (@p that)).
    * 
    * @param that: value to be AND'ed with this BigInt.
    * 
    * @return: the result of (this & (@p that)).
    */
    @!APILevel[12, atomicservice : true]
    public operator func &(that: BigInt): BigInt
    
    /**
    * Return a BigInt, whose value is (this | (@p that)).
    * 
    * @param that: value to be OR'ed with this BigInt.
    * 
    * @return: the result of (this | (@p that)).
    */
    @!APILevel[12, atomicservice : true]
    public operator func |(that: BigInt): BigInt
    
    /**
    * Return a BigInt, whose value is (this ^ (@p that)).
    * 
    * @param that: value to be XOR'ed with this BigInt.
    * 
    * @return: the result of (this ^ (@p that)).
    */
    @!APILevel[12, atomicservice : true]
    public operator func ^(that: BigInt): BigInt
    
    /**
    * Return a BigInt, whose value is (! this).
    * 
    * @return: the result of (! this).
    */
    @!APILevel[12, atomicservice : true]
    public operator func !(): BigInt
    
    /**
    * Return a BigInt by shifting this BigInt right n bits.
    * 
    * @param: the number of bit to shift.
    * 
    * @throw ArithmeticException if @p n < 0.
    */
    @!APILevel[12, atomicservice : true]
    public operator func >>(n: Int64): BigInt
    
    /**
    * Return a BigInt by shifting this BigInt right n bits.
    * 
    * @param: the number of bit to shift.
    * 
    * @throw ArithmeticException if @p n < 0.
    */
    @!APILevel[12, atomicservice : true]
    public operator func <<(n: Int64): BigInt
    
    /**
    * Report whether this BigInt is equal to another BigInt.
    * 
    * @param that: the BigInt for comparison. 
    * 
    * @return: true if this BigInt is equal to @p that, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: BigInt): Bool
    
    /**
    * Report whether this BigInt is not equal to another BigInt.
    * 
    * @param that: the BigInt for comparison. 
    * 
    * @return: true if this BigInt is not equal to @p that, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: BigInt): Bool
    
    /**
    * Report whether this BigInt is greater than another BigInt.
    * 
    * @param that: the BigInt for comparison. 
    * 
    * @return: true if this BigInt is greater than @p that, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func >(that: BigInt): Bool
    
    /**
    * Report whether this BigInt is greater than or equal to another BigInt.
    * 
    * @param that: the BigInt for comparison. 
    * 
    * @return: true if this BigInt is greater than or equal to @p that, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func >=(that: BigInt): Bool
    
    /**
    * Report whether this BigInt is less than another BigInt.
    * 
    * @param that: the BigInt for comparison. 
    * 
    * @return: true if this BigInt is less than @p that, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func <(that: BigInt): Bool
    
    /**
    * Report whether this BigInt is less than or equal to another BigInt.
    * 
    * @param that: the BigInt for comparison. 
    * 
    * @return: true if this BigInt is less than or equal to @p that, otherwise false.
    */
    @!APILevel[12, atomicservice : true]
    public operator func <=(that: BigInt): Bool
    
    /**
    * Compare the relationship between this BigInt and another BigInt.
    *
    * @param that: the BigInt for comparison
    * 
    * @return: the relationship between two BigInt instances.
    */
    @!APILevel[12, atomicservice : true]
    public func compare(that: BigInt): Ordering
    
    /**
    * Obtain the hash value of this BigInt.
    *
    * @return: hash value of this BigInt.
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    /**
    * Return this BigInt as a decimal-based string.
    *
    * @return: the string representation of this BigDecimal.
    */
    @!APILevel[12, atomicservice : true]
    public func toString(): String
    
    /**
    * Return a byte array of the two's-complement representation of this BigInteger. 
    * The byte array is in big-endian byte-order.
    */
    @!APILevel[12, atomicservice : true]
    public func toBytes(): Array<Byte>
    
    /**
    * Converts this BigInt to a Int8. 
    * 
    * @param overflowHandling: handling policy when overflow occurs.
    * 
    * @return: the corresponding Int8 value is returned based on the overflow handling.
    * 
    * @throws OverflowException if the value of the BigInt exceeds the range of Int8 and the overflow handling of Throwing is selected
    */
    @!APILevel[12, atomicservice : true]
    public func toInt8(overflowHandling!: OverflowStrategy = Throwing): Int8
    
    /**
    * Converts this BigInt to a Int16. 
    * 
    * @param overflowHandling: handling policy when overflow occurs.
    * 
    * @return: the corresponding Int16 value is returned based on the overflow handling.
    * 
    * @throws OverflowException if the value of the BigInt exceeds the range of Int16 and the overflow handling of Throwing is selected
    */
    @!APILevel[12, atomicservice : true]
    public func toInt16(overflowHandling!: OverflowStrategy = Throwing): Int16
    
    /**
    * Converts this BigInt to a Int32. 
    * 
    * @param overflowHandling: handling policy when overflow occurs.
    * 
    * @return: the corresponding Int32 value is returned based on the overflow handling.
    * 
    * @throws OverflowException if the value of the BigInt exceeds the range of Int32 and the overflow handling of Throwing is selected
    */
    @!APILevel[12, atomicservice : true]
    public func toInt32(overflowHandling!: OverflowStrategy = Throwing): Int32
    
    /**
    * Converts this BigInt to a Int64. 
    * 
    * @param overflowHandling: handling policy when overflow occurs.
    * 
    * @return: the corresponding Int64 value is returned based on the overflow handling.
    * 
    * @throws OverflowException if the value of the BigInt exceeds the range of Int64 and the overflow handling of Throwing is selected
    */
    @!APILevel[12, atomicservice : true]
    public func toInt64(overflowHandling!: OverflowStrategy = Throwing): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toIntNative(overflowHandling!: OverflowStrategy = Throwing): IntNative
    
    @!APILevel[12, atomicservice : true]
    public func toFloat16(): Float16
    
    @!APILevel[12, atomicservice : true]
    public func toFloat32(): Float32
    
    @!APILevel[12, atomicservice : true]
    public func toFloat64(): Float64
    
    /**
    * Converts this BigInt to a UInt8. 
    * 
    * @param overflowHandling: handling policy when overflow occurs.
    * 
    * @return: the corresponding UInt8 value is returned based on the overflow handling.
    * 
    * @throws OverflowException if the value of the BigInt exceeds the range of UInt8 and the overflow handling of Throwing is selected
    */
    @!APILevel[12, atomicservice : true]
    public func toUInt8(overflowHandling!: OverflowStrategy = Throwing): UInt8
    
    /**
    * Converts this BigInt to a UInt16. 
    * 
    * @param overflowHandling: handling policy when overflow occurs.
    * 
    * @return: the corresponding UInt16 value is returned based on the overflow handling.
    * 
    * @throws OverflowException if the value of the BigInt exceeds the range of UInt16 and the overflow handling of Throwing is selected
    */
    @!APILevel[12, atomicservice : true]
    public func toUInt16(overflowHandling!: OverflowStrategy = Throwing): UInt16
    
    /**
    * Converts this BigInt to a UInt32. 
    * 
    * @param overflowHandling: handling policy when overflow occurs.
    * 
    * @return: the corresponding UInt32 value is returned based on the overflow handling.
    * 
    * @throws OverflowException if the value of the BigInt exceeds the range of UInt32 and the overflow handling of Throwing is selected
    */
    @!APILevel[12, atomicservice : true]
    public func toUInt32(overflowHandling!: OverflowStrategy = Throwing): UInt32
    
    /**
    * Converts this BigInt to a UInt64. 
    * 
    * @param overflowHandling: handling policy when overflow occurs.
    * 
    * @return: the corresponding UInt64 value is returned based on the overflow handling.
    * 
    * @throws OverflowException if the value of the BigInt exceeds the range of UInt64 and the overflow handling of Throwing is selected
    */
    @!APILevel[12, atomicservice : true]
    public func toUInt64(overflowHandling!: OverflowStrategy = Throwing): UInt64
    
    @!APILevel[12, atomicservice : true]
    public func toUIntNative(overflowHandling!: OverflowStrategy = Throwing): UIntNative
}

/**
* Return a BigInt, whose value is the absolute value of @p i BigInt.
* 
* @return: a BigInt equals to abs(@p i).
*/
@!APILevel[12, atomicservice : true]
public func abs(i: BigInt): BigInt

/**
* Returns a BigInt, whose value is the square root of BigInt @p i.
* The result is non-negative and rounding towards zero.
* 
* @return: the result of sqrt(@p i).
* 
* @throws IllegalArgumentException if this < 0.
*/
@!APILevel[12, atomicservice : true]
public func sqrt(i: BigInt): BigInt

/**
* Return the greatest common divisor of @p i1 and @p i2.
* The result is alway non-negtive. 
* 
* @param i1: value with which the gcd is to be computed;
* @param i2: value with which the gcd is to be computed.
* 
* @return: the greatest common divisor of @p i1 and @p i2.
*/
@!APILevel[12, atomicservice : true]
public func gcd(i1: BigInt, i2: BigInt): BigInt

/**
* Return the least common multiple of @p i1 and @p i2.
* The result is alway non-negtive. 
* 
* @param i1: value with which the lcm is to be computed;
* @param i2: value with which the lcm is to be computed.
* 
* @return: the least common multiple of @p i1 and @p i2.
*/
@!APILevel[12, atomicservice : true]
public func lcm(i1: BigInt, i2: BigInt): BigInt

@Deprecated[message: "Use global function `public func countOnes(i: BigInt): Int64` instead."]
@!APILevel[12, atomicservice : true]
public func countOne(i: BigInt): Int64

/**
* Calculate the number of one in the two's-complement binary, 
* which can present BigInt @p i with minmum of the number of bits.
* 
* @param i: value that is calculated the number of one.
* 
* @return: the number of one in the two's-complement binary of @p i.
*/
@!APILevel[12, atomicservice : true]
public func countOnes(i: BigInt): Int64

/**
* Obtains the number of consecutive 0s starting from the least significant bits.
*
* @param x some value.
* @return number of consecutive 0s starting from the least significant bit of @p x.
*
*/
@!APILevel[12, atomicservice : true]
public func trailingZeros(x: BigInt): Int64

extend BigInt <: Number<BigInt> {
}

extend BigInt <: Integer<BigInt> {
    @!APILevel[12, atomicservice : true]
    public static func isSigned(): Bool
}

extend BigInt <: Parsable<BigInt> {
    /**
    * Translate a value presented by string @p value into a BigInt.
    * The form of @p value string follows the BNF 'IntegerString' shown below.
    *
    * IntegerString
    *     : SignString? BaseString? ValueString
    *
    * SignString
    *     : '+' | '-'
    *
    * BaseString
    *     : '0b' | '0B' | '0o' | '0O' | '0x' | '0X'
    *
    * ValueString
    *     : digit*
    *
    * digit
    *     : '0' ~ '9' | 'A' ~ 'F' | 'a' ~ 'f'
    *
    * where the digit follows the restriction: 
    * - if the BaseString is '0b' or '0B', then digit should belong to '0' ~ '1'; 
    * - if the BaseString is '0o' or '0O', then digit should belong to '0' ~ '7'; 
    * - if the BaseString is '0x' or '0X', then digit should belong to '0' ~ '9', 'a' ~ 'f' or 'A' ~ 'F' ; 
    * - if the BaseString does not exist, then digit should belong to '0' ~ '9'; 
    * 
    * @param value: the string to translate to the BigInt;
    * 
    * @throws IllegalArgumentException 
    * - if @p value does match the required string format
    */
    @!APILevel[12, atomicservice : true]
    public static func parse(value: String): BigInt
    
    @!APILevel[12, atomicservice : true]
    public static func tryParse(value: String): ?BigInt
}

extend BigInt <: RadixConvertible<BigInt> {
    /**
    * Translate a value presented by string @p value into a BigInt.
    * Here, @p value is a string form of a @p radix number.
    * The form of @p value string follows the BNF 'IntegerString' shown below.
    *
    * IntegerString
    *     : SignString? ValueString
    *
    * SignString
    *     : '+' | '-'
    *
    * ValueString
    *     : digit+
    *
    * digit
    *     : '0' ~ '9' | 'A' ~ 'Z' | 'a' ~ 'z'
    *
    * where the digit follows the restriction: 
    * - if digit belongs to 0 ~ 9, then (digit - r'0') < radix; 
    * - if digit belongs to r'A' ~ r'Z', then (digit - r'A') + 10 < radix;
    * - if digit belongs to r'a' ~ r'z', then (digit - r'A') + 10 < radix.
    * 
    * @param value: the string form of the specific base number to translate to the BigInt;
    * @param radix: the radix of the number in translation.
    * 
    * @throws IllegalArgumentException 
    * - if @p value does match the required string format
    * - if @p radix does not in the range of [2, 36].
    */
    @!APILevel[12, atomicservice : true]
    public static func parse(value: String, radix!: Int): BigInt
    
    @!APILevel[12, atomicservice : true]
    public static func tryParse(value: String, radix!: Int): ?BigInt
    
    /**
    * Return the String representation of this BigInteger in the given radix.
    * 
    * @param radix: the radix of this BigInt.
    * 
    * @return: string representation of this BigInteger in the given radix.
    * 
    * @throws IllegalArgumentException if @p radix does not in the range of [2, 36]
    */
    @!APILevel[12, atomicservice : true]
    public func toString(radix!: Int): String
}

extend BigInt <: Formattable {
    /**
    * The format of @p fmt is as follows:
    *
    * Format
    *  : Flag? Width? Precision? Specifier?
    *
    * Flag
    *  : '-' | '+' | '#' | '0'
    *
    * Width
    *  : Integer
    *
    * Precision
    *  : '.' Integer
    *
    * Specifier
    *  :  'b' | 'B' | 'o' | 'O' | 'x' | 'X'
    *
    * The data layout in BigInt is as follows:
    *
    *  0x 1010 1010 1010 1010 1010 1010 1010 1010
    *    |-- [1] --|-- [0] --|------- int -------|
    */
    @!APILevel[12, atomicservice : true]
    public func format(fmt: String): String
}

/**
* @brief Decimal
*
* @since 0.46.1
*/
@!APILevel[12, atomicservice : true]
public struct Decimal <: Comparable<Decimal> & Hashable & ToString {
    /**
    * Return the scale of this Decimal.
    * A Decimal is presented as (value * (10 ^ {- scale})), and
    * this property returns the scale part.
    *
    * @return: the scale of this Decimal.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public prop scale: Int32
    
    /**
    * Return the precision, which is the number of digits in value, of this Decimal.
    *
    * @return: the scale of this Decimal.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public prop precision: Int64
    
    /**
    * Return the digits of this Decimal.
    * A Decimal is presented as (value * (10 ^ {- scale}), and
    * this property returns the value part.
    *
    * @return: the unscaled value of this Decimal.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public prop value: BigInt
    
    /**
    * Return the signum function of this Decimal.
    *
    * @return:
    *  -1: if this Decimal is negative;
    *   0: if this Decimal equals to zero;
    *   1: if this Decimal is positive.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public prop sign: Int64
    
    /**
    * Translate a string @p val into a Decimal.
    * The form of the string @p val follows the BNF 'DecimalString' shown below.
    *
    * DecimalString
    *     : (SignString)? ValueString (ExponentString)?
    * SignString
    *     : + | -
    * ValueString
    *     : IntegerPart.(FractionPart)?
    *     | .FractionPart
    *     | IntegerPart
    * IntegerPart
    *     : Digits
    * FractionPart
    *     : Digits
    * ExponentString
    *     : ExponentIndicator (SignString)? IntegerPart
    * ExponentIndicator
    *     : e | E
    * Digits
    *     : Digit | Digit Digits
    * Digit
    *     : 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9
    *
    * Translating @p val to a Decimal directly.
    *
    * @param val: a value translated to a Decimal
    *
    * @throws IllegalArgumentException if @p val does match the required string format
    *
    * @since 0.46.1
    */
    @Deprecated[message: "Use member function `public static func parse(value: String): Decimal` instead."]
    @!APILevel[12, atomicservice : true]
    public init(val: String)
    
    /**
    * Translate a BigInt @p value and an scale into a Decimal.
    * The value of the Decimal is (val × (10 ^ {-scale}))
    *
    * Constructing a Decimal, whose integer part is @p val and scale is @p scale.
    *
    * @param val: a value translated to a Decimal
    * @param scale: the count of numbers after the decimal point
    *
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public init(val: BigInt, scale: Int32)
    
    @!APILevel[12, atomicservice : true]
    public init(val: BigInt)
    
    /**
    * Translate an Int8 type value @p val into a Decimal.
    * The decimal sacle value is 0.
    * Translating @p val to a Decimal directly.
    *
    * @param val: a value translated to a Decimal
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public init(val: Int8)
    
    /**
    * Translate an Int16 type value @p val into a Decimal.
    * The decimal sacle value is 0.
    * Translating @p val to a Decimal directly.
    *
    * @param val: a value translated to a Decimal
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public init(val: Int16)
    
    /**
    * Translate an Int32 type value @p val into a Decimal.
    * The decimal sacle value is 0.
    * Translating @p val to a Decimal directly.
    *
    * @param val: a value translated to a Decimal
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public init(val: Int32)
    
    /**
    * Translate an IntNative type value @p val into a Decimal.
    * The decimal sacle value is 0.
    * Translating @p val to a Decimal directly.
    *
    * @param val: a value translated to a Decimal
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public init(val: IntNative)
    
    /**
    * Translate an Int64 type value @p val into a Decimal.
    * The decimal sacle value is 0.
    * Translating @p val to a Decimal directly.
    *
    * @param val: a value translated to a Decimal
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public init(val: Int64)
    
    /**
    * Translate an UInt8 type value @p val into a Decimal.
    * The decimal sacle value is 0.
    * Translating @p val to a Decimal directly.
    *
    * @param val: a value translated to a Decimal
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public init(val: UInt8)
    
    /**
    * Translate an UInt16 type value @p val into a Decimal.
    * The decimal sacle value is 0.
    * Translating @p val to a Decimal directly.
    *
    * @param val: a value translated to a Decimal
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public init(val: UInt16)
    
    /**
    * Translate an UInt32 type value @p val into a Decimal.
    * The decimal sacle value is 0.
    * Translating @p val to a Decimal directly.
    *
    * @param val: a value translated to a Decimal
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public init(val: UInt32)
    
    /**
    * Translate an UIntNative type value @p val into a Decimal.
    * The decimal sacle value is 0.
    * Translating @p val to a Decimal directly.
    *
    * @param val: a value translated to a Decimal
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public init(val: UIntNative)
    
    /**
    * Translate an UInt64 type value @p val into a Decimal.
    * The decimal sacle value is 0.
    * Translating @p val to a Decimal directly.
    *
    * @param val: a value translated to a Decimal
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public init(val: UInt64)
    
    /**
    * Translate a Float16 type value @p val into a Decimal.
    * Translating @p val to a Decimal directly.
    *
    * Note: The results of this constructor can be somewhat unpredictable.
    * This constructor provides an exact conversion, and may not have
    * the same result as Decimal(v.toString()), where the type of v is Float16.
    * For example, use 0.1f16 to constructor a Decimal instance, actually get 0.0999755859375 not equal to 0.1.
    *
    * Conversely, result of Decimal(string) constructor is perfectly predictable.
    * Decimal(string) constructor method is more recommend.
    *
    * @param val: a value translated to a Decimal
    *
    * @throws IllegalArgumentException if @p val equals to 'inf', '-inf' or 'nan'
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public init(val: Float16)
    
    /**
    * Translate a Float32 type value @p val into a Decimal.
    * Translating @p val to a Decimal directly.
    *
    * Note: The results of this constructor can be somewhat unpredictable.
    * This constructor provides an exact conversion, and may not have
    * the same result as Decimal(v.toString()), where the type of v is Float32.
    * For example, use 0.1f32 to constructor a Decimal instance,
    * actually get 0.100000001490116119384765625 not equal to 0.1.
    *
    * Conversely, result of Decimal(string) constructor is perfectly predictable.
    * Decimal(string) constructor method is more recommend.
    *
    * @param val: a value translated to a Decimal
    *
    * @throws IllegalArgumentException if @p val equals to 'inf', '-inf' or 'nan'
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public init(val: Float32)
    
    /**
    * Translate a Float64 type value @p val into a Decimal.
    * Translating @p val to a Decimal directly.
    *
    * Note: The results of this constructor can be somewhat unpredictable.
    * This constructor provides an exact conversion, and may not have
    * the same result as Decimal(v.toString()), where the type of v is Float64.
    * For example, use 0.1f64 to constructor a Decimal instance,
    * actually get 0.1000000000000000055511151231257827021181583404541015625 not equal to 0.1.
    *
    * Conversely, result of Decimal(string) constructor is perfectly predictable.
    * Decimal(string) constructor method is more recommend.
    *
    * @param val: a value translated to a Decimal
    *
    * @throws IllegalArgumentException if @p val equals to 'inf', '-inf' or 'nan'
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public init(val: Float64)
    
    /**
    * Overloading r'+' operator.
    * Return a Decimal, whose value is (this + @p d),
    * with the precision is 0 which means unlimited precision,
    * the specific precision is determined by the calculation result.
    * The result has scale = max(this.scale, @p d.scale).
    *
    * @param d: the value to be added to this Decimal;
    *
    * @return: the result of (this + @p d).
    *
    * @throw OverflowException if (this.scale - @p d.scale) exceeds the range of Int32.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public operator func +(d: Decimal): Decimal
    
    @!APILevel[12, atomicservice : true]
    public operator func -(d: Decimal): Decimal
    
    @!APILevel[12, atomicservice : true]
    public operator func *(d: Decimal): Decimal
    
    @!APILevel[12, atomicservice : true]
    public operator func /(d: Decimal): Decimal
    
    /**
    * Return a Decimal, whose value is (this / @p d),
    * with the precision and roundingMode.
    *
    * @param d: the value that this Decimal is to be divided;
    * @param precision: transfer decimal value to precision.
    * @param roundingMode: transfer the value to roundingMode.
    *
    * @return: the result of (this / @p d) rounded according to the precision and roundingMode.
    *
    * @throws IllegalArgumentException if @p d == 0.
    * @throws OverflowException if the target Decimal is not in the range of
    * [-(maxValue(precision) * (10 ^ {Int32.MAX})), maxValue(precision) * (10 ^ {Int32.MAX})]
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func divWithPrecision(d: Decimal, precision: Int64, roundingMode!: RoundingMode = HalfEven): Decimal
    
    @Deprecated[message: "Use member function `public func divAndMod(d: Decimal): (BigInt, Decimal)` instead."]
    @!APILevel[12, atomicservice : true]
    public func divAndRem(d: Decimal): (BigInt, Decimal)
    
    /**
    * Return a BigInt i and Remainder r, where i equals to the integer part of (this / d),
    * and r satisfies this = (i * d) + r.
    *
    * @param d: the value that this Decimal is to be divided;
    *
    * @throws IllegalArgumentException if @p d == 0.
    * @throws OverflowException if the target Decimal is not in the range of
    * [-(maxValue(precision) * (10 ^ {Int32.MAX})), maxValue(precision) * (10 ^ {Int32.MAX})]
    *
    * For example, (5.3 / 2.1) = 2 --- 1.1
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func divAndMod(d: Decimal): (BigInt, Decimal)
    
    @!APILevel[12, atomicservice : true]
    public operator func -(): Decimal
    
    /** 
    * Return a Decimal, which is equivalent to this one 
    * with the decimal point moved n places to the left if n >= 0, or
    * with the decimal point moved (-n) places to the right if n < 0.
    * 
    * @param n: number of places to move the decimal point.
    * 
    * @return: the result of (this * 10^{-n}), whose digits is the same as this Decimal.
    */
    @!APILevel[12, atomicservice : true]
    public func shiftPoint(n: Int32): Decimal
    
    /**
    * Return a Decimal rounded according to @p precision and @p roundingMode.
    * 
    * @param precision: transfer decimal value to precision.
    * @param roundingMode: transfer the value to roundingMode.
    * 
    * @return: a Decimal rounded according to @p precision and @p roundingMode.
    * 
    * @throws OverflowException if the target Decimal is not in the range of 
    * [-(maxValue(precision) * (10 ^ {Int32.MAX})), maxValue(precision) * (10 ^ {Int32.MAX})]
    */
    @!APILevel[12, atomicservice : true]
    public func roundWithPrecision(precision: Int64, roundingMode!: RoundingMode = RoundingMode.HalfEven): Decimal
    
    /**
    * Return whether this Decimal is less than another Decimal.
    *
    * @param d: the Decimal for comparison.
    *
    * @return: true if this Decimal is less than @p d, otherwise false.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public operator func <(d: Decimal): Bool
    
    /**
    * Return whether this Decimal is greater than another Decimal.
    *
    * @param d: the Decimal for comparison.
    *
    * @return: true if this Decimal is greater than @p d, otherwise false.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public operator func >(d: Decimal): Bool
    
    /**
    * Return whether this Decimal is less than or equal to another Decimal.
    *
    * @param d: the Decimal for comparison.
    *
    * @return: true if this Decimal is less than or equal to @p d, otherwise false.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public operator func <=(d: Decimal): Bool
    
    /**
    * Return whether this Decimal is greater than or equal to another Decimal.
    *
    * @param d: the Decimal for comparison.
    *
    * @return: true if this Decimal is greater than or equal to @p d, otherwise false.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public operator func >=(d: Decimal): Bool
    
    /**
    * Return whether this Decimal is equal to another Decimal.
    *
    * @param d: the Decimal for comparison.
    *
    * @return: true if this Decimal is equal to @p d, otherwise false.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public operator func ==(d: Decimal): Bool
    
    /**
    * Return whether this Decimal is not equal to another Decimal.
    *
    * @param d: the Decimal for comparison.
    *
    * @return: true if this Decimal is not equal to @p d, otherwise false.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public operator func !=(d: Decimal): Bool
    
    /**
    * Compare the relationship between this Decimal and another Decimal.
    *
    * @param d: the Decimal for comparison
    *
    * @return: the relationship between two Decimal instances.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func compare(d: Decimal): Ordering
    
    @!APILevel[12, atomicservice : true]
    public operator func **(n: Int64): Decimal
    
    /**
    * Return a Decimal, whose value is (this ^ {@p n}), 
    * with the precision and roundingMode.
    *  
    * @param n: power to raise this Decimal to.
    * @param precision: transfer decimal value to precision.
    * @param roundingMode: transfer the value to roundingMode.
    * 
    * @return: the result of (this ^ {@p n}) rounded according to the precision and roundingMode. 
    * 
    * @throws OverflowException if the target Decimal is not in the range of 
    * [-(maxValue(precision) * (10 ^ {Int32.MAX})), maxValue(precision) * (10 ^ {Int32.MAX})]
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func powWithPrecision(n: Int64, precision: Int64, roundingMode!: RoundingMode = RoundingMode.HalfEven): Decimal
    
    /**
    * Returns a Decimal, whose value is the square root of this Decimal.
    * Default precision is 0.
    * If the result is a finite decimal, the specific precision is 
    * determined by the calculation result.
    * If the result is an infinite decimal, the specific precision 
    * complies with IEEE decimal128, precision is 34 and
    * roundingMode is HalfEven. 
    * 
    * @return: the result of sqrt(this) rounded according to @p precision and @p roundingMode.
    * 
    * @throws IllegalArgumentException if this < 0.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func sqrtWithPrecision(precision: Int64, roundingMode!: RoundingMode = RoundingMode.HalfEven): Decimal
    
    /**
    * Obtain the hash value of this Decimal.
    * Decimal hashcode is based on bigint hashcode and decimal scale value.
    *
    * @return: hash value of this Decimal.
    *
    * @since 0.46.1
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    /**
    * Obtain the string representation of this Decimal.
    * The return string is the non-exponential representation of this Decimal.
    *
    * @return: the string representation of this Decimal.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toString(): String
    
    /**
    * Return a Decimal, whose scale is specified by @p newScale,  
    * equal to this Decimal rounded according to roundingMode @p rm.
    * Assuming that this Decimal is (n * (10 ^ {- scale})) and we call this.reScale(newScale, rm),
    * the return Decimal is (m * (10 ^ {- newScale})) and satisfies: 
    * (n * (10 ^ {- scale})) = (m * (10 ^ {- newScale})).
    * So, m = Int(n * (10 ^ {- scale + newScale})) rounded according to rm.
    *  
    * @param newScale: new scale of the Decimal; 
    * @param rm: The rounding mode to apply. 
    * 
    * @return: a Decimal, whose scale is the specified value, rounded according to @p rm.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func reScale(newScale: Int32, roundingMode!: RoundingMode = HalfEven): Decimal
    
    /**
    * Return a Decimal, which is numerically equal to this one 
    * but with any trailing zeros removed from the representation.
    * For example, if this Decimal is (100 * (10 ^ {- 2})), the result is (1 * (10 ^ {0})).
    *
    * @return: a numerically equal Decimal with any trailing zeros removed. 
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func removeTrailingZeros(): Decimal
    
    /**
    * Return a Decimal, whose value part equals to one and scale equals to this.scale.
    * 
    * @return: a Decimal, whose value = 1, and scale = this.scale.  
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func scaleUnit(): Decimal
    
    /**
    * Justify whether this Decimal is an integer.
    * 
    * @return: true if this Decimal is an integer, otherwise false.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func isInteger(): Bool
    
    /**
    * Convert this Decimal to an Int8. 
    * Any fractional part of this Decimal will be discarded, 
    * the integer part of this Decimal will be remained.
    * If the integer part of this Decimal is too big to fit in an Int8, 
    * handling the behavior of integer overflow according to @p overflowHandling.
    * The default handling strategy is Throwing OverflowException.
    * 
    * @return: this Decimal to Int8.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toInt8(overflowHandling!: OverflowStrategy = Throwing): Int8
    
    /**
    * Convert this Decimal to an Int16. 
    * Any fractional part of this Decimal will be discarded, 
    * the integer part of this Decimal will be remained.
    * If the integer part of this Decimal is too big to fit in an Int16, 
    * handling the behavior of integer overflow according to @p overflowHandling.
    * The default handling strategy is Throwing OverflowException.
    * 
    * @return: this Decimal to Int16.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toInt16(overflowHandling!: OverflowStrategy = Throwing): Int16
    
    /**
    * Convert this Decimal to an Int32. 
    * Any fractional part of this Decimal will be discarded, 
    * the integer part of this Decimal will be remained.
    * If the integer part of this Decimal is too big to fit in an Int32, 
    * handling the behavior of integer overflow according to @p overflowHandling.
    * The default handling strategy is Throwing OverflowException.
    * 
    * @return: this Decimal to Int32.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toInt32(overflowHandling!: OverflowStrategy = Throwing): Int32
    
    /**
    * Convert this Decimal to an Int64. 
    * Any fractional part of this Decimal will be discarded, 
    * the integer part of this Decimal will be remained.
    * If the integer part of this Decimal is too big to fit in an Int64, 
    * handling the behavior of integer overflow according to @p overflowHandling.
    * The default handling strategy is Throwing OverflowException.
    * 
    * @return: this Decimal to Int64.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toInt64(overflowHandling!: OverflowStrategy = Throwing): Int64
    
    /**
    * Convert this Decimal to an IntNative. 
    * Any fractional part of this Decimal will be discarded, 
    * the integer part of this Decimal will be remained.
    * If the integer part of this Decimal is too big to fit in an IntNative, 
    * handling the behavior of integer overflow according to @p overflowHandling.
    * The default handling strategy is Throwing OverflowException.
    * 
    * @return: this Decimal to IntNative.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toIntNative(overflowHandling!: OverflowStrategy = Throwing): IntNative
    
    /**
    * Convert this Decimal to an UInt8. 
    * Any fractional part of this Decimal will be discarded, 
    * the integer part of this Decimal will be remained.
    * If the integer part of this Decimal is too big to fit in an UInt8, 
    * handling the behavior of integer overflow according to @p overflowHandling.
    * The default handling strategy is Throwing OverflowException.
    * 
    * @return: this Decimal to UInt8.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toUInt8(overflowHandling!: OverflowStrategy = Throwing): UInt8
    
    /**
    * Convert this Decimal to an UInt16. 
    * Any fractional part of this Decimal will be discarded, 
    * the integer part of this Decimal will be remained.
    * If the integer part of this Decimal is too big to fit in an UInt16, 
    * handling the behavior of integer overflow according to @p overflowHandling.
    * The default handling strategy is Throwing OverflowException.
    * 
    * @return: this Decimal to UInt16.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toUInt16(overflowHandling!: OverflowStrategy = Throwing): UInt16
    
    /**
    * Convert this Decimal to an UInt32. 
    * Any fractional part of this Decimal will be discarded, 
    * the integer part of this Decimal will be remained.
    * If the integer part of this Decimal is too big to fit in an UInt32, 
    * handling the behavior of integer overflow according to @p overflowHandling.
    * The default handling strategy is Throwing OverflowException.
    * 
    * @return: this Decimal to UInt32.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toUInt32(overflowHandling!: OverflowStrategy = Throwing): UInt32
    
    /**
    * Convert this Decimal to an UInt64. 
    * Any fractional part of this Decimal will be discarded, 
    * the integer part of this Decimal will be remained.
    * If the integer part of this Decimal is too big to fit in an UInt64, 
    * handling the behavior of integer overflow according to @p overflowHandling.
    * The default handling strategy is Throwing OverflowException.
    * 
    * @return: this Decimal to UInt64.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toUInt64(overflowHandling!: OverflowStrategy = Throwing): UInt64
    
    /**
    * Convert this Decimal to an UIntNative. 
    * Any fractional part of this Decimal will be discarded, 
    * the integer part of this Decimal will be remained.
    * If the integer part of this Decimal is too big to fit in an UIntNative, 
    * handling the behavior of integer overflow according to @p overflowHandling.
    * The default handling strategy is Throwing OverflowException.
    * 
    * @return: this Decimal to UIntNative.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toUIntNative(overflowHandling!: OverflowStrategy = Throwing): UIntNative
    
    /**
    * Convert this Decimal to an UInt16. 
    * Any fractional part of this Decimal will be discarded.
    * 
    * @return: this Decimal to BigInt.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toBigInt(): BigInt
    
    /**
    * Convert this Decimal to a Float16. 
    * If this Decimal is too big to fit in an Float16, 
    * the result is inf, if this Decimal > 0; or -inf, if this Decimal < 0.
    * 
    * @return: this Decimal to Float16.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toFloat16(): Float16
    
    /**
    * Convert this Decimal to a Float32. 
    * If this Decimal is too big to fit in an Float32, 
    * the result is inf, if this Decimal > 0; or -inf, if this Decimal < 0.
    * 
    * @return: this Decimal to Float32.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toFloat32(): Float32
    
    /**
    * Convert this Decimal to a Float64. 
    * If this Decimal is too big to fit in an Float64, 
    * the result is inf, if this Decimal > 0; or -inf, if this Decimal < 0.
    * 
    * @return: this Decimal to Float64.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toFloat64(): Float64
    
    /**
    * Convert this Decimal to engineering notation format. 
    * For example, Decimal.parse("0.00012").toEngString() = 120 * (10 ^ {-6}). 
    * 
    * @return: engineering notation for this Decimal.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toEngString(): String
    
    /**
    * Convert this Decimal scientific notation format.
    * For example, Decimal.parse("0.00012").toSciString() = 1.2 * (10 ^ {-4})
    * 
    * @return: scientific notation for this Decimal.
    *
    * @since 0.46.1
    */
    @!APILevel[12, atomicservice : true]
    public func toSciString(): String
}

/**
* Return a Decimal, whose value is the absolute value of this Decimal,
* and whose scale is this.scale.
*
* @param d: the value need to do abs.
*
* @return: a Decimal equals to this.abs().
*
* @since 0.46.1
*/
@!APILevel[12, atomicservice : true]
public func abs(d: Decimal): Decimal

/**
* Return a Decimal rounded to adjacent integer using the specified rounding mode.
* 
* @param d: the value need to do round;
* @param roundingMode: transfer the value to roundingMode.
* 
* @return: a Decimal rounded according to the rounding mode.
* 
* @throws OverflowException if the target Decimal is not in the range of 
* [-(maxValue(precision) * (10 ^ {Int32.MAX})), maxValue(precision) * (10 ^ {Int32.MAX})]
*/
@!APILevel[12, atomicservice : true]
public func round(d: Decimal, roundingMode!: RoundingMode = RoundingMode.HalfEven): Decimal

/**
* Returns a Decimal, whose value is the square root of this Decimal.
* Default precision is 0.
* If the result is a finite decimal, the specific precision is 
* determined by the calculation result.
* If the result is an infinite decimal, the specific precision 
* complies with IEEE decimal128, precision is 34 and
* roundingMode is HalfEven. 
*
* @param d: the value need to do sqrt.
* 
* @return: the result of sqrt(this) rounded.
* 
* @throws IllegalArgumentException if this < 0.
*
* @since 0.46.1
*/
@!APILevel[12, atomicservice : true]
public func sqrt(d: Decimal): Decimal

extend Decimal <: Number<Decimal> {
}

/**
* Three strategies to handle overflows when converting BigDecimal to integer:
* - Throwing: Throwing Overflow exception;
* - Wrapping: Wrapping around at the numeric bounds of the type;
* - Saturating: Saturating at the numeric bounds of the type
*/
@!APILevel[12, atomicservice : true]
public enum OverflowStrategy <: Equatable<OverflowStrategy> & ToString {
    @!APILevel[12, atomicservice : true]
    Saturating |
    @!APILevel[12, atomicservice : true]
    Throwing |
    @!APILevel[12, atomicservice : true]
    Wrapping
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: OverflowStrategy): Bool
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

extend Decimal <: Parsable<Decimal> {
    @!APILevel[12, atomicservice : true]
    public static func parse(value: String): Decimal
    
    @!APILevel[12, atomicservice : true]
    public static func tryParse(value: String): ?Decimal
}

extend Decimal <: Formattable {
    @!APILevel[12, atomicservice : true]
    public func format(fmt: String): String
}

