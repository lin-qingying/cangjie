enum NumericTag { A(Int64) | B(Int64) | C }

public func integerOr(): Int64 {
    let value = 1
    match (value) { case 1 | 2 => 1; case <!UNREACHABLE_PATTERN!>_<!> => 2 }
}
public func missedIntegerOr(): Int64 {
    let value = 3
    match (value) { case 1 | 2 => 1; case _ => 2 }
}
public func boolOr(): Int64 {
    let value = true
    match (value) { case true | false where false => 1; case _ => 2 }
}
public func optionOr(): Int64 {
    let value = Some(true)
    match (value) { case Some(_) | None where false => 1; case _ => 2 }
}
public func numericEnumOr(): Int64 {
    let value = A(1)
    match (value) { case A(_) | B(_) => 1; case <!UNREACHABLE_PATTERN!>C<!> => 2 }
}
public func duplicateOr(value: Int64): Int64 {
    match (value) { case 1 | <!UNREACHABLE_PATTERN!>1<!> => 1; case _ => 2 }
}
public func guardedDuplicateOr(value: Int64, flag: Bool): Int64 {
    match (value) { case 1 | <!UNREACHABLE_PATTERN!>1<!> where flag => 1; case _ => 2 }
}
public func wildcardOr(value: Int64): Int64 {
    match (value) { case _ | <!UNREACHABLE_PATTERN!>_<!> => 1 }
}
public func tupleOr(value: (Bool, Bool)): Int64 {
    match (value) { case (_, _) | <!UNREACHABLE_PATTERN!>(true, _)<!> => 1 }
}
public func coveredOr(value: Int64): Int64 {
    match (value) { case _ => 1; case <!UNREACHABLE_PATTERN!>1 | 2<!> => 2 }
}
public func guardThenCondition(value: Int64): Int64 {
    match (value) { case 1 where true => 1; case <!UNREACHABLE_PATTERN!>2<!> => 2; case <!UNREACHABLE_PATTERN!>_<!> => 3 }
}
public func skippedGuard(): Int64 {
    var count = 0
    let selected = match (0) {
        case <!UNREACHABLE_PATTERN!>1<!> where ((count = 1) == ()) => 1
        case _ => 2
    }
    match (count) {
        case 0 => selected
        case <!UNREACHABLE_PATTERN!>_<!> => 0
    }
}

func chooseGuard(f: () -> Int64): Bool { f() == 1 }
func chooseGuard(f: () -> String): Bool { f() == "a" }
public func overloadedGuard(value: Int64): Int64 {
    match (value) {
        case 0 where chooseGuard({ => 1 }) => 1
        case _ => 2
    }
}
