func signed(value: Int16): Int64 {
    match (value) {
        case -121 => 1
        case <!UNREACHABLE_PATTERN!>-121i16<!> => 2
        case _ => 3
    }
}

func radix(value: Int64): Int64 {
    match (value) {
        case -0x10 => 1
        case <!UNREACHABLE_PATTERN!>-16<!> => 2
        case _ => 3
    }
}

func signedBoundary(value: Int64): Int64 {
    match (value) {
        case -9223372036854775808 => 0
        case _ => 1
    }
}

func unsignedBoundary(value: UInt64): Int64 {
    match (value) {
        case 18446744073709551615u64 => 0
        case _ => 1
    }
}

func nestedTuple(value: (Int16, Int16)): Int64 {
    match (value) {
        case (-1, -2) => 0
        case (_, _) => 1
    }
}

func nestedEnum(value: Option<Int16>): Int64 {
    match (value) {
        case Some(-1) => 0
        case Some(_) => 1
        case None => 2
    }
}

func conditions(value: Int16): Unit {
    if (let -1 <- value) { print(value) }
    while (let -2 <- value) { break }
}
