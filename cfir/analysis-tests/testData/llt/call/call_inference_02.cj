


class A<T> {
    public func test(a: Int64): Unit {}
}

extend<T> A<T> {
    public func test(): Unit {}
}

main(): Int64 {
    var a = A<Int64>()
    a.test<!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!><Int64><!>(1)
    a.test<!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!><Rune><!>()
    return 0
}
