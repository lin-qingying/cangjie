public func minimumInteger(): Int64 {
    let value: Int64 = -9223372036854775808
    match (value) { case -9223372036854775808 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func maximumUnsigned(): Int64 {
    let value: UInt64 = 0xffff_ffff_ffff_ffffu64
    match (value) { case 18446744073709551615 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func minimumPower(): Int64 {
    var value: Int64 = -2
    match (value ** 63u64) { case -9223372036854775808 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func nestedConversion(): Int64 {
    var value = -128
    let converted = Int8(Int16(value))
    match (converted) { case -128 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func contextualOperand(): Int64 {
    var x = 1
    let result: Int64 = x + match (true) {
        case true => x = 2; 3
        case <!UNREACHABLE_PATTERN!>false<!> => 4
    }
    match (result) { case 4 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func loopResult(): Int64 {
    var i = 0
    var last = 0
    while (i < 2) {
        last = match (i) { case 0 => 1; case _ => 2 }
        i++
    }
    match (last) { case 1 => 1; case 2 => 2; case _ => 3 }
}
public func floatConversion(): Int64 {
    var i = 1
    match (Float64(i)) { case 1.0 => 1; case _ => 2 }
}
