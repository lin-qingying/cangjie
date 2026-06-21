


main() {
    if (false) {
        None<Int64>
    } else {
        func func1<T0>(): Int64 {
            Int64(1)
        };
        func1<Int8>();
        Some(0)
    } ?? if (true) {
        func func1<T0>(): Int64 {
            Int64(1)
        };
        func1<Int8>();
        -1
    } else {
        -1
    }
}
