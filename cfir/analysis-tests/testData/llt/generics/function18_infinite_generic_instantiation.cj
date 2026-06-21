


class Base<T> {}

func buildList<T>(n: Int32): Int64 {
    if (n > 0) {
        <!GENERIC_INFINITE_INSTANTIATION!>b<!>uildList<Base<T>>(n - 1)
    } else {
        buildList<Base<T>>(0)
    }
    return 0
}

main(): Int64 {
    return buildList<Int32>(10)
}
