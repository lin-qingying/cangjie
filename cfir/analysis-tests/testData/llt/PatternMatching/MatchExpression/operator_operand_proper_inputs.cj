// proper source 实参不能被 operator 的借用目标收窄；用户 operator 保留确定返回类型。
func id<T>(value: T): T { value }
func choose<T>(left: T, right: T): T { left }

class PrimitiveResult {
    public operator func +(other: PrimitiveResult): Int64 { 7 }
    public operator func *(other: PrimitiveResult): Float64 { 3.5 }
}

public func properIdRight(byte: UInt8, wide: Int64) {
    byte <!INVALID_BINARY_OPERATOR!>+<!> id(wide)
}
public func properIdLeft(byte: UInt8, wide: Int64) {
    id(wide) <!INVALID_BINARY_OPERATOR!>+<!> byte
}
public func properChooseRight(byte: UInt8, wide: Int64) {
    byte <!INVALID_BINARY_OPERATOR!>*<!> choose(wide, 1)
}
public func properChooseLeft(byte: UInt8, wide: Int64) {
    choose(1, wide) <!INVALID_BINARY_OPERATOR!>*<!> byte
}
public func properMatchRight(byte: UInt8, wide: Int64, flag: Bool) {
    byte <!INVALID_BINARY_OPERATOR!>+<!> match (flag) { case true => id(wide); case false => choose(wide, 2) }
}
public func properMatchLeft(byte: UInt8, wide: Int64, flag: Bool) {
    match (flag) { case true => choose(2, wide); case false => id(wide) } <!INVALID_BINARY_OPERATOR!>+<!> byte
}
public func properIfRight(byte: UInt8, wide: Int64, flag: Bool) {
    byte <!INVALID_BINARY_OPERATOR!>+<!> if (flag) { id(wide) } else { choose(wide, 3) }
}
public func properIfLeft(byte: UInt8, wide: Int64, flag: Bool) {
    (if (flag) { choose(3, wide) } else { id(wide) }) <!INVALID_BINARY_OPERATOR!>+<!> byte
}
public func userIntegerAtExplicitTarget(): UInt8 {
    <!RETURN_TYPE_MISMATCH!>PrimitiveResult() + PrimitiveResult()<!>
}
public func userFloatAtExplicitTarget(): Float32 {
    <!RETURN_TYPE_MISMATCH!>PrimitiveResult() * PrimitiveResult()<!>
}
public func userIntegerAsByteOperand(byte: UInt8) {
    byte <!INVALID_BINARY_OPERATOR!>+<!> (PrimitiveResult() + PrimitiveResult())
}
public func userFloatAsFloat32Operand(value: Float32) {
    value <!INVALID_BINARY_OPERATOR!>*<!> (PrimitiveResult() * PrimitiveResult())
}
