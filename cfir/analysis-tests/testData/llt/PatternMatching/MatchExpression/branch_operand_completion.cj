func same<T>(value: T): T { value }
public func rightMatch(value: UInt8, flag: Bool): UInt8 {
    value + match (flag) { case true => 1; case false => 2 }
}
public func leftMatch(value: UInt8, flag: Bool): UInt8 {
    match (flag) { case true => 1; case false => 2 } + value
}
public func rightGenericMatch(value: UInt8, flag: Bool): UInt8 {
    value + match (flag) { case true => same(1); case false => same(2) }
}
public func leftGenericMatch(value: UInt8, flag: Bool): UInt8 {
    match (flag) { case true => same(1); case false => same(2) } + value
}
public func rightIf(value: UInt8, flag: Bool): UInt8 {
    value + if (flag) { same(1) } else { same(2) }
}
public func leftIf(value: UInt8, flag: Bool): UInt8 {
    (if (flag) { same(1) } else { same(2) }) + value
}
public func rightTry(value: UInt8): UInt8 {
    value + try { 1 } catch (_: Exception) { 2 }
}
public func leftTry(value: UInt8): UInt8 {
    (try { 1 } catch (_: Exception) { 2 }) + value
}
public func localCompound(flag: Bool) {
    var value: UInt8 = 1
    value *= match (flag) { case true => 2; case false => 3 }
}
public func indexedCompound(flag: Bool) {
    let value = Array<UInt8>(1, repeat: 1)
    value[0] *= match (flag) { case true => same(2); case false => same(3) }
}
public func rightEvaluationOrder(): Int64 {
    var value: UInt8 = 1
    let result = value + match (true) {
        case true => value = 2; 3
        case <!UNREACHABLE_PATTERN!>false<!> => 4
    }
    match (result) { case 4 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func leftEvaluationOrder(): Int64 {
    var value: UInt8 = 1
    let result = match (true) {
        case true => value = 2; 3
        case <!UNREACHABLE_PATTERN!>false<!> => 4
    } + value
    match (result) { case 5 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func inferredRightGenericMatch(value: UInt8, flag: Bool) {
    value + match (flag) { case true => same(1); case false => same(2) }
}
public func inferredLeftGenericMatch(value: UInt8, flag: Bool) {
    match (flag) { case true => same(1); case false => same(2) } + value
}
public func inferredRightGenericIf(value: UInt8, flag: Bool) {
    value + if (flag) { same(1) } else { same(2) }
}
public func inferredLeftGenericIf(value: UInt8, flag: Bool) {
    (if (flag) { same(1) } else { same(2) }) + value
}
