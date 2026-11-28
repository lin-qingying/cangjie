// 无后缀 ideal 可使用 UInt8 operator 目标；class operator 的 primitive 结果仍来自用户方法。
func id<T>(value: T): T { value }
func choose<T>(left: T, _: T): T { left }

class UserNumber {
    public operator func +(_: UserNumber): Int64 { 7 }
    public operator func *(_: UserNumber): Float64 { 3.5 }
}

public func idealIdRight(byte: UInt8) { byte + id(1) }
public func idealIdLeft(byte: UInt8) { id(1) + byte }
public func idealChooseRight(byte: UInt8) { byte * choose(2, 3) }
public func idealChooseLeft(byte: UInt8) { choose(3, 4) * byte }
public func idealMatchRight(byte: UInt8, flag: Bool) {
    byte + match (flag) { case true => id(1); case false => choose(2, 3) }
}
public func idealMatchLeft(byte: UInt8, flag: Bool) {
    match (flag) { case true => choose(2, 3); case false => id(1) } + byte
}
public func userPlus(): Int64 { UserNumber() + UserNumber() }
public func userMultiply(): Float64 { UserNumber() * UserNumber() }
public func userPlusAsPrimitiveOperand(): Int64 {
    (UserNumber() + UserNumber()) + 1
}
public func userMultiplyAsPrimitiveOperand(): Float64 {
    (UserNumber() * UserNumber()) * 2.0
}
public func userLeftFlowPlus(flag: Bool): Int64 {
    match (flag) { case true => id(UserNumber()); case false => UserNumber() } + UserNumber()
}
public func userRightFlowMultiply(flag: Bool): Float64 {
    UserNumber() * match (flag) { case true => UserNumber(); case false => choose(UserNumber(), UserNumber()) }
}
public func userOperatorAsGenericInput(): Int64 {
    id(UserNumber() + UserNumber())
}

main(): Int64 {
    let byte: UInt8 = 1
    let a: UInt8 = idealIdRight(byte)
    let b: UInt8 = idealIdLeft(byte)
    let c: UInt8 = idealChooseRight(byte)
    let d: UInt8 = idealChooseLeft(byte)
    let e: UInt8 = idealMatchRight(byte, true)
    let f: UInt8 = idealMatchLeft(byte, false)
    if (a != 2 || b != 2 || c != 2 || d != 3 || e != 2 || f != 2) { return 1 }
    if (userPlus() != 7 || userMultiply() != 3.5) { return 2 }
    if (userPlusAsPrimitiveOperand() != 8 || userMultiplyAsPrimitiveOperand() != 7.0) { return 3 }
    if (userLeftFlowPlus(true) != 7 || userRightFlowMultiply(false) != 3.5) { return 4 }
    if (userOperatorAsGenericInput() != 7) { return 5 }
    return 0
}
