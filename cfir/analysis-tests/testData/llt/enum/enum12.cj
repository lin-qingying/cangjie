


enum B<T> {
    a(Bool) | bNil
}

enum A<T> {
    a(T) | aNil
}

main(): Int64 {
    var i = a<Int64>(1)
    var j = a<Int64>(true)
    let ret1 = match (i) {
        case a(x) =>
            if (x == 1) {
                1
            } else {
                0
            }
        case <!UNREACHABLE_PATTERN!>aNil<!> => 0
    }
    let ret2 = match (j) {
        case a(x) =>
            if (x) {
                1
            } else {
                0
            }
        case <!UNREACHABLE_PATTERN!>bNil<!> => 0
    }
    if (ret1 + ret2 == 2) {
        return 0
    }
    return 1
}
