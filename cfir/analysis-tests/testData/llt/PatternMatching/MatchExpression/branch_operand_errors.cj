func wideValue(): Int64 { 1 }
public func concreteCall(value: UInt8) {
    value <!INVALID_BINARY_OPERATOR!>+<!> wideValue()
}
public func declaredValue(value: UInt8) {
    value <!INVALID_BINARY_OPERATOR!>+<!> match (true) { case _ => let result = 1; result }
}
public func suffixedValue(value: UInt8) {
    value <!INVALID_BINARY_OPERATOR!>+<!> match (true) { case _ => 1i32 }
}
public func branchCall(value: UInt8, flag: Bool) {
    value <!INVALID_BINARY_OPERATOR!>+<!> match (flag) { case true => wideValue(); case false => wideValue() }
}
