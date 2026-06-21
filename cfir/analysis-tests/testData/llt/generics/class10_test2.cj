


class A<T> {
    public func test(a: A<T>, b: <!GENERIC_INFINITE_INSTANTIATION!>A<!><(A<T>, A<T>)>): Bool {
        true
    }
}

main(): Int64 {
    var a: A<Int32> = A<Int32>()
    var b: A<(A<Int32>, A<Int32>)> = A<(A<Int32>, A<Int32>)>()
    A<Int32>().test(a, b)
    return 0
}
