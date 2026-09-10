public func literalConditionChain(): Int64 {
    let value = (true, 1)
    match (value) {
        case (true, 1) => 0
        case <!UNREACHABLE_PATTERN!>(false, 2)<!> => 1
        case <!UNREACHABLE_PATTERN!>_<!> => 2
    }
}

public func orConditionChain(): Int64 {
    let value = (true, true)
    match (value) {
        case (true, true) => 0
        case <!UNREACHABLE_PATTERN!>(true, false)<!> | <!UNREACHABLE_PATTERN!>(false, true)<!> => 1
        case <!UNREACHABLE_PATTERN!>_<!> => 2
    }
}
