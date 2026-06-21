


interface A<R> {}

interface Test<T, R> {
    static func test(t: R): <!GENERIC_ARGUMENT_NO_MATCH!>T<R><!> {}
    func test1(t: T): A<T>
}

main(): Int64 {
    return 0
}
