


main() {
    let v1 = match (if (true) {
        func func1<T0>(): Int64 {
            Int64(1)
        };
        func1<Int8>();
        0
    } else {
        -1
    }) {
        case a: Equatable<Int64> =>
            if (a == 0) {
                0
            } else {
                -1
            }
        case <!UNREACHABLE_PATTERN!>_<!> => 1
    }
    let v2 = match (if (true) {
        func func1<T0>(): Int64 {
            Int64(1)
        };
        func1<Int8>();
        v1
    } else {
        v1
    }) {
        case a: Int64 => 0
        case <!UNREACHABLE_PATTERN!>_<!> => 1
    }
    v2
}
