// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.random

import std.math.*
import std.core.*

class Random {
    var mt: std.core.Array<UInt64>

    var mti: Int64

    var nextGaussian: std.core.Option<Float64>

    var _seed: UInt64

    init() { /* compiled code */ }

    init(seed: UInt64) { /* compiled code */ }

    prop seed: UInt64 {
        get() { /* compiled code */ }
    }

    func next(bits: UInt64): UInt64 { /* compiled code */ }

    func nextBits(bits: UInt64): UInt64 { /* compiled code */ }

    func nextBool(): Bool { /* compiled code */ }

    func nextUInt8(): UInt8 { /* compiled code */ }

    func nextUInt16(): UInt16 { /* compiled code */ }

    func nextUInt32(): UInt32 { /* compiled code */ }

    func nextUInt64(): UInt64 { /* compiled code */ }

    func nextInt8(): Int8 { /* compiled code */ }

    func nextInt16(): Int16 { /* compiled code */ }

    func nextInt32(): Int32 { /* compiled code */ }

    func nextInt64(): Int64 { /* compiled code */ }

    func nextUInt8(upper: UInt8): UInt8 { /* compiled code */ }

    func nextUInt16(upper: UInt16): UInt16 { /* compiled code */ }

    func nextUInt32(upper: UInt32): UInt32 { /* compiled code */ }

    func nextUInt64(upper: UInt64): UInt64 { /* compiled code */ }

    func nextInt8(upper: Int8): Int8 { /* compiled code */ }

    func nextInt16(upper: Int16): Int16 { /* compiled code */ }

    func nextInt32(upper: Int32): Int32 { /* compiled code */ }

    func nextInt64(upper: Int64): Int64 { /* compiled code */ }

    func nextUInt8s(array: std.core.Array<UInt8>): std.core.Array<UInt8> { /* compiled code */ }

    func nextBytes(bytes: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func nextBytes(length: Int32): std.core.Array<UInt8> { /* compiled code */ }

    func nextFloat16(): Float16 { /* compiled code */ }

    func nextFloat32(): Float32 { /* compiled code */ }

    func nextFloat64(): Float64 { /* compiled code */ }

    func nextGaussianFloat16(mean: Float16, sigma: Float16): Float16 { /* compiled code */ }

    func nextGaussianFloat32(mean: Float32, sigma: Float32): Float32 { /* compiled code */ }

    func nextGaussianFloat64(mean: Float64, sigma: Float64): Float64 { /* compiled code */ }
}
