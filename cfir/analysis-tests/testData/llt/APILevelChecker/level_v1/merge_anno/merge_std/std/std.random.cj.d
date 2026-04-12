// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.



package std.random
import std.math.*


/**
* Random class used for random manipulations.
* @since 0.16.5
*/
@!APILevel[12, atomicservice : true]
public class Random {
    /**
    * Create a new Random object.
    *
    * Default constructor.
    *
    * @since 0.16.5
    */
    @!APILevel[12, atomicservice : true]
    public init()
    
    /**
    * Create a new Random object.
    *
    * @param seed a seed of type UInt64.
    *
    * @since 0.16.5
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public init(seed: UInt64)
    
    /**
    * Set the size of Seed.
    *
    * @param seed a seed of type UInt64.
    */
    @!APILevel[12, atomicservice : true]
    public prop seed: UInt64
    
    /**
    * Get random of UInt64.
    *
    * @param bits bits of type UInt64.
    * @return Parameters of UInt64.
    *
    * @since 0.16.5
    *
    * @throws IllegalArgumentException if bits greater than 64.
    */
    @Deprecated["Use member function `public func nextBits(bits: UInt64): UInt64` instead."]
    @!APILevel[12, atomicservice : true]
    public func next(bits: UInt64): UInt64
    
    /**
    * Get random of UInt64.
    *
    * @param bits bits of type UInt64.
    * @return Parameters of UInt64.
    *
    * @throws IllegalArgumentException if bits greater than 64 or equal to 0.
    */
    @!APILevel[12, atomicservice : true]
    public func nextBits(bits: UInt64): UInt64
    
    /**
    * Get random of Bool.
    *
    * @return bool a random bool.
    *
    * @since 0.16.5
    */
    @!APILevel[12, atomicservice : true]
    public func nextBool(): Bool
    
    /**
    * Get random of UInt8.
    *
    * @return Parameters of UInt8.
    *
    * @since 0.16.5
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func nextUInt8(): UInt8
    
    /**
    * Get random of UInt16.
    *
    * @return Parameters of UInt16.
    *
    * @since 0.16.5
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func nextUInt16(): UInt16
    
    /**
    * Get random of UInt32.
    *
    * @return Parameters of UInt32.
    *
    * @since 0.16.5
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func nextUInt32(): UInt32
    
    /**
    * Get random of UInt64.
    *
    * @return Parameters of UInt64.
    *
    * @since 0.16.5
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func nextUInt64(): UInt64
    
    /**
    * Get random of Int8.
    *
    * @return Parameters of Int8.
    *
    * @since 0.16.5
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func nextInt8(): Int8
    
    /**
    * Get random of Int16.
    *
    * @return Parameters of Int16.
    *
    * @since 0.16.5
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func nextInt16(): Int16
    
    /**
    * Get random of Int32.
    *
    * @return Parameters of Int32.
    *
    * @since 0.16.5
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func nextInt32(): Int32
    
    /**
    * Get random of Int64.
    *
    * @return Parameters of Int64.
    *
    * @since 0.16.5
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func nextInt64(): Int64
    
    /**
    * Get random of UInt8.An exception is thrown when upper is less than or equal to 0.
    * upper is excluded from the results
    *
    * @param upper UInt8 type upper.
    * @return Parameters of UInt8.
    *
    * @since 0.16.5
    *
    * @throws IllegalArgumentException if upper is equal to zero.
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func nextUInt8(upper: UInt8): UInt8
    
    /**
    * Get random of UInt16.An exception is thrown when upper is less than or equal to 0.
    * upper is excluded from the results
    *
    * @param upper UInt16 type upper.
    * @return Parameters of UInt16.
    *
    * @since 0.16.5
    *
    * @throws IllegalArgumentException if upper is equal to zero.
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func nextUInt16(upper: UInt16): UInt16
    
    /**
    * Get random of UInt32.An exception is thrown when upper is less than or equal to 0.
    * upper is excluded from the results
    *
    * @param upper UInt32 type upper.
    * @return Parameters of UInt32.
    *
    * @since 0.16.5
    *
    * @throws IllegalArgumentException if upper is equal to zero.
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func nextUInt32(upper: UInt32): UInt32
    
    /**
    * Get random of UInt64.An exception is thrown when upper is less than or equal to 0.
    * upper is excluded from the results
    *
    * @param upper UInt64 type upper.
    * @return Parameters of UInt64.
    *
    * @since 0.16.5
    *
    * @throws IllegalArgumentException if upper is equal to zero.
    */
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public func nextUInt64(upper: UInt64): UInt64
    
    /**
    * Get random of Int8.An exception is thrown when upper is less than or equal to 0.
    * upper is excluded from the results
    *
    * @param upper Int8 type upper.
    * @return Parameters of Int8.
    *
    * @since 0.16.5
    *
    * @throws IllegalArgumentException if upper <= 0.
    */
    @!APILevel[12, atomicservice : true]
    public func nextInt8(upper: Int8): Int8
    
    /**
    * Get random of Int16.An exception is thrown when upper is less than or equal to 0.
    * upper is excluded from the results
    *
    * @param upper Int16 type upper.
    * @return Parameters of Int16.
    *
    * @since 0.16.5
    *
    * @throws IllegalArgumentException if upper <= 0.
    */
    @!APILevel[12, atomicservice : true]
    public func nextInt16(upper: Int16): Int16
    
    /**
    * Get random of Int32.An exception is thrown when upper is less than or equal to 0.
    * upper is excluded from the results
    *
    * @param upper Int32 type upper.
    * @return Parameters of Int32.
    *
    * @since 0.16.5
    *
    * @throws IllegalArgumentException if upper <= 0.
    */
    @!APILevel[12, atomicservice : true]
    public func nextInt32(upper: Int32): Int32
    
    /**
    * Get random of Int64.An exception is thrown when upper is less than or equal to 0.
    * upper is excluded from the results
    *
    * @param upper Int64 type upper.
    * @return Parameters of Int64.
    *
    * @since 0.16.5
    *
    * @throws IllegalArgumentException if upper <= 0.
    */
    @!APILevel[12, atomicservice : true]
    public func nextInt64(upper: Int64): Int64
    
    /**
    * Get random of UInt8s.
    *
    * @return Parameters of UInt8s[].
    *
    * @since 0.16.5
    */
    @Deprecated["Use member function `public func nextBytes(arr: Array<Byte>): Unit` instead."]
    @!APILevel[12, atomicservice : true]
    public func nextUInt8s(array: Array<UInt8>): Array<UInt8>
    
    /**
    * Fill the byte array with random bytes.
    */
    @!APILevel[12, atomicservice : true]
    public func nextBytes(bytes: Array<Byte>): Unit
    
    /**
    * Generate a byte array with random bytes.
    */
    @!APILevel[12, atomicservice : true]
    public func nextBytes(length: Int32): Array<Byte>
    
    /**
    * Get random of Float16.
    *
    * @return Parameters of Float16.
    *
    * @since 0.16.5
    */
    @!APILevel[12, atomicservice : true]
    public func nextFloat16(): Float16
    
    /**
    * Get random of Float32.
    *
    * @return Parameters of Float32.
    *
    * @since 0.16.5
    */
    @!APILevel[12, atomicservice : true]
    public func nextFloat32(): Float32
    
    /**
    * Get random of Float64.
    *
    * @return Parameters of Float64.
    *
    * @since 0.16.5
    */
    @!APILevel[12, atomicservice : true]
    public func nextFloat64(): Float64
    
    /**
    * Obtaining Gaussian (normal) distribution random values.
    *
    * @param mean: mean value
    * @param sigma: standard deviation
    * @return Float16 random value
    *
    * @since 0.40.2
    */
    @!APILevel[12, atomicservice : true]
    public func nextGaussianFloat16(mean!: Float16 = 0.0, sigma!: Float16 = 1.0): Float16
    
    /**
    * Obtaining Gaussian (normal) distribution random values.
    *
    * @param mean: mean value
    * @param sigma: standard deviation
    * @return Float32 random value
    *
    * @since 0.40.2
    */
    @!APILevel[12, atomicservice : true]
    public func nextGaussianFloat32(mean!: Float32 = 0.0, sigma!: Float32 = 1.0): Float32
    
    /**
    * Obtaining Gaussian (normal) distribution random values.
    *
    * @param mean: mean value
    * @param sigma: standard deviation
    * @return Float64 random value
    *
    * @since 0.40.2
    */
    @!APILevel[12, atomicservice : true]
    public func nextGaussianFloat64(mean!: Float64 = 0.0, sigma!: Float64 = 1.0): Float64
}

