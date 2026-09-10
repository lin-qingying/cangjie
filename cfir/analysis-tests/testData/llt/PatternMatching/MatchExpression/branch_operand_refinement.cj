func same<T>(value: T): T { value }
func choose<T>(left: T, right: T): T { left }

struct Value {
    public operator func +(other: Value): Value { other }
}

func overloadedFlow(flag: Bool) {
    match (flag) { case true => same(Value()); case false => Value() } + Value()
}

func localGenericFlow(value: UInt8, flag: Bool) {
    match (flag) {
        case true =>
            func local<T>(item: T): T { item }
            local(1)
        case false => 2
    } + value
}

func bothControlFlow(flag: Bool) {
    match (flag) { case true => same(1); case false => same(2) } +
        match (flag) { case true => same(3); case false => same(4) }
}

public func failedGenericRefinement(value: UInt8, wide: Int64, flag: Bool) {
    value <!INVALID_BINARY_OPERATOR!>+<!> match (flag) { case true => choose(1, wide); case false => choose(2, wide) }
}
