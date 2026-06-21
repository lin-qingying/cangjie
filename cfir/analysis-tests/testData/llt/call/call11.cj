

func test<T>(input: Int64): Int64 {
    return input
}

main() {
    var a = <!UNABLE_TO_INFER_GENERIC_FUNC!>test(1)<!>
    return a
}
