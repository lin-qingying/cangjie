// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.overflow

import std.core.*

let isNative64: Bool

interface CarryingOp {
    func carryingAdd(y: T): (Bool, T) { /* compiled code */ }

    func carryingSub(y: T): (Bool, T) { /* compiled code */ }

    func carryingMul(y: T): (Bool, T) { /* compiled code */ }

    func carryingDiv(y: T): (Bool, T) { /* compiled code */ }

    func carryingMod(y: T): (Bool, T) { /* compiled code */ }

    func carryingInc(): (Bool, T) { /* compiled code */ }

    func carryingDec(): (Bool, T) { /* compiled code */ }

    func carryingNeg(): (Bool, T) { /* compiled code */ }

    func carryingShl(y: UInt64): (Bool, T) { /* compiled code */ }

    func carryingShr(y: UInt64): (Bool, T) { /* compiled code */ }
}

interface CarryingPow {
    func carryingPow(y: UInt64): (Bool, Int64) { /* compiled code */ }
}

extend Extend <: CarryingOp {
    func carryingAdd(y: Int8): (Bool, Int8) { /* compiled code */ }

    func carryingSub(y: Int8): (Bool, Int8) { /* compiled code */ }

    func carryingMul(y: Int8): (Bool, Int8) { /* compiled code */ }

    func carryingDiv(y: Int8): (Bool, Int8) { /* compiled code */ }

    func carryingMod(y: Int8): (Bool, Int8) { /* compiled code */ }

    func carryingInc(): (Bool, Int8) { /* compiled code */ }

    func carryingDec(): (Bool, Int8) { /* compiled code */ }

    func carryingNeg(): (Bool, Int8) { /* compiled code */ }

    func carryingShl(y: UInt64): (Bool, Int8) { /* compiled code */ }

    func carryingShr(y: UInt64): (Bool, Int8) { /* compiled code */ }
}

extend Extend <: CarryingOp {
    func carryingAdd(y: Int16): (Bool, Int16) { /* compiled code */ }

    func carryingSub(y: Int16): (Bool, Int16) { /* compiled code */ }

    func carryingMul(y: Int16): (Bool, Int16) { /* compiled code */ }

    func carryingDiv(y: Int16): (Bool, Int16) { /* compiled code */ }

    func carryingMod(y: Int16): (Bool, Int16) { /* compiled code */ }

    func carryingInc(): (Bool, Int16) { /* compiled code */ }

    func carryingDec(): (Bool, Int16) { /* compiled code */ }

    func carryingNeg(): (Bool, Int16) { /* compiled code */ }

    func carryingShl(y: UInt64): (Bool, Int16) { /* compiled code */ }

    func carryingShr(y: UInt64): (Bool, Int16) { /* compiled code */ }
}

extend Extend <: CarryingOp {
    func carryingAdd(y: Int32): (Bool, Int32) { /* compiled code */ }

    func carryingSub(y: Int32): (Bool, Int32) { /* compiled code */ }

    func carryingMul(y: Int32): (Bool, Int32) { /* compiled code */ }

    func carryingDiv(y: Int32): (Bool, Int32) { /* compiled code */ }

    func carryingMod(y: Int32): (Bool, Int32) { /* compiled code */ }

    func carryingInc(): (Bool, Int32) { /* compiled code */ }

    func carryingDec(): (Bool, Int32) { /* compiled code */ }

    func carryingNeg(): (Bool, Int32) { /* compiled code */ }

    func carryingShl(y: UInt64): (Bool, Int32) { /* compiled code */ }

    func carryingShr(y: UInt64): (Bool, Int32) { /* compiled code */ }
}

extend Extend <: CarryingOp & CarryingPow {
    func carryingAdd(y: Int64): (Bool, Int64) { /* compiled code */ }

    func carryingSub(y: Int64): (Bool, Int64) { /* compiled code */ }

    func carryingMul(y: Int64): (Bool, Int64) { /* compiled code */ }

    func carryingDiv(y: Int64): (Bool, Int64) { /* compiled code */ }

    func carryingMod(y: Int64): (Bool, Int64) { /* compiled code */ }

    func carryingInc(): (Bool, Int64) { /* compiled code */ }

    func carryingDec(): (Bool, Int64) { /* compiled code */ }

    func carryingNeg(): (Bool, Int64) { /* compiled code */ }

    func carryingShl(y: UInt64): (Bool, Int64) { /* compiled code */ }

    func carryingShr(y: UInt64): (Bool, Int64) { /* compiled code */ }

    func carryingPow(y: UInt64): (Bool, Int64) { /* compiled code */ }
}

extend Extend <: CarryingOp {
    func carryingAdd(y: IntNative): (Bool, IntNative) { /* compiled code */ }

    func carryingSub(y: IntNative): (Bool, IntNative) { /* compiled code */ }

    func carryingMul(y: IntNative): (Bool, IntNative) { /* compiled code */ }

    func carryingDiv(y: IntNative): (Bool, IntNative) { /* compiled code */ }

    func carryingMod(y: IntNative): (Bool, IntNative) { /* compiled code */ }

    func carryingInc(): (Bool, IntNative) { /* compiled code */ }

    func carryingDec(): (Bool, IntNative) { /* compiled code */ }

    func carryingNeg(): (Bool, IntNative) { /* compiled code */ }

    func carryingShl(y: UInt64): (Bool, IntNative) { /* compiled code */ }

    func carryingShr(y: UInt64): (Bool, IntNative) { /* compiled code */ }
}

extend Extend <: CarryingOp {
    func carryingAdd(y: UInt8): (Bool, UInt8) { /* compiled code */ }

    func carryingSub(y: UInt8): (Bool, UInt8) { /* compiled code */ }

    func carryingMul(y: UInt8): (Bool, UInt8) { /* compiled code */ }

    func carryingDiv(y: UInt8): (Bool, UInt8) { /* compiled code */ }

    func carryingMod(y: UInt8): (Bool, UInt8) { /* compiled code */ }

    func carryingInc(): (Bool, UInt8) { /* compiled code */ }

    func carryingDec(): (Bool, UInt8) { /* compiled code */ }

    func carryingNeg(): (Bool, UInt8) { /* compiled code */ }

    func carryingShl(y: UInt64): (Bool, UInt8) { /* compiled code */ }

    func carryingShr(y: UInt64): (Bool, UInt8) { /* compiled code */ }
}

extend Extend <: CarryingOp {
    func carryingAdd(y: UInt16): (Bool, UInt16) { /* compiled code */ }

    func carryingSub(y: UInt16): (Bool, UInt16) { /* compiled code */ }

    func carryingMul(y: UInt16): (Bool, UInt16) { /* compiled code */ }

    func carryingDiv(y: UInt16): (Bool, UInt16) { /* compiled code */ }

    func carryingMod(y: UInt16): (Bool, UInt16) { /* compiled code */ }

    func carryingInc(): (Bool, UInt16) { /* compiled code */ }

    func carryingDec(): (Bool, UInt16) { /* compiled code */ }

    func carryingNeg(): (Bool, UInt16) { /* compiled code */ }

    func carryingShl(y: UInt64): (Bool, UInt16) { /* compiled code */ }

    func carryingShr(y: UInt64): (Bool, UInt16) { /* compiled code */ }
}

extend Extend <: CarryingOp {
    func carryingAdd(y: UInt32): (Bool, UInt32) { /* compiled code */ }

    func carryingSub(y: UInt32): (Bool, UInt32) { /* compiled code */ }

    func carryingMul(y: UInt32): (Bool, UInt32) { /* compiled code */ }

    func carryingDiv(y: UInt32): (Bool, UInt32) { /* compiled code */ }

    func carryingMod(y: UInt32): (Bool, UInt32) { /* compiled code */ }

    func carryingInc(): (Bool, UInt32) { /* compiled code */ }

    func carryingDec(): (Bool, UInt32) { /* compiled code */ }

    func carryingNeg(): (Bool, UInt32) { /* compiled code */ }

    func carryingShl(y: UInt64): (Bool, UInt32) { /* compiled code */ }

    func carryingShr(y: UInt64): (Bool, UInt32) { /* compiled code */ }
}

extend Extend <: CarryingOp {
    func carryingAdd(y: UInt64): (Bool, UInt64) { /* compiled code */ }

    func carryingSub(y: UInt64): (Bool, UInt64) { /* compiled code */ }

    func carryingMul(y: UInt64): (Bool, UInt64) { /* compiled code */ }

    func carryingDiv(y: UInt64): (Bool, UInt64) { /* compiled code */ }

    func carryingMod(y: UInt64): (Bool, UInt64) { /* compiled code */ }

    func carryingInc(): (Bool, UInt64) { /* compiled code */ }

    func carryingDec(): (Bool, UInt64) { /* compiled code */ }

    func carryingNeg(): (Bool, UInt64) { /* compiled code */ }

    func carryingShl(y: UInt64): (Bool, UInt64) { /* compiled code */ }

    func carryingShr(y: UInt64): (Bool, UInt64) { /* compiled code */ }
}

extend Extend <: CarryingOp {
    func carryingAdd(y: UIntNative): (Bool, UIntNative) { /* compiled code */ }

    func carryingSub(y: UIntNative): (Bool, UIntNative) { /* compiled code */ }

    func carryingMul(y: UIntNative): (Bool, UIntNative) { /* compiled code */ }

    func carryingDiv(y: UIntNative): (Bool, UIntNative) { /* compiled code */ }

    func carryingMod(y: UIntNative): (Bool, UIntNative) { /* compiled code */ }

    func carryingInc(): (Bool, UIntNative) { /* compiled code */ }

    func carryingDec(): (Bool, UIntNative) { /* compiled code */ }

    func carryingNeg(): (Bool, UIntNative) { /* compiled code */ }

    func carryingShl(y: UInt64): (Bool, UIntNative) { /* compiled code */ }

    func carryingShr(y: UInt64): (Bool, UIntNative) { /* compiled code */ }
}

interface CheckedOp {
    func checkedAdd(y: T): std.core.Option<T> { /* compiled code */ }

    func checkedSub(y: T): std.core.Option<T> { /* compiled code */ }

    func checkedMul(y: T): std.core.Option<T> { /* compiled code */ }

    func checkedDiv(y: T): std.core.Option<T> { /* compiled code */ }

    func checkedMod(y: T): std.core.Option<T> { /* compiled code */ }

    func checkedInc(): std.core.Option<T> { /* compiled code */ }

    func checkedDec(): std.core.Option<T> { /* compiled code */ }

    func checkedNeg(): std.core.Option<T> { /* compiled code */ }

    func checkedShl(y: UInt64): std.core.Option<T> { /* compiled code */ }

    func checkedShr(y: UInt64): std.core.Option<T> { /* compiled code */ }
}

interface CheckedPow {
    func checkedPow(y: UInt64): std.core.Option<Int64> { /* compiled code */ }
}

func checkedShl(x: Int8, y: UInt64): std.core.Option<Int8> { /* compiled code */ }

func checkedShl(x: Int16, y: UInt64): std.core.Option<Int16> { /* compiled code */ }

func checkedShl(x: Int32, y: UInt64): std.core.Option<Int32> { /* compiled code */ }

func checkedShl(x: Int64, y: UInt64): std.core.Option<Int64> { /* compiled code */ }

func checkedShl(x: IntNative, y: UInt64): std.core.Option<IntNative> { /* compiled code */ }

func checkedShl(x: UInt8, y: UInt64): std.core.Option<UInt8> { /* compiled code */ }

func checkedShl(x: UInt16, y: UInt64): std.core.Option<UInt16> { /* compiled code */ }

func checkedShl(x: UInt32, y: UInt64): std.core.Option<UInt32> { /* compiled code */ }

func checkedShl(x: UInt64, y: UInt64): std.core.Option<UInt64> { /* compiled code */ }

func checkedShl(x: UIntNative, y: UInt64): std.core.Option<UIntNative> { /* compiled code */ }

func checkedShr(x: Int8, y: UInt64): std.core.Option<Int8> { /* compiled code */ }

func checkedShr(x: Int16, y: UInt64): std.core.Option<Int16> { /* compiled code */ }

func checkedShr(x: Int32, y: UInt64): std.core.Option<Int32> { /* compiled code */ }

func checkedShr(x: Int64, y: UInt64): std.core.Option<Int64> { /* compiled code */ }

func checkedShr(x: IntNative, y: UInt64): std.core.Option<IntNative> { /* compiled code */ }

func checkedShr(x: UInt8, y: UInt64): std.core.Option<UInt8> { /* compiled code */ }

func checkedShr(x: UInt16, y: UInt64): std.core.Option<UInt16> { /* compiled code */ }

func checkedShr(x: UInt32, y: UInt64): std.core.Option<UInt32> { /* compiled code */ }

func checkedShr(x: UInt64, y: UInt64): std.core.Option<UInt64> { /* compiled code */ }

func checkedShr(x: UIntNative, y: UInt64): std.core.Option<UIntNative> { /* compiled code */ }

func checkedAdd(x: T, y: T): std.core.Option<T> { /* compiled code */ }

func checkedSub(x: T, y: T): std.core.Option<T> { /* compiled code */ }

func checkedMul(x: T, y: T): std.core.Option<T> { /* compiled code */ }

func checkedDiv(x: T, y: T): std.core.Option<T> { /* compiled code */ }

func checkedMod(x: T, y: T): std.core.Option<T> { /* compiled code */ }

func checkedPow(x: Int64, y: UInt64): std.core.Option<Int64> { /* compiled code */ }

func checkedInc(x: T): std.core.Option<T> { /* compiled code */ }

func checkedDec(x: T): std.core.Option<T> { /* compiled code */ }

func checkedNeg(x: T): std.core.Option<T> { /* compiled code */ }

extend Extend <: CheckedOp {
    func checkedAdd(y: Int8): std.core.Option<Int8> { /* compiled code */ }

    func checkedSub(y: Int8): std.core.Option<Int8> { /* compiled code */ }

    func checkedMul(y: Int8): std.core.Option<Int8> { /* compiled code */ }

    func checkedDiv(y: Int8): std.core.Option<Int8> { /* compiled code */ }

    func checkedMod(y: Int8): std.core.Option<Int8> { /* compiled code */ }

    func checkedInc(): std.core.Option<Int8> { /* compiled code */ }

    func checkedDec(): std.core.Option<Int8> { /* compiled code */ }

    func checkedNeg(): std.core.Option<Int8> { /* compiled code */ }

    func checkedShl(y: UInt64): std.core.Option<Int8> { /* compiled code */ }

    func checkedShr(y: UInt64): std.core.Option<Int8> { /* compiled code */ }
}

extend Extend <: CheckedOp {
    func checkedAdd(y: Int16): std.core.Option<Int16> { /* compiled code */ }

    func checkedSub(y: Int16): std.core.Option<Int16> { /* compiled code */ }

    func checkedMul(y: Int16): std.core.Option<Int16> { /* compiled code */ }

    func checkedDiv(y: Int16): std.core.Option<Int16> { /* compiled code */ }

    func checkedMod(y: Int16): std.core.Option<Int16> { /* compiled code */ }

    func checkedInc(): std.core.Option<Int16> { /* compiled code */ }

    func checkedDec(): std.core.Option<Int16> { /* compiled code */ }

    func checkedNeg(): std.core.Option<Int16> { /* compiled code */ }

    func checkedShl(y: UInt64): std.core.Option<Int16> { /* compiled code */ }

    func checkedShr(y: UInt64): std.core.Option<Int16> { /* compiled code */ }
}

extend Extend <: CheckedOp {
    func checkedAdd(y: Int32): std.core.Option<Int32> { /* compiled code */ }

    func checkedSub(y: Int32): std.core.Option<Int32> { /* compiled code */ }

    func checkedMul(y: Int32): std.core.Option<Int32> { /* compiled code */ }

    func checkedDiv(y: Int32): std.core.Option<Int32> { /* compiled code */ }

    func checkedMod(y: Int32): std.core.Option<Int32> { /* compiled code */ }

    func checkedInc(): std.core.Option<Int32> { /* compiled code */ }

    func checkedDec(): std.core.Option<Int32> { /* compiled code */ }

    func checkedNeg(): std.core.Option<Int32> { /* compiled code */ }

    func checkedShl(y: UInt64): std.core.Option<Int32> { /* compiled code */ }

    func checkedShr(y: UInt64): std.core.Option<Int32> { /* compiled code */ }
}

extend Extend <: CheckedOp & CheckedPow {
    func checkedAdd(y: Int64): std.core.Option<Int64> { /* compiled code */ }

    func checkedSub(y: Int64): std.core.Option<Int64> { /* compiled code */ }

    func checkedMul(y: Int64): std.core.Option<Int64> { /* compiled code */ }

    func checkedDiv(y: Int64): std.core.Option<Int64> { /* compiled code */ }

    func checkedMod(y: Int64): std.core.Option<Int64> { /* compiled code */ }

    func checkedPow(y: UInt64): std.core.Option<Int64> { /* compiled code */ }

    func checkedInc(): std.core.Option<Int64> { /* compiled code */ }

    func checkedDec(): std.core.Option<Int64> { /* compiled code */ }

    func checkedNeg(): std.core.Option<Int64> { /* compiled code */ }

    func checkedShl(y: UInt64): std.core.Option<Int64> { /* compiled code */ }

    func checkedShr(y: UInt64): std.core.Option<Int64> { /* compiled code */ }
}

extend Extend <: CheckedOp {
    func checkedAdd(y: UInt8): std.core.Option<UInt8> { /* compiled code */ }

    func checkedSub(y: UInt8): std.core.Option<UInt8> { /* compiled code */ }

    func checkedMul(y: UInt8): std.core.Option<UInt8> { /* compiled code */ }

    func checkedDiv(y: UInt8): std.core.Option<UInt8> { /* compiled code */ }

    func checkedMod(y: UInt8): std.core.Option<UInt8> { /* compiled code */ }

    func checkedInc(): std.core.Option<UInt8> { /* compiled code */ }

    func checkedDec(): std.core.Option<UInt8> { /* compiled code */ }

    func checkedNeg(): std.core.Option<UInt8> { /* compiled code */ }

    func checkedShl(y: UInt64): std.core.Option<UInt8> { /* compiled code */ }

    func checkedShr(y: UInt64): std.core.Option<UInt8> { /* compiled code */ }
}

extend Extend <: CheckedOp {
    func checkedAdd(y: UInt16): std.core.Option<UInt16> { /* compiled code */ }

    func checkedSub(y: UInt16): std.core.Option<UInt16> { /* compiled code */ }

    func checkedMul(y: UInt16): std.core.Option<UInt16> { /* compiled code */ }

    func checkedDiv(y: UInt16): std.core.Option<UInt16> { /* compiled code */ }

    func checkedMod(y: UInt16): std.core.Option<UInt16> { /* compiled code */ }

    func checkedInc(): std.core.Option<UInt16> { /* compiled code */ }

    func checkedDec(): std.core.Option<UInt16> { /* compiled code */ }

    func checkedNeg(): std.core.Option<UInt16> { /* compiled code */ }

    func checkedShl(y: UInt64): std.core.Option<UInt16> { /* compiled code */ }

    func checkedShr(y: UInt64): std.core.Option<UInt16> { /* compiled code */ }
}

extend Extend <: CheckedOp {
    func checkedAdd(y: UInt32): std.core.Option<UInt32> { /* compiled code */ }

    func checkedSub(y: UInt32): std.core.Option<UInt32> { /* compiled code */ }

    func checkedMul(y: UInt32): std.core.Option<UInt32> { /* compiled code */ }

    func checkedDiv(y: UInt32): std.core.Option<UInt32> { /* compiled code */ }

    func checkedMod(y: UInt32): std.core.Option<UInt32> { /* compiled code */ }

    func checkedInc(): std.core.Option<UInt32> { /* compiled code */ }

    func checkedDec(): std.core.Option<UInt32> { /* compiled code */ }

    func checkedNeg(): std.core.Option<UInt32> { /* compiled code */ }

    func checkedShl(y: UInt64): std.core.Option<UInt32> { /* compiled code */ }

    func checkedShr(y: UInt64): std.core.Option<UInt32> { /* compiled code */ }
}

extend Extend <: CheckedOp {
    func checkedAdd(y: UInt64): std.core.Option<UInt64> { /* compiled code */ }

    func checkedSub(y: UInt64): std.core.Option<UInt64> { /* compiled code */ }

    func checkedMul(y: UInt64): std.core.Option<UInt64> { /* compiled code */ }

    func checkedDiv(y: UInt64): std.core.Option<UInt64> { /* compiled code */ }

    func checkedMod(y: UInt64): std.core.Option<UInt64> { /* compiled code */ }

    func checkedInc(): std.core.Option<UInt64> { /* compiled code */ }

    func checkedDec(): std.core.Option<UInt64> { /* compiled code */ }

    func checkedNeg(): std.core.Option<UInt64> { /* compiled code */ }

    func checkedShl(y: UInt64): std.core.Option<UInt64> { /* compiled code */ }

    func checkedShr(y: UInt64): std.core.Option<UInt64> { /* compiled code */ }
}

extend Extend <: CheckedOp {
    func checkedAdd(y: IntNative): std.core.Option<IntNative> { /* compiled code */ }

    func checkedSub(y: IntNative): std.core.Option<IntNative> { /* compiled code */ }

    func checkedMul(y: IntNative): std.core.Option<IntNative> { /* compiled code */ }

    func checkedDiv(y: IntNative): std.core.Option<IntNative> { /* compiled code */ }

    func checkedMod(y: IntNative): std.core.Option<IntNative> { /* compiled code */ }

    func checkedInc(): std.core.Option<IntNative> { /* compiled code */ }

    func checkedDec(): std.core.Option<IntNative> { /* compiled code */ }

    func checkedNeg(): std.core.Option<IntNative> { /* compiled code */ }

    func checkedShl(y: UInt64): std.core.Option<IntNative> { /* compiled code */ }

    func checkedShr(y: UInt64): std.core.Option<IntNative> { /* compiled code */ }
}

extend Extend <: CheckedOp {
    func checkedAdd(y: UIntNative): std.core.Option<UIntNative> { /* compiled code */ }

    func checkedSub(y: UIntNative): std.core.Option<UIntNative> { /* compiled code */ }

    func checkedMul(y: UIntNative): std.core.Option<UIntNative> { /* compiled code */ }

    func checkedDiv(y: UIntNative): std.core.Option<UIntNative> { /* compiled code */ }

    func checkedMod(y: UIntNative): std.core.Option<UIntNative> { /* compiled code */ }

    func checkedInc(): std.core.Option<UIntNative> { /* compiled code */ }

    func checkedDec(): std.core.Option<UIntNative> { /* compiled code */ }

    func checkedNeg(): std.core.Option<UIntNative> { /* compiled code */ }

    func checkedShl(y: UInt64): std.core.Option<UIntNative> { /* compiled code */ }

    func checkedShr(y: UInt64): std.core.Option<UIntNative> { /* compiled code */ }
}

func saturatingAdd(x: T, y: T): T { /* compiled code */ }

func saturatingSub(x: T, y: T): T { /* compiled code */ }

func saturatingMul(x: T, y: T): T { /* compiled code */ }

func saturatingDiv(x: T, y: T): T { /* compiled code */ }

func saturatingMod(x: T, y: T): T { /* compiled code */ }

func saturatingPow(x: Int64, y: UInt64): Int64 { /* compiled code */ }

func saturatingInc(x: T): T { /* compiled code */ }

func saturatingNeg(x: T): T { /* compiled code */ }

func saturatingDec(x: T): T { /* compiled code */ }

interface SaturatingOp {
    func saturatingAdd(y: T): T { /* compiled code */ }

    func saturatingSub(y: T): T { /* compiled code */ }

    func saturatingMul(y: T): T { /* compiled code */ }

    func saturatingDiv(y: T): T { /* compiled code */ }

    func saturatingMod(y: T): T { /* compiled code */ }

    func saturatingInc(): T { /* compiled code */ }

    func saturatingDec(): T { /* compiled code */ }

    func saturatingNeg(): T { /* compiled code */ }

    func saturatingShl(y: UInt64): T { /* compiled code */ }

    func saturatingShr(y: UInt64): T { /* compiled code */ }
}

interface SaturatingPow {
    func saturatingPow(y: UInt64): Int64 { /* compiled code */ }
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: Int8): Int8 { /* compiled code */ }

    func saturatingSub(y: Int8): Int8 { /* compiled code */ }

    func saturatingMul(y: Int8): Int8 { /* compiled code */ }

    func saturatingDiv(y: Int8): Int8 { /* compiled code */ }

    func saturatingMod(y: Int8): Int8 { /* compiled code */ }

    func saturatingInc(): Int8 { /* compiled code */ }

    func saturatingDec(): Int8 { /* compiled code */ }

    func saturatingNeg(): Int8 { /* compiled code */ }

    func saturatingShl(y: UInt64): Int8 { /* compiled code */ }

    func saturatingShr(y: UInt64): Int8 { /* compiled code */ }
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: Int16): Int16 { /* compiled code */ }

    func saturatingSub(y: Int16): Int16 { /* compiled code */ }

    func saturatingMul(y: Int16): Int16 { /* compiled code */ }

    func saturatingDiv(y: Int16): Int16 { /* compiled code */ }

    func saturatingMod(y: Int16): Int16 { /* compiled code */ }

    func saturatingInc(): Int16 { /* compiled code */ }

    func saturatingDec(): Int16 { /* compiled code */ }

    func saturatingNeg(): Int16 { /* compiled code */ }

    func saturatingShl(y: UInt64): Int16 { /* compiled code */ }

    func saturatingShr(y: UInt64): Int16 { /* compiled code */ }
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: Int32): Int32 { /* compiled code */ }

    func saturatingSub(y: Int32): Int32 { /* compiled code */ }

    func saturatingMul(y: Int32): Int32 { /* compiled code */ }

    func saturatingDiv(y: Int32): Int32 { /* compiled code */ }

    func saturatingMod(y: Int32): Int32 { /* compiled code */ }

    func saturatingInc(): Int32 { /* compiled code */ }

    func saturatingDec(): Int32 { /* compiled code */ }

    func saturatingNeg(): Int32 { /* compiled code */ }

    func saturatingShl(y: UInt64): Int32 { /* compiled code */ }

    func saturatingShr(y: UInt64): Int32 { /* compiled code */ }
}

extend Extend <: SaturatingOp & SaturatingPow {
    func saturatingAdd(y: Int64): Int64 { /* compiled code */ }

    func saturatingSub(y: Int64): Int64 { /* compiled code */ }

    func saturatingMul(y: Int64): Int64 { /* compiled code */ }

    func saturatingDiv(y: Int64): Int64 { /* compiled code */ }

    func saturatingMod(y: Int64): Int64 { /* compiled code */ }

    func saturatingPow(y: UInt64): Int64 { /* compiled code */ }

    func saturatingInc(): Int64 { /* compiled code */ }

    func saturatingDec(): Int64 { /* compiled code */ }

    func saturatingNeg(): Int64 { /* compiled code */ }

    func saturatingShl(y: UInt64): Int64 { /* compiled code */ }

    func saturatingShr(y: UInt64): Int64 { /* compiled code */ }
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: UInt8): UInt8 { /* compiled code */ }

    func saturatingSub(y: UInt8): UInt8 { /* compiled code */ }

    func saturatingMul(y: UInt8): UInt8 { /* compiled code */ }

    func saturatingDiv(y: UInt8): UInt8 { /* compiled code */ }

    func saturatingMod(y: UInt8): UInt8 { /* compiled code */ }

    func saturatingInc(): UInt8 { /* compiled code */ }

    func saturatingDec(): UInt8 { /* compiled code */ }

    func saturatingNeg(): UInt8 { /* compiled code */ }

    func saturatingShl(y: UInt64): UInt8 { /* compiled code */ }

    func saturatingShr(y: UInt64): UInt8 { /* compiled code */ }
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: UInt16): UInt16 { /* compiled code */ }

    func saturatingSub(y: UInt16): UInt16 { /* compiled code */ }

    func saturatingMul(y: UInt16): UInt16 { /* compiled code */ }

    func saturatingDiv(y: UInt16): UInt16 { /* compiled code */ }

    func saturatingMod(y: UInt16): UInt16 { /* compiled code */ }

    func saturatingInc(): UInt16 { /* compiled code */ }

    func saturatingDec(): UInt16 { /* compiled code */ }

    func saturatingNeg(): UInt16 { /* compiled code */ }

    func saturatingShl(y: UInt64): UInt16 { /* compiled code */ }

    func saturatingShr(y: UInt64): UInt16 { /* compiled code */ }
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: UInt32): UInt32 { /* compiled code */ }

    func saturatingSub(y: UInt32): UInt32 { /* compiled code */ }

    func saturatingMul(y: UInt32): UInt32 { /* compiled code */ }

    func saturatingDiv(y: UInt32): UInt32 { /* compiled code */ }

    func saturatingMod(y: UInt32): UInt32 { /* compiled code */ }

    func saturatingInc(): UInt32 { /* compiled code */ }

    func saturatingDec(): UInt32 { /* compiled code */ }

    func saturatingNeg(): UInt32 { /* compiled code */ }

    func saturatingShl(y: UInt64): UInt32 { /* compiled code */ }

    func saturatingShr(y: UInt64): UInt32 { /* compiled code */ }
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: UInt64): UInt64 { /* compiled code */ }

    func saturatingSub(y: UInt64): UInt64 { /* compiled code */ }

    func saturatingMul(y: UInt64): UInt64 { /* compiled code */ }

    func saturatingDiv(y: UInt64): UInt64 { /* compiled code */ }

    func saturatingMod(y: UInt64): UInt64 { /* compiled code */ }

    func saturatingInc(): UInt64 { /* compiled code */ }

    func saturatingDec(): UInt64 { /* compiled code */ }

    func saturatingNeg(): UInt64 { /* compiled code */ }

    func saturatingShl(y: UInt64): UInt64 { /* compiled code */ }

    func saturatingShr(y: UInt64): UInt64 { /* compiled code */ }
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: IntNative): IntNative { /* compiled code */ }

    func saturatingSub(y: IntNative): IntNative { /* compiled code */ }

    func saturatingMul(y: IntNative): IntNative { /* compiled code */ }

    func saturatingDiv(y: IntNative): IntNative { /* compiled code */ }

    func saturatingMod(y: IntNative): IntNative { /* compiled code */ }

    func saturatingInc(): IntNative { /* compiled code */ }

    func saturatingDec(): IntNative { /* compiled code */ }

    func saturatingNeg(): IntNative { /* compiled code */ }

    func saturatingShl(y: UInt64): IntNative { /* compiled code */ }

    func saturatingShr(y: UInt64): IntNative { /* compiled code */ }
}

extend Extend <: SaturatingOp {
    func saturatingAdd(y: UIntNative): UIntNative { /* compiled code */ }

    func saturatingSub(y: UIntNative): UIntNative { /* compiled code */ }

    func saturatingMul(y: UIntNative): UIntNative { /* compiled code */ }

    func saturatingDiv(y: UIntNative): UIntNative { /* compiled code */ }

    func saturatingMod(y: UIntNative): UIntNative { /* compiled code */ }

    func saturatingInc(): UIntNative { /* compiled code */ }

    func saturatingDec(): UIntNative { /* compiled code */ }

    func saturatingNeg(): UIntNative { /* compiled code */ }

    func saturatingShl(y: UInt64): UIntNative { /* compiled code */ }

    func saturatingShr(y: UInt64): UIntNative { /* compiled code */ }
}

func throwingAdd(x: T, y: T): T { /* compiled code */ }

func throwingSub(x: T, y: T): T { /* compiled code */ }

func throwingMul(x: T, y: T): T { /* compiled code */ }

func throwingDiv(x: T, y: T): T { /* compiled code */ }

func throwingMod(x: T, y: T): T { /* compiled code */ }

func throwingPow(x: Int64, y: UInt64): Int64 { /* compiled code */ }

func throwingInc(x: T): T { /* compiled code */ }

func throwingDec(x: T): T { /* compiled code */ }

func throwingNeg(x: T): T { /* compiled code */ }

interface ThrowingOp {
    func throwingAdd(y: T): T { /* compiled code */ }

    func throwingSub(y: T): T { /* compiled code */ }

    func throwingMul(y: T): T { /* compiled code */ }

    func throwingDiv(y: T): T { /* compiled code */ }

    func throwingMod(y: T): T { /* compiled code */ }

    func throwingInc(): T { /* compiled code */ }

    func throwingDec(): T { /* compiled code */ }

    func throwingNeg(): T { /* compiled code */ }

    func throwingShl(y: UInt64): T { /* compiled code */ }

    func throwingShr(y: UInt64): T { /* compiled code */ }
}

interface ThrowingPow {
    func throwingPow(y: UInt64): Int64 { /* compiled code */ }
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: Int8): Int8 { /* compiled code */ }

    func throwingSub(y: Int8): Int8 { /* compiled code */ }

    func throwingMul(y: Int8): Int8 { /* compiled code */ }

    func throwingDiv(y: Int8): Int8 { /* compiled code */ }

    func throwingMod(y: Int8): Int8 { /* compiled code */ }

    func throwingInc(): Int8 { /* compiled code */ }

    func throwingDec(): Int8 { /* compiled code */ }

    func throwingNeg(): Int8 { /* compiled code */ }

    func throwingShl(y: UInt64): Int8 { /* compiled code */ }

    func throwingShr(y: UInt64): Int8 { /* compiled code */ }
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: Int16): Int16 { /* compiled code */ }

    func throwingSub(y: Int16): Int16 { /* compiled code */ }

    func throwingMul(y: Int16): Int16 { /* compiled code */ }

    func throwingDiv(y: Int16): Int16 { /* compiled code */ }

    func throwingMod(y: Int16): Int16 { /* compiled code */ }

    func throwingInc(): Int16 { /* compiled code */ }

    func throwingDec(): Int16 { /* compiled code */ }

    func throwingNeg(): Int16 { /* compiled code */ }

    func throwingShl(y: UInt64): Int16 { /* compiled code */ }

    func throwingShr(y: UInt64): Int16 { /* compiled code */ }
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: Int32): Int32 { /* compiled code */ }

    func throwingSub(y: Int32): Int32 { /* compiled code */ }

    func throwingMul(y: Int32): Int32 { /* compiled code */ }

    func throwingDiv(y: Int32): Int32 { /* compiled code */ }

    func throwingMod(y: Int32): Int32 { /* compiled code */ }

    func throwingInc(): Int32 { /* compiled code */ }

    func throwingDec(): Int32 { /* compiled code */ }

    func throwingNeg(): Int32 { /* compiled code */ }

    func throwingShl(y: UInt64): Int32 { /* compiled code */ }

    func throwingShr(y: UInt64): Int32 { /* compiled code */ }
}

extend Extend <: ThrowingOp & ThrowingPow {
    func throwingAdd(y: Int64): Int64 { /* compiled code */ }

    func throwingSub(y: Int64): Int64 { /* compiled code */ }

    func throwingMul(y: Int64): Int64 { /* compiled code */ }

    func throwingDiv(y: Int64): Int64 { /* compiled code */ }

    func throwingMod(y: Int64): Int64 { /* compiled code */ }

    func throwingPow(y: UInt64): Int64 { /* compiled code */ }

    func throwingInc(): Int64 { /* compiled code */ }

    func throwingDec(): Int64 { /* compiled code */ }

    func throwingNeg(): Int64 { /* compiled code */ }

    func throwingShl(y: UInt64): Int64 { /* compiled code */ }

    func throwingShr(y: UInt64): Int64 { /* compiled code */ }
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: UInt8): UInt8 { /* compiled code */ }

    func throwingSub(y: UInt8): UInt8 { /* compiled code */ }

    func throwingMul(y: UInt8): UInt8 { /* compiled code */ }

    func throwingDiv(y: UInt8): UInt8 { /* compiled code */ }

    func throwingMod(y: UInt8): UInt8 { /* compiled code */ }

    func throwingInc(): UInt8 { /* compiled code */ }

    func throwingDec(): UInt8 { /* compiled code */ }

    func throwingNeg(): UInt8 { /* compiled code */ }

    func throwingShl(y: UInt64): UInt8 { /* compiled code */ }

    func throwingShr(y: UInt64): UInt8 { /* compiled code */ }
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: UInt16): UInt16 { /* compiled code */ }

    func throwingSub(y: UInt16): UInt16 { /* compiled code */ }

    func throwingMul(y: UInt16): UInt16 { /* compiled code */ }

    func throwingDiv(y: UInt16): UInt16 { /* compiled code */ }

    func throwingMod(y: UInt16): UInt16 { /* compiled code */ }

    func throwingInc(): UInt16 { /* compiled code */ }

    func throwingDec(): UInt16 { /* compiled code */ }

    func throwingNeg(): UInt16 { /* compiled code */ }

    func throwingShl(y: UInt64): UInt16 { /* compiled code */ }

    func throwingShr(y: UInt64): UInt16 { /* compiled code */ }
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: UInt32): UInt32 { /* compiled code */ }

    func throwingSub(y: UInt32): UInt32 { /* compiled code */ }

    func throwingMul(y: UInt32): UInt32 { /* compiled code */ }

    func throwingDiv(y: UInt32): UInt32 { /* compiled code */ }

    func throwingMod(y: UInt32): UInt32 { /* compiled code */ }

    func throwingInc(): UInt32 { /* compiled code */ }

    func throwingDec(): UInt32 { /* compiled code */ }

    func throwingNeg(): UInt32 { /* compiled code */ }

    func throwingShl(y: UInt64): UInt32 { /* compiled code */ }

    func throwingShr(y: UInt64): UInt32 { /* compiled code */ }
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: UInt64): UInt64 { /* compiled code */ }

    func throwingSub(y: UInt64): UInt64 { /* compiled code */ }

    func throwingMul(y: UInt64): UInt64 { /* compiled code */ }

    func throwingDiv(y: UInt64): UInt64 { /* compiled code */ }

    func throwingMod(y: UInt64): UInt64 { /* compiled code */ }

    func throwingInc(): UInt64 { /* compiled code */ }

    func throwingDec(): UInt64 { /* compiled code */ }

    func throwingNeg(): UInt64 { /* compiled code */ }

    func throwingShl(y: UInt64): UInt64 { /* compiled code */ }

    func throwingShr(y: UInt64): UInt64 { /* compiled code */ }
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: IntNative): IntNative { /* compiled code */ }

    func throwingSub(y: IntNative): IntNative { /* compiled code */ }

    func throwingMul(y: IntNative): IntNative { /* compiled code */ }

    func throwingDiv(y: IntNative): IntNative { /* compiled code */ }

    func throwingMod(y: IntNative): IntNative { /* compiled code */ }

    func throwingInc(): IntNative { /* compiled code */ }

    func throwingDec(): IntNative { /* compiled code */ }

    func throwingNeg(): IntNative { /* compiled code */ }

    func throwingShl(y: UInt64): IntNative { /* compiled code */ }

    func throwingShr(y: UInt64): IntNative { /* compiled code */ }
}

extend Extend <: ThrowingOp {
    func throwingAdd(y: UIntNative): UIntNative { /* compiled code */ }

    func throwingSub(y: UIntNative): UIntNative { /* compiled code */ }

    func throwingMul(y: UIntNative): UIntNative { /* compiled code */ }

    func throwingDiv(y: UIntNative): UIntNative { /* compiled code */ }

    func throwingMod(y: UIntNative): UIntNative { /* compiled code */ }

    func throwingInc(): UIntNative { /* compiled code */ }

    func throwingDec(): UIntNative { /* compiled code */ }

    func throwingNeg(): UIntNative { /* compiled code */ }

    func throwingShl(y: UInt64): UIntNative { /* compiled code */ }

    func throwingShr(y: UInt64): UIntNative { /* compiled code */ }
}

class OvershiftException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class UndershiftException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

func wrappingAdd(x: T, y: T): T { /* compiled code */ }

func wrappingSub(x: T, y: T): T { /* compiled code */ }

func wrappingMul(x: T, y: T): T { /* compiled code */ }

func wrappingDiv(x: T, y: T): T { /* compiled code */ }

func wrappingMod(x: T, y: T): T { /* compiled code */ }

func wrappingPow(x: Int64, y: UInt64): Int64 { /* compiled code */ }

func wrappingInc(x: T): T { /* compiled code */ }

func wrappingDec(x: T): T { /* compiled code */ }

func wrappingNeg(x: T): T { /* compiled code */ }

interface WrappingOp {
    func wrappingAdd(y: T): T { /* compiled code */ }

    func wrappingSub(y: T): T { /* compiled code */ }

    func wrappingMul(y: T): T { /* compiled code */ }

    func wrappingDiv(y: T): T { /* compiled code */ }

    func wrappingMod(y: T): T { /* compiled code */ }

    func wrappingInc(): T { /* compiled code */ }

    func wrappingDec(): T { /* compiled code */ }

    func wrappingNeg(): T { /* compiled code */ }

    func wrappingShl(y: UInt64): T { /* compiled code */ }

    func wrappingShr(y: UInt64): T { /* compiled code */ }
}

interface WrappingPow {
    func wrappingPow(y: UInt64): Int64 { /* compiled code */ }
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: Int8): Int8 { /* compiled code */ }

    func wrappingSub(y: Int8): Int8 { /* compiled code */ }

    func wrappingMul(y: Int8): Int8 { /* compiled code */ }

    func wrappingDiv(y: Int8): Int8 { /* compiled code */ }

    func wrappingMod(y: Int8): Int8 { /* compiled code */ }

    func wrappingInc(): Int8 { /* compiled code */ }

    func wrappingDec(): Int8 { /* compiled code */ }

    func wrappingNeg(): Int8 { /* compiled code */ }

    func wrappingShl(y: UInt64): Int8 { /* compiled code */ }

    func wrappingShr(y: UInt64): Int8 { /* compiled code */ }
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: Int16): Int16 { /* compiled code */ }

    func wrappingSub(y: Int16): Int16 { /* compiled code */ }

    func wrappingMul(y: Int16): Int16 { /* compiled code */ }

    func wrappingDiv(y: Int16): Int16 { /* compiled code */ }

    func wrappingMod(y: Int16): Int16 { /* compiled code */ }

    func wrappingInc(): Int16 { /* compiled code */ }

    func wrappingDec(): Int16 { /* compiled code */ }

    func wrappingNeg(): Int16 { /* compiled code */ }

    func wrappingShl(y: UInt64): Int16 { /* compiled code */ }

    func wrappingShr(y: UInt64): Int16 { /* compiled code */ }
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: Int32): Int32 { /* compiled code */ }

    func wrappingSub(y: Int32): Int32 { /* compiled code */ }

    func wrappingMul(y: Int32): Int32 { /* compiled code */ }

    func wrappingDiv(y: Int32): Int32 { /* compiled code */ }

    func wrappingMod(y: Int32): Int32 { /* compiled code */ }

    func wrappingInc(): Int32 { /* compiled code */ }

    func wrappingDec(): Int32 { /* compiled code */ }

    func wrappingNeg(): Int32 { /* compiled code */ }

    func wrappingShl(y: UInt64): Int32 { /* compiled code */ }

    func wrappingShr(y: UInt64): Int32 { /* compiled code */ }
}

extend Extend <: WrappingOp & WrappingPow {
    func wrappingAdd(y: Int64): Int64 { /* compiled code */ }

    func wrappingSub(y: Int64): Int64 { /* compiled code */ }

    func wrappingMul(y: Int64): Int64 { /* compiled code */ }

    func wrappingDiv(y: Int64): Int64 { /* compiled code */ }

    func wrappingMod(y: Int64): Int64 { /* compiled code */ }

    func wrappingPow(y: UInt64): Int64 { /* compiled code */ }

    func wrappingInc(): Int64 { /* compiled code */ }

    func wrappingDec(): Int64 { /* compiled code */ }

    func wrappingNeg(): Int64 { /* compiled code */ }

    func wrappingShl(y: UInt64): Int64 { /* compiled code */ }

    func wrappingShr(y: UInt64): Int64 { /* compiled code */ }
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: UInt8): UInt8 { /* compiled code */ }

    func wrappingSub(y: UInt8): UInt8 { /* compiled code */ }

    func wrappingMul(y: UInt8): UInt8 { /* compiled code */ }

    func wrappingDiv(y: UInt8): UInt8 { /* compiled code */ }

    func wrappingMod(y: UInt8): UInt8 { /* compiled code */ }

    func wrappingInc(): UInt8 { /* compiled code */ }

    func wrappingDec(): UInt8 { /* compiled code */ }

    func wrappingNeg(): UInt8 { /* compiled code */ }

    func wrappingShl(y: UInt64): UInt8 { /* compiled code */ }

    func wrappingShr(y: UInt64): UInt8 { /* compiled code */ }
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: UInt16): UInt16 { /* compiled code */ }

    func wrappingSub(y: UInt16): UInt16 { /* compiled code */ }

    func wrappingMul(y: UInt16): UInt16 { /* compiled code */ }

    func wrappingDiv(y: UInt16): UInt16 { /* compiled code */ }

    func wrappingMod(y: UInt16): UInt16 { /* compiled code */ }

    func wrappingInc(): UInt16 { /* compiled code */ }

    func wrappingDec(): UInt16 { /* compiled code */ }

    func wrappingNeg(): UInt16 { /* compiled code */ }

    func wrappingShl(y: UInt64): UInt16 { /* compiled code */ }

    func wrappingShr(y: UInt64): UInt16 { /* compiled code */ }
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: UInt32): UInt32 { /* compiled code */ }

    func wrappingSub(y: UInt32): UInt32 { /* compiled code */ }

    func wrappingMul(y: UInt32): UInt32 { /* compiled code */ }

    func wrappingDiv(y: UInt32): UInt32 { /* compiled code */ }

    func wrappingMod(y: UInt32): UInt32 { /* compiled code */ }

    func wrappingInc(): UInt32 { /* compiled code */ }

    func wrappingDec(): UInt32 { /* compiled code */ }

    func wrappingNeg(): UInt32 { /* compiled code */ }

    func wrappingShl(y: UInt64): UInt32 { /* compiled code */ }

    func wrappingShr(y: UInt64): UInt32 { /* compiled code */ }
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: UInt64): UInt64 { /* compiled code */ }

    func wrappingSub(y: UInt64): UInt64 { /* compiled code */ }

    func wrappingMul(y: UInt64): UInt64 { /* compiled code */ }

    func wrappingDiv(y: UInt64): UInt64 { /* compiled code */ }

    func wrappingMod(y: UInt64): UInt64 { /* compiled code */ }

    func wrappingInc(): UInt64 { /* compiled code */ }

    func wrappingDec(): UInt64 { /* compiled code */ }

    func wrappingNeg(): UInt64 { /* compiled code */ }

    func wrappingShl(y: UInt64): UInt64 { /* compiled code */ }

    func wrappingShr(y: UInt64): UInt64 { /* compiled code */ }
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: IntNative): IntNative { /* compiled code */ }

    func wrappingSub(y: IntNative): IntNative { /* compiled code */ }

    func wrappingMul(y: IntNative): IntNative { /* compiled code */ }

    func wrappingDiv(y: IntNative): IntNative { /* compiled code */ }

    func wrappingMod(y: IntNative): IntNative { /* compiled code */ }

    func wrappingInc(): IntNative { /* compiled code */ }

    func wrappingDec(): IntNative { /* compiled code */ }

    func wrappingNeg(): IntNative { /* compiled code */ }

    func wrappingShl(y: UInt64): IntNative { /* compiled code */ }

    func wrappingShr(y: UInt64): IntNative { /* compiled code */ }
}

extend Extend <: WrappingOp {
    func wrappingAdd(y: UIntNative): UIntNative { /* compiled code */ }

    func wrappingSub(y: UIntNative): UIntNative { /* compiled code */ }

    func wrappingMul(y: UIntNative): UIntNative { /* compiled code */ }

    func wrappingDiv(y: UIntNative): UIntNative { /* compiled code */ }

    func wrappingMod(y: UIntNative): UIntNative { /* compiled code */ }

    func wrappingInc(): UIntNative { /* compiled code */ }

    func wrappingDec(): UIntNative { /* compiled code */ }

    func wrappingNeg(): UIntNative { /* compiled code */ }

    func wrappingShl(y: UInt64): UIntNative { /* compiled code */ }

    func wrappingShr(y: UInt64): UIntNative { /* compiled code */ }
}
