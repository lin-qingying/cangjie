// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.overflow

import std.core.*

let isNative64: Bool

interface CarryingOp {
    func carryingAdd(y: T): (Bool, T)

    func carryingSub(y: T): (Bool, T)

    func carryingMul(y: T): (Bool, T)

    func carryingDiv(y: T): (Bool, T)

    func carryingMod(y: T): (Bool, T)

    func carryingInc: (Bool, T)

    func carryingDec: (Bool, T)

    func carryingNeg: (Bool, T)

    func carryingShl(y: UInt64): (Bool, T)

    func carryingShr(y: UInt64): (Bool, T)
}

interface CarryingPow {
    func carryingPow(y: UInt64): (Bool, Int64)
}

extend Extend <: CarryingOp {
    func carryingAdd(y: Int8): (Bool, Int8)

    func carryingSub(y: Int8): (Bool, Int8)

    func carryingMul(y: Int8): (Bool, Int8)

    func carryingDiv(y: Int8): (Bool, Int8)

    func carryingMod(y: Int8): (Bool, Int8)

    func carryingInc: (Bool, Int8)

    func carryingDec: (Bool, Int8)

    func carryingNeg: (Bool, Int8)

    func carryingShl(y: UInt64): (Bool, Int8)

    func carryingShr(y: UInt64): (Bool, Int8)
}

extend Extend <: CarryingOp {
    func carryingAdd(y: Int16): (Bool, Int16)

    func carryingSub(y: Int16): (Bool, Int16)

    func carryingMul(y: Int16): (Bool, Int16)

    func carryingDiv(y: Int16): (Bool, Int16)

    func carryingMod(y: Int16): (Bool, Int16)

    func carryingInc: (Bool, Int16)

    func carryingDec: (Bool, Int16)

    func carryingNeg: (Bool, Int16)

    func carryingShl(y: UInt64): (Bool, Int16)

    func carryingShr(y: UInt64): (Bool, Int16)
}

extend Extend <: CarryingOp {
    func carryingAdd(y: Int32): (Bool, Int32)

    func carryingSub(y: Int32): (Bool, Int32)

    func carryingMul(y: Int32): (Bool, Int32)

    func carryingDiv(y: Int32): (Bool, Int32)

    func carryingMod(y: Int32): (Bool, Int32)

    func carryingInc: (Bool, Int32)

    func carryingDec: (Bool, Int32)

    func carryingNeg: (Bool, Int32)

    func carryingShl(y: UInt64): (Bool, Int32)

    func carryingShr(y: UInt64): (Bool, Int32)
}

extend Extend <: CarryingOp & CarryingPow {
    func carryingAdd(y: Int64): (Bool, Int64)

    func carryingSub(y: Int64): (Bool, Int64)

    func carryingMul(y: Int64): (Bool, Int64)

    func carryingDiv(y: Int64): (Bool, Int64)

    func carryingMod(y: Int64): (Bool, Int64)

    func carryingInc: (Bool, Int64)

    func carryingDec: (Bool, Int64)

    func carryingNeg: (Bool, Int64)

    func carryingShl(y: UInt64): (Bool, Int64)

    func carryingShr(y: UInt64): (Bool, Int64)

    func carryingPow(y: UInt64): (Bool, Int64)
}

extend Extend <: CarryingOp {
    func carryingAdd(y: IntNative): (Bool, IntNative)

    func carryingSub(y: IntNative): (Bool, IntNative)

    func carryingMul(y: IntNative): (Bool, IntNative)

    func carryingDiv(y: IntNative): (Bool, IntNative)

    func carryingMod(y: IntNative): (Bool, IntNative)

    func carryingInc: (Bool, IntNative)

    func carryingDec: (Bool, IntNative)

    func carryingNeg: (Bool, IntNative)

    func carryingShl(y: UInt64): (Bool, IntNative)

    func carryingShr(y: UInt64): (Bool, IntNative)
}

extend Extend <: CarryingOp {
    func carryingAdd(y: UInt8): (Bool, UInt8)

    func carryingSub(y: UInt8): (Bool, UInt8)

    func carryingMul(y: UInt8): (Bool, UInt8)

    func carryingDiv(y: UInt8): (Bool, UInt8)

    func carryingMod(y: UInt8): (Bool, UInt8)

    func carryingInc: (Bool, UInt8)

    func carryingDec: (Bool, UInt8)

    func carryingNeg: (Bool, UInt8)

    func carryingShl(y: UInt64): (Bool, UInt8)

    func carryingShr(y: UInt64): (Bool, UInt8)
}

extend Extend <: CarryingOp {
    func carryingAdd(y: UInt16): (Bool, UInt16)

    func carryingSub(y: UInt16): (Bool, UInt16)

    func carryingMul(y: UInt16): (Bool, UInt16)

    func carryingDiv(y: UInt16): (Bool, UInt16)

    func carryingMod(y: UInt16): (Bool, UInt16)

    func carryingInc: (Bool, UInt16)

    func carryingDec: (Bool, UInt16)

    func carryingNeg: (Bool, UInt16)

    func carryingShl(y: UInt64): (Bool, UInt16)

    func carryingShr(y: UInt64): (Bool, UInt16)
}

extend Extend <: CarryingOp {
    func carryingAdd(y: UInt32): (Bool, UInt32)

    func carryingSub(y: UInt32): (Bool, UInt32)

    func carryingMul(y: UInt32): (Bool, UInt32)

    func carryingDiv(y: UInt32): (Bool, UInt32)

    func carryingMod(y: UInt32): (Bool, UInt32)

    func carryingInc: (Bool, UInt32)

    func carryingDec: (Bool, UInt32)

    func carryingNeg: (Bool, UInt32)

    func carryingShl(y: UInt64): (Bool, UInt32)

    func carryingShr(y: UInt64): (Bool, UInt32)
}

extend Extend <: CarryingOp {
    func carryingAdd(y: UInt64): (Bool, UInt64)

    func carryingSub(y: UInt64): (Bool, UInt64)

    func carryingMul(y: UInt64): (Bool, UInt64)

    func carryingDiv(y: UInt64): (Bool, UInt64)

    func carryingMod(y: UInt64): (Bool, UInt64)

    func carryingInc: (Bool, UInt64)

    func carryingDec: (Bool, UInt64)

    func carryingNeg: (Bool, UInt64)

    func carryingShl(y: UInt64): (Bool, UInt64)

    func carryingShr(y: UInt64): (Bool, UInt64)
}

extend Extend <: CarryingOp {
    func carryingAdd(y: UIntNative): (Bool, UIntNative)

    func carryingSub(y: UIntNative): (Bool, UIntNative)

    func carryingMul(y: UIntNative): (Bool, UIntNative)

    func carryingDiv(y: UIntNative): (Bool, UIntNative)

    func carryingMod(y: UIntNative): (Bool, UIntNative)

    func carryingInc: (Bool, UIntNative)

    func carryingDec: (Bool, UIntNative)

    func carryingNeg: (Bool, UIntNative)

    func carryingShl(y: UInt64): (Bool, UIntNative)

    func carryingShr(y: UInt64): (Bool, UIntNative)
}

interface CheckedOp {
    func checkedAdd(y: T): std.core.Option<T>

    func checkedSub(y: T): std.core.Option<T>

    func checkedMul(y: T): std.core.Option<T>

    func checkedDiv(y: T): std.core.Option<T>

    func checkedMod(y: T): std.core.Option<T>

    func checkedInc: std.core.Option<T>

    func checkedDec: std.core.Option<T>

    func checkedNeg: std.core.Option<T>

    func checkedShl(y: UInt64): std.core.Option<T>

    func checkedShr(y: UInt64): std.core.Option<T>
}

interface CheckedPow {
    func checkedPow(y: UInt64): std.core.Option<Int64>
}

func checkedShl(x: Int8, y: UInt64): std.core.Option<Int8>

func checkedShl(x: Int16, y: UInt64): std.core.Option<Int16>

func checkedShl(x: Int32, y: UInt64): std.core.Option<Int32>

func checkedShl(x: Int64, y: UInt64): std.core.Option<Int64>

func checkedShl(x: IntNative, y: UInt64): std.core.Option<IntNative>

func checkedShl(x: UInt8, y: UInt64): std.core.Option<UInt8>

func checkedShl(x: UInt16, y: UInt64): std.core.Option<UInt16>

func checkedShl(x: UInt32, y: UInt64): std.core.Option<UInt32>

func checkedShl(x: UInt64, y: UInt64): std.core.Option<UInt64>

func checkedShl(x: UIntNative, y: UInt64): std.core.Option<UIntNative>

func checkedShr(x: Int8, y: UInt64): std.core.Option<Int8>

func checkedShr(x: Int16, y: UInt64): std.core.Option<Int16>

func checkedShr(x: Int32, y: UInt64): std.core.Option<Int32>

func checkedShr(x: Int64, y: UInt64): std.core.Option<Int64>

func checkedShr(x: IntNative, y: UInt64): std.core.Option<IntNative>

func checkedShr(x: UInt8, y: UInt64): std.core.Option<UInt8>

func checkedShr(x: UInt16, y: UInt64): std.core.Option<UInt16>

func checkedShr(x: UInt32, y: UInt64): std.core.Option<UInt32>

func checkedShr(x: UInt64, y: UInt64): std.core.Option<UInt64>

func checkedShr(x: UIntNative, y: UInt64): std.core.Option<UIntNative>

func checkedAdd(x: T, y: T): std.core.Option<T>

func checkedSub(x: T, y: T): std.core.Option<T>

func checkedMul(x: T, y: T): std.core.Option<T>

func checkedDiv(x: T, y: T): std.core.Option<T>

func checkedMod(x: T, y: T): std.core.Option<T>

func checkedPow(x: Int64, y: UInt64): std.core.Option<Int64>

func checkedInc(x: T): std.core.Option<T>

func checkedDec(x: T): std.core.Option<T>

func checkedNeg(x: T): std.core.Option<T>

extend Extend <: CheckedOp {
    func checkedAdd(y: Int8): std.core.Option<Int8>

    func checkedSub(y: Int8): std.core.Option<Int8>

    func checkedMul(y: Int8): std.core.Option<Int8>

    func checkedDiv(y: Int8): std.core.Option<Int8>

    func checkedMod(y: Int8): std.core.Option<Int8>

    func checkedInc: std.core.Option<Int8>

    func checkedDec: std.core.Option<Int8>

    func checkedNeg: std.core.Option<Int8>

    func checkedShl(y: UInt64): std.core.Option<Int8>

    func checkedShr(y: UInt64): std.core.Option<Int8>
}

extend Extend <: CheckedOp {
    func checkedAdd(y: Int16): std.core.Option<Int16>

    func checkedSub(y: Int16): std.core.Option<Int16>

    func checkedMul(y: Int16): std.core.Option<Int16>

    func checkedDiv(y: Int16): std.core.Option<Int16>

    func checkedMod(y: Int16): std.core.Option<Int16>

    func checkedInc: std.core.Option<Int16>

    func checkedDec: std.core.Option<Int16>

    func checkedNeg: std.core.Option<Int16>

    func checkedShl(y: UInt64): std.core.Option<Int16>

    func checkedShr(y: UInt64): std.core.Option<Int16>
}

extend Extend <: CheckedOp {
    func checkedAdd(y: Int32): std.core.Option<Int32>

    func checkedSub(y: Int32): std.core.Option<Int32>

    func checkedMul(y: Int32): std.core.Option<Int32>

    func checkedDiv(y: Int32): std.core.Option<Int32>

    func checkedMod(y: Int32): std.core.Option<Int32>

    func checkedInc: std.core.Option<Int32>

    func checkedDec: std.core.Option<Int32>

    func checkedNeg: std.core.Option<Int32>

    func checkedShl(y: UInt64): std.core.Option<Int32>

    func checkedShr(y: UInt64): std.core.Option<Int32>
}

extend Extend <: CheckedOp & CheckedPow {
    func checkedAdd(y: Int64): std.core.Option<Int64>

    func checkedSub(y: Int64): std.core.Option<Int64>

    func checkedMul(y: Int64): std.core.Option<Int64>

    func checkedDiv(y: Int64): std.core.Option<Int64>

    func checkedMod(y: Int64): std.core.Option<Int64>

    func checkedPow(y: UInt64): std.core.Option<Int64>

    func checkedInc: std.core.Option<Int64>

    func checkedDec: std.core.Option<Int64>

    func checkedNeg: std.core.Option<Int64>

    func checkedShl(y: UInt64): std.core.Option<Int64>

    func checkedShr(y: UInt64): std.core.Option<Int64>
}

extend Extend <: CheckedOp {
    func checkedAdd(y: UInt8): std.core.Option<UInt8>

    func checkedSub(y: UInt8): std.core.Option<UInt8>

    func checkedMul(y: UInt8): std.core.Option<UInt8>

    func checkedDiv(y: UInt8): std.core.Option<UInt8>

    func checkedMod(y: UInt8): std.core.Option<UInt8>

    func checkedInc: std.core.Option<UInt8>

    func checkedDec: std.core.Option<UInt8>

    func checkedNeg: std.core.Option<UInt8>

    func checkedShl(y: UInt64): std.core.Option<UInt8>

    func checkedShr(y: UInt64): std.core.Option<UInt8>
}

extend Extend <: CheckedOp {
    func checkedAdd(y: UInt16): std.core.Option<UInt16>

    func checkedSub(y: UInt16): std.core.Option<UInt16>

    func checkedMul(y: UInt16): std.core.Option<UInt16>

    func checkedDiv(y: UInt16): std.core.Option<UInt16>

    func checkedMod(y: UInt16): std.core.Option<UInt16>

    func checkedInc: std.core.Option<UInt16>

    func checkedDec: std.core.Option<UInt16>

    func checkedNeg: std.core.Option<UInt16>

    func checkedShl(y: UInt64): std.core.Option<UInt16>

    func checkedShr(y: UInt64): std.core.Option<UInt16>
}

extend Extend <: CheckedOp {
    func checkedAdd(y: UInt32): std.core.Option<UInt32>

    func checkedSub(y: UInt32): std.core.Option<UInt32>

    func checkedMul(y: UInt32): std.core.Option<UInt32>

    func checkedDiv(y: UInt32): std.core.Option<UInt32>

    func checkedMod(y: UInt32): std.core.Option<UInt32>

    func checkedInc: std.core.Option<UInt32>

    func checkedDec: std.core.Option<UInt32>

    func checkedNeg: std.core.Option<UInt32>

    func checkedShl(y: UInt64): std.core.Option<UInt32>

    func checkedShr(y: UInt64): std.core.Option<UInt32>
}

extend Extend <: CheckedOp {
    func checkedAdd(y: UInt64): std.core.Option<UInt64>

    func checkedSub(y: UInt64): std.core.Option<UInt64>

    func checkedMul(y: UInt64): std.core.Option<UInt64>

    func checkedDiv(y: UInt64): std.core.Option<UInt64>

    func checkedMod(y: UInt64): std.core.Option<UInt64>

    func checkedInc: std.core.Option<UInt64>

    func checkedDec: std.core.Option<UInt64>

    func checkedNeg: std.core.Option<UInt64>

    func checkedShl(y: UInt64): std.core.Option<UInt64>

    func checkedShr(y: UInt64): std.core.Option<UInt64>
}

extend Extend <: CheckedOp {
    func checkedAdd(y: IntNative): std.core.Option<IntNative>

    func checkedSub(y: IntNative): std.core.Option<IntNative>

    func checkedMul(y: IntNative): std.core.Option<IntNative>

    func checkedDiv(y: IntNative): std.core.Option<IntNative>

    func checkedMod(y: IntNative): std.core.Option<IntNative>

    func checkedInc: std.core.Option<IntNative>

    func checkedDec: std.core.Option<IntNative>

    func checkedNeg: std.core.Option<IntNative>

    func checkedShl(y: UInt64): std.core.Option<IntNative>

    func checkedShr(y: UInt64): std.core.Option<IntNative>
}

extend Extend <: CheckedOp {
    func checkedAdd(y: UIntNative): std.core.Option<UIntNative>

    func checkedSub(y: UIntNative): std.core.Option<UIntNative>

    func checkedMul(y: UIntNative): std.core.Option<UIntNative>

    func checkedDiv(y: UIntNative): std.core.Option<UIntNative>

    func checkedMod(y: UIntNative): std.core.Option<UIntNative>

    func checkedInc: std.core.Option<UIntNative>

    func checkedDec: std.core.Option<UIntNative>

    func checkedNeg: std.core.Option<UIntNative>

    func checkedShl(y: UInt64): std.core.Option<UIntNative>

    func checkedShr(y: UInt64): std.core.Option<UIntNative>
}

func saturatingAdd(x: T, y: T): T

func saturatingSub(x: T, y: T): T

func saturatingMul(x: T, y: T): T

func saturatingDiv(x: T, y: T): T

func saturatingMod(x: T, y: T): T

func saturatingPow(x: Int64, y: UInt64): Int64

func saturatingInc(x: T): T

func saturatingNeg(x: T): T

func saturatingDec(x: T): T

interface SaturatingOp {
    func saturatingAdd(y: T): T

    func saturatingSub(y: T): T

    func saturatingMul(y: T): T

    func saturatingDiv(y: T): T

    func saturatingMod(y: T): T

    func saturatingInc: T

    func saturatingDec: T

    func saturatingNeg: T

    func saturatingShl(y: UInt64): T

    func saturatingShr(y: UInt64): T
}

interface SaturatingPow {
    func saturatingPow(y: UInt64): Int64
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: Int8): Int8

    func saturatingSub(y: Int8): Int8

    func saturatingMul(y: Int8): Int8

    func saturatingDiv(y: Int8): Int8

    func saturatingMod(y: Int8): Int8

    func saturatingInc: Int8

    func saturatingDec: Int8

    func saturatingNeg: Int8

    func saturatingShl(y: UInt64): Int8

    func saturatingShr(y: UInt64): Int8
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: Int16): Int16

    func saturatingSub(y: Int16): Int16

    func saturatingMul(y: Int16): Int16

    func saturatingDiv(y: Int16): Int16

    func saturatingMod(y: Int16): Int16

    func saturatingInc: Int16

    func saturatingDec: Int16

    func saturatingNeg: Int16

    func saturatingShl(y: UInt64): Int16

    func saturatingShr(y: UInt64): Int16
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: Int32): Int32

    func saturatingSub(y: Int32): Int32

    func saturatingMul(y: Int32): Int32

    func saturatingDiv(y: Int32): Int32

    func saturatingMod(y: Int32): Int32

    func saturatingInc: Int32

    func saturatingDec: Int32

    func saturatingNeg: Int32

    func saturatingShl(y: UInt64): Int32

    func saturatingShr(y: UInt64): Int32
}

extend Extend <: SaturatingOp & SaturatingPow {
    func saturatingAdd(y: Int64): Int64

    func saturatingSub(y: Int64): Int64

    func saturatingMul(y: Int64): Int64

    func saturatingDiv(y: Int64): Int64

    func saturatingMod(y: Int64): Int64

    func saturatingPow(y: UInt64): Int64

    func saturatingInc: Int64

    func saturatingDec: Int64

    func saturatingNeg: Int64

    func saturatingShl(y: UInt64): Int64

    func saturatingShr(y: UInt64): Int64
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: UInt8): UInt8

    func saturatingSub(y: UInt8): UInt8

    func saturatingMul(y: UInt8): UInt8

    func saturatingDiv(y: UInt8): UInt8

    func saturatingMod(y: UInt8): UInt8

    func saturatingInc: UInt8

    func saturatingDec: UInt8

    func saturatingNeg: UInt8

    func saturatingShl(y: UInt64): UInt8

    func saturatingShr(y: UInt64): UInt8
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: UInt16): UInt16

    func saturatingSub(y: UInt16): UInt16

    func saturatingMul(y: UInt16): UInt16

    func saturatingDiv(y: UInt16): UInt16

    func saturatingMod(y: UInt16): UInt16

    func saturatingInc: UInt16

    func saturatingDec: UInt16

    func saturatingNeg: UInt16

    func saturatingShl(y: UInt64): UInt16

    func saturatingShr(y: UInt64): UInt16
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: UInt32): UInt32

    func saturatingSub(y: UInt32): UInt32

    func saturatingMul(y: UInt32): UInt32

    func saturatingDiv(y: UInt32): UInt32

    func saturatingMod(y: UInt32): UInt32

    func saturatingInc: UInt32

    func saturatingDec: UInt32

    func saturatingNeg: UInt32

    func saturatingShl(y: UInt64): UInt32

    func saturatingShr(y: UInt64): UInt32
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: UInt64): UInt64

    func saturatingSub(y: UInt64): UInt64

    func saturatingMul(y: UInt64): UInt64

    func saturatingDiv(y: UInt64): UInt64

    func saturatingMod(y: UInt64): UInt64

    func saturatingInc: UInt64

    func saturatingDec: UInt64

    func saturatingNeg: UInt64

    func saturatingShl(y: UInt64): UInt64

    func saturatingShr(y: UInt64): UInt64
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: IntNative): IntNative

    func saturatingSub(y: IntNative): IntNative

    func saturatingMul(y: IntNative): IntNative

    func saturatingDiv(y: IntNative): IntNative

    func saturatingMod(y: IntNative): IntNative

    func saturatingInc: IntNative

    func saturatingDec: IntNative

    func saturatingNeg: IntNative

    func saturatingShl(y: UInt64): IntNative

    func saturatingShr(y: UInt64): IntNative
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: UIntNative): UIntNative

    func saturatingSub(y: UIntNative): UIntNative

    func saturatingMul(y: UIntNative): UIntNative

    func saturatingDiv(y: UIntNative): UIntNative

    func saturatingMod(y: UIntNative): UIntNative

    func saturatingInc: UIntNative

    func saturatingDec: UIntNative

    func saturatingNeg: UIntNative

    func saturatingShl(y: UInt64): UIntNative

    func saturatingShr(y: UInt64): UIntNative
}

func throwingAdd(x: T, y: T): T

func throwingSub(x: T, y: T): T

func throwingMul(x: T, y: T): T

func throwingDiv(x: T, y: T): T

func throwingMod(x: T, y: T): T

func throwingPow(x: Int64, y: UInt64): Int64

func throwingInc(x: T): T

func throwingDec(x: T): T

func throwingNeg(x: T): T

interface ThrowingOp {
    func throwingAdd(y: T): T

    func throwingSub(y: T): T

    func throwingMul(y: T): T

    func throwingDiv(y: T): T

    func throwingMod(y: T): T

    func throwingInc: T

    func throwingDec: T

    func throwingNeg: T

    func throwingShl(y: UInt64): T

    func throwingShr(y: UInt64): T
}

interface ThrowingPow {
    func throwingPow(y: UInt64): Int64
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: Int8): Int8

    func throwingSub(y: Int8): Int8

    func throwingMul(y: Int8): Int8

    func throwingDiv(y: Int8): Int8

    func throwingMod(y: Int8): Int8

    func throwingInc: Int8

    func throwingDec: Int8

    func throwingNeg: Int8

    func throwingShl(y: UInt64): Int8

    func throwingShr(y: UInt64): Int8
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: Int16): Int16

    func throwingSub(y: Int16): Int16

    func throwingMul(y: Int16): Int16

    func throwingDiv(y: Int16): Int16

    func throwingMod(y: Int16): Int16

    func throwingInc: Int16

    func throwingDec: Int16

    func throwingNeg: Int16

    func throwingShl(y: UInt64): Int16

    func throwingShr(y: UInt64): Int16
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: Int32): Int32

    func throwingSub(y: Int32): Int32

    func throwingMul(y: Int32): Int32

    func throwingDiv(y: Int32): Int32

    func throwingMod(y: Int32): Int32

    func throwingInc: Int32

    func throwingDec: Int32

    func throwingNeg: Int32

    func throwingShl(y: UInt64): Int32

    func throwingShr(y: UInt64): Int32
}

extend Extend <: ThrowingOp & ThrowingPow {
    func throwingAdd(y: Int64): Int64

    func throwingSub(y: Int64): Int64

    func throwingMul(y: Int64): Int64

    func throwingDiv(y: Int64): Int64

    func throwingMod(y: Int64): Int64

    func throwingPow(y: UInt64): Int64

    func throwingInc: Int64

    func throwingDec: Int64

    func throwingNeg: Int64

    func throwingShl(y: UInt64): Int64

    func throwingShr(y: UInt64): Int64
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: UInt8): UInt8

    func throwingSub(y: UInt8): UInt8

    func throwingMul(y: UInt8): UInt8

    func throwingDiv(y: UInt8): UInt8

    func throwingMod(y: UInt8): UInt8

    func throwingInc: UInt8

    func throwingDec: UInt8

    func throwingNeg: UInt8

    func throwingShl(y: UInt64): UInt8

    func throwingShr(y: UInt64): UInt8
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: UInt16): UInt16

    func throwingSub(y: UInt16): UInt16

    func throwingMul(y: UInt16): UInt16

    func throwingDiv(y: UInt16): UInt16

    func throwingMod(y: UInt16): UInt16

    func throwingInc: UInt16

    func throwingDec: UInt16

    func throwingNeg: UInt16

    func throwingShl(y: UInt64): UInt16

    func throwingShr(y: UInt64): UInt16
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: UInt32): UInt32

    func throwingSub(y: UInt32): UInt32

    func throwingMul(y: UInt32): UInt32

    func throwingDiv(y: UInt32): UInt32

    func throwingMod(y: UInt32): UInt32

    func throwingInc: UInt32

    func throwingDec: UInt32

    func throwingNeg: UInt32

    func throwingShl(y: UInt64): UInt32

    func throwingShr(y: UInt64): UInt32
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: UInt64): UInt64

    func throwingSub(y: UInt64): UInt64

    func throwingMul(y: UInt64): UInt64

    func throwingDiv(y: UInt64): UInt64

    func throwingMod(y: UInt64): UInt64

    func throwingInc: UInt64

    func throwingDec: UInt64

    func throwingNeg: UInt64

    func throwingShl(y: UInt64): UInt64

    func throwingShr(y: UInt64): UInt64
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: IntNative): IntNative

    func throwingSub(y: IntNative): IntNative

    func throwingMul(y: IntNative): IntNative

    func throwingDiv(y: IntNative): IntNative

    func throwingMod(y: IntNative): IntNative

    func throwingInc: IntNative

    func throwingDec: IntNative

    func throwingNeg: IntNative

    func throwingShl(y: UInt64): IntNative

    func throwingShr(y: UInt64): IntNative
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: UIntNative): UIntNative

    func throwingSub(y: UIntNative): UIntNative

    func throwingMul(y: UIntNative): UIntNative

    func throwingDiv(y: UIntNative): UIntNative

    func throwingMod(y: UIntNative): UIntNative

    func throwingInc: UIntNative

    func throwingDec: UIntNative

    func throwingNeg: UIntNative

    func throwingShl(y: UInt64): UIntNative

    func throwingShr(y: UInt64): UIntNative
}

class OvershiftException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class UndershiftException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

func wrappingAdd(x: T, y: T): T

func wrappingSub(x: T, y: T): T

func wrappingMul(x: T, y: T): T

func wrappingDiv(x: T, y: T): T

func wrappingMod(x: T, y: T): T

func wrappingPow(x: Int64, y: UInt64): Int64

func wrappingInc(x: T): T

func wrappingDec(x: T): T

func wrappingNeg(x: T): T

interface WrappingOp {
    func wrappingAdd(y: T): T

    func wrappingSub(y: T): T

    func wrappingMul(y: T): T

    func wrappingDiv(y: T): T

    func wrappingMod(y: T): T

    func wrappingInc: T

    func wrappingDec: T

    func wrappingNeg: T

    func wrappingShl(y: UInt64): T

    func wrappingShr(y: UInt64): T
}

interface WrappingPow {
    func wrappingPow(y: UInt64): Int64
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: Int8): Int8

    func wrappingSub(y: Int8): Int8

    func wrappingMul(y: Int8): Int8

    func wrappingDiv(y: Int8): Int8

    func wrappingMod(y: Int8): Int8

    func wrappingInc: Int8

    func wrappingDec: Int8

    func wrappingNeg: Int8

    func wrappingShl(y: UInt64): Int8

    func wrappingShr(y: UInt64): Int8
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: Int16): Int16

    func wrappingSub(y: Int16): Int16

    func wrappingMul(y: Int16): Int16

    func wrappingDiv(y: Int16): Int16

    func wrappingMod(y: Int16): Int16

    func wrappingInc: Int16

    func wrappingDec: Int16

    func wrappingNeg: Int16

    func wrappingShl(y: UInt64): Int16

    func wrappingShr(y: UInt64): Int16
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: Int32): Int32

    func wrappingSub(y: Int32): Int32

    func wrappingMul(y: Int32): Int32

    func wrappingDiv(y: Int32): Int32

    func wrappingMod(y: Int32): Int32

    func wrappingInc: Int32

    func wrappingDec: Int32

    func wrappingNeg: Int32

    func wrappingShl(y: UInt64): Int32

    func wrappingShr(y: UInt64): Int32
}

extend Extend <: WrappingOp & WrappingPow {
    func wrappingAdd(y: Int64): Int64

    func wrappingSub(y: Int64): Int64

    func wrappingMul(y: Int64): Int64

    func wrappingDiv(y: Int64): Int64

    func wrappingMod(y: Int64): Int64

    func wrappingPow(y: UInt64): Int64

    func wrappingInc: Int64

    func wrappingDec: Int64

    func wrappingNeg: Int64

    func wrappingShl(y: UInt64): Int64

    func wrappingShr(y: UInt64): Int64
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: UInt8): UInt8

    func wrappingSub(y: UInt8): UInt8

    func wrappingMul(y: UInt8): UInt8

    func wrappingDiv(y: UInt8): UInt8

    func wrappingMod(y: UInt8): UInt8

    func wrappingInc: UInt8

    func wrappingDec: UInt8

    func wrappingNeg: UInt8

    func wrappingShl(y: UInt64): UInt8

    func wrappingShr(y: UInt64): UInt8
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: UInt16): UInt16

    func wrappingSub(y: UInt16): UInt16

    func wrappingMul(y: UInt16): UInt16

    func wrappingDiv(y: UInt16): UInt16

    func wrappingMod(y: UInt16): UInt16

    func wrappingInc: UInt16

    func wrappingDec: UInt16

    func wrappingNeg: UInt16

    func wrappingShl(y: UInt64): UInt16

    func wrappingShr(y: UInt64): UInt16
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: UInt32): UInt32

    func wrappingSub(y: UInt32): UInt32

    func wrappingMul(y: UInt32): UInt32

    func wrappingDiv(y: UInt32): UInt32

    func wrappingMod(y: UInt32): UInt32

    func wrappingInc: UInt32

    func wrappingDec: UInt32

    func wrappingNeg: UInt32

    func wrappingShl(y: UInt64): UInt32

    func wrappingShr(y: UInt64): UInt32
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: UInt64): UInt64

    func wrappingSub(y: UInt64): UInt64

    func wrappingMul(y: UInt64): UInt64

    func wrappingDiv(y: UInt64): UInt64

    func wrappingMod(y: UInt64): UInt64

    func wrappingInc: UInt64

    func wrappingDec: UInt64

    func wrappingNeg: UInt64

    func wrappingShl(y: UInt64): UInt64

    func wrappingShr(y: UInt64): UInt64
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: IntNative): IntNative

    func wrappingSub(y: IntNative): IntNative

    func wrappingMul(y: IntNative): IntNative

    func wrappingDiv(y: IntNative): IntNative

    func wrappingMod(y: IntNative): IntNative

    func wrappingInc: IntNative

    func wrappingDec: IntNative

    func wrappingNeg: IntNative

    func wrappingShl(y: UInt64): IntNative

    func wrappingShr(y: UInt64): IntNative
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: UIntNative): UIntNative

    func wrappingSub(y: UIntNative): UIntNative

    func wrappingMul(y: UIntNative): UIntNative

    func wrappingDiv(y: UIntNative): UIntNative

    func wrappingMod(y: UIntNative): UIntNative

    func wrappingInc: UIntNative

    func wrappingDec: UIntNative

    func wrappingNeg: UIntNative

    func wrappingShl(y: UInt64): UIntNative

    func wrappingShr(y: UInt64): UIntNative
}
