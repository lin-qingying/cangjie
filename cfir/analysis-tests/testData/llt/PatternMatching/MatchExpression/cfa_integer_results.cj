public func arithmetic(): Int64 {
    var x = 0
    let y = x + 2
    match (y - 1) { case 1 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func conversion(): Int64 {
    let value = Int16(-6)
    match (value) { case <!UNREACHABLE_PATTERN!>-121<!> => 1; case _ => 2 }
}
public func spelling(): Int64 {
    let value: Int16 = 1i16
    match (value) { case 1 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func matchResult(value: Bool): Int64 {
    let result = match (value) { case true => 1; case false => 1 }
    match (result) { case 1 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func chosenResult(): Int64 {
    let result = match (1) { case 1 => 7; case <!UNREACHABLE_PATTERN!>_<!> => 8 }
    match (result) { case 7 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func enumResult(value: Bool): Int64 {
    let result = match (value) { case true => Some(1); case false => Some(2) }
    match (result) { case Some(_) => 1; case <!UNREACHABLE_PATTERN!>None<!> => 2 }
}
public func differentResult(value: Bool): Int64 {
    var x = 1
    let result = match (value) {
        case true => x = 2; x
        case false => x = 3; x
    }
    match (result) { case 2 => 1; case 3 => 2; case _ => 3 }
}
public func signedOperations(): Int64 {
    var x: Int16 = -7
    let y = (x / 3) * 4 + (x % 3)
    match (y) { case -9 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func signedBits(): Int64 {
    var x: Int8 = 1
    let y = (x << 7) >> 6
    match (y) { case -2 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func unsignedBits(): Int64 {
    var x: UInt8 = 128
    let y = (x << 1) | 3
    let z = (y & 1) ^ 2
    match (!z) { case 252 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func power(): Int64 {
    var x: Int64 = -2
    match (x ** 3u64) { case -8 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func trivialProduct(value: Int64): Int64 {
    match (value * 0) { case 0 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func trivialRemainder(value: Int64): Int64 {
    match (value % 1) { case 0 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func trivialPower(value: Int64): Int64 {
    match (value ** 0u64) { case 1 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func originalOperand(): Int64 {
    var x = 1
    let result = x + match (true) {
        case true => x = 2; 3
        case <!UNREACHABLE_PATTERN!>false<!> => 4
    }
    match (result) { case 4 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func tupleResult(value: Bool): Int64 {
    let result = match (value) {
        case true => (1, 2)
        case false => (1, 3)
    }
    match (result) { case (1, _) => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func branchTail(value: Bool): Int64 {
    var x = 0
    let result = match (value) {
        case true => x = 2; x - 1
        case false => x = 3; x - 2
    }
    match (result) { case 1 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func integerComparison(): Int64 {
    var x = 1
    match (x < 2) { case true => 1; case <!UNREACHABLE_PATTERN!>false<!> => 2 }
}
public func integerEquality(): Int64 {
    var x = 1
    match (x == 2) { case <!UNREACHABLE_PATTERN!>true<!> => 1; case false => 2 }
}
func opaqueValue(value: Int64): Int64 { value + 1 }
public func opaqueResult(): Int64 {
    match (opaqueValue(1)) { case 2 => 1; case _ => 2 }
}
