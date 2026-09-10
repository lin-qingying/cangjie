func declarationType(): Unit {
    let <!TUPLE_PATTERN_NOT_MATCH!>(a, b)<!> = 1
    print(a)
}

func declarationSize(): Unit {
    let <!TUPLE_PATTERN_WITH_CORRECT_SIZE_EXPECTED!>(a, b)<!> = (1, 2, 3)
    print(a)
}

func declarationNested(): Unit {
    let <!MISMATCHED_TYPE_FOR_PATTERN_IN_VARDECL!>(<!TYPE_MISMATCH!>(a, b)<!>, c)<!> = (1, 2)
    print(c)
}

func validNested(): Int64 {
    let ((a, b), c) = ((1, 2), 3)
    a + b + c
}

func matchType(value: Int64): Unit {
    match (value) {
        case <!TYPE_MISMATCH!>(a, b)<!> => print(<!UNRESOLVED_REFERENCE!>a<!>)
    }
}

func matchSize(value: (Int64, Int64)): Unit {
    match (value) {
        case <!TUPLE_PATTERN_WITH_CORRECT_SIZE_EXPECTED!>(a, b, c)<!> => print(<!UNRESOLVED_REFERENCE!>a<!>)
    }
}

func letCondition(value: Int64): Unit {
    if (let <!TYPE_MISMATCH!>(a, b)<!> <- value) { print(<!UNRESOLVED_REFERENCE!>a<!>) }
}

func typedSize(value: (Int64, Int64)): Unit {
    match (value) {
        case <!TUPLE_PATTERN_WITH_CORRECT_SIZE_EXPECTED!>(a: Int64, b: Int64, c: Int64)<!> => print(a)
    }
}

func typedType(value: Int64): Unit {
    match (value) {
        case <!TYPE_MISMATCH!>(a: Int64, b: Int64)<!> => print(a)
    }
}

type PairAlias = (Int64, Int64)

func declarationAlias(): Int64 {
    let (a, b): PairAlias = (1, 2)
    a + b
}

func matchAlias(value: PairAlias): Unit {
    match (value) {
        case <!TUPLE_PATTERN_WITH_CORRECT_SIZE_EXPECTED!>(_, _, _)<!> => ()
    }
}
