enum Tag { Hit(Int64) | Miss }

public func nestedMismatch(): Int64 {
    let value = ((1, 2), 3)
    match (value) { case ((4, _), _) => 1; case _ => 2 }
}

public func nestedAndTop(): Int64 {
    let value = ((1, 2), 3)
    match (value) { case ((4, _), 3) => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}

public func topMismatch(): Int64 {
    let value = (1, 2)
    match (value) { case <!UNREACHABLE_PATTERN!>(4, _)<!> => 1; case _ => 2 }
}

public func stringComparison(): Int64 {
    let value = "a"
    match (value) { case "b" => 1; case _ => 2 }
}

public func directEnum(): Int64 {
    let value = Hit(1)
    match (value) { case <!UNREACHABLE_PATTERN!>Miss<!> => 1; case Hit(_) => 2 }
}

public func projectedEnum(): Int64 {
    let value = (Hit(1), 0)
    match (value) { case (Miss, _) => 1; case (Hit(_), _) => 2 }
}
