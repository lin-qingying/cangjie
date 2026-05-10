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

    prop seed: UInt64

    func next(bits: UInt64): UInt64

    func nextBits(bits: UInt64): UInt64

    func nextBool: Bool

    func nextUInt8: UInt8

    func nextUInt16: UInt16

    func nextUInt32: UInt32

    func nextUInt64: UInt64

    func nextInt8: Int8

    func nextInt16: Int16

    func nextInt32: Int32

    func nextInt64: Int64

    func nextUInt8(upper: UInt8): UInt8

    func nextUInt16(upper: UInt16): UInt16

    func nextUInt32(upper: UInt32): UInt32

    func nextUInt64(upper: UInt64): UInt64

    func nextInt8(upper: Int8): Int8

    func nextInt16(upper: Int16): Int16

    func nextInt32(upper: Int32): Int32

    func nextInt64(upper: Int64): Int64

    func nextUInt8s(array: std.core.Array<UInt8>): std.core.Array<UInt8>

    func nextBytes(bytes: std.core.Array<UInt8>): Unit

    func nextBytes(length: Int32): std.core.Array<UInt8>

    func nextFloat16: Float16

    func nextFloat32: Float32

    func nextFloat64: Float64

    func nextGaussianFloat16(mean: Float16, sigma: Float16): Float16

    func nextGaussianFloat32(mean: Float32, sigma: Float32): Float32

    func nextGaussianFloat64(mean: Float64, sigma: Float64): Float64
}
