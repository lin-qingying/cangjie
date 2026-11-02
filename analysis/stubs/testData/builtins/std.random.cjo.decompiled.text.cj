// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.random

import std.math.*
import std.core.*

public class Random {
    private var mt: std.core.Array<UInt64>

    private var mti: Int64

    private var nextGaussian: std.core.Option<Float64>

    private var _seed: UInt64

    init() { /* compiled code */ }

    init(seed: UInt64) { /* compiled code */ }

    public prop seed: UInt64 {
        get() { /* compiled code */ }
    }

    @Deprecated["Use member function `public func nextBits(bits: UInt64): UInt64` instead."]
    public func next(bits: UInt64): UInt64 { /* compiled code */ }

    public func nextBits(bits: UInt64): UInt64 { /* compiled code */ }

    public func nextBool(): Bool { /* compiled code */ }

    public func nextUInt8(): UInt8 { /* compiled code */ }

    public func nextUInt16(): UInt16 { /* compiled code */ }

    public func nextUInt32(): UInt32 { /* compiled code */ }

    public func nextUInt64(): UInt64 { /* compiled code */ }

    public func nextInt8(): Int8 { /* compiled code */ }

    public func nextInt16(): Int16 { /* compiled code */ }

    public func nextInt32(): Int32 { /* compiled code */ }

    public func nextInt64(): Int64 { /* compiled code */ }

    public func nextUInt8(upper: UInt8): UInt8 { /* compiled code */ }

    public func nextUInt16(upper: UInt16): UInt16 { /* compiled code */ }

    public func nextUInt32(upper: UInt32): UInt32 { /* compiled code */ }

    public func nextUInt64(upper: UInt64): UInt64 { /* compiled code */ }

    public func nextInt8(upper: Int8): Int8 { /* compiled code */ }

    public func nextInt16(upper: Int16): Int16 { /* compiled code */ }

    public func nextInt32(upper: Int32): Int32 { /* compiled code */ }

    public func nextInt64(upper: Int64): Int64 { /* compiled code */ }

    @Deprecated["Use member function `public func nextBytes(arr: Array<Byte>): Unit` instead."]
    public func nextUInt8s(array: std.core.Array<UInt8>): std.core.Array<UInt8> { /* compiled code */ }

    public func nextBytes(bytes: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public func nextBytes(length: Int32): std.core.Array<UInt8> { /* compiled code */ }

    public func nextFloat16(): Float16 { /* compiled code */ }

    public func nextFloat32(): Float32 { /* compiled code */ }

    public func nextFloat64(): Float64 { /* compiled code */ }

    public func nextGaussianFloat16(mean!: Float16 = COMPILED_CODE /* compiled code */, sigma!: Float16 = COMPILED_CODE /* compiled code */): Float16 { /* compiled code */ }

    public func nextGaussianFloat32(mean!: Float32 = COMPILED_CODE /* compiled code */, sigma!: Float32 = COMPILED_CODE /* compiled code */): Float32 { /* compiled code */ }

    public func nextGaussianFloat64(mean!: Float64 = COMPILED_CODE /* compiled code */, sigma!: Float64 = COMPILED_CODE /* compiled code */): Float64 { /* compiled code */ }
}
