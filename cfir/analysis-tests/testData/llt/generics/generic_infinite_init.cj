


class A<T> {
    public func test(a: <!GENERIC_INFINITE_INSTANTIATION!>A<!><(A<T>, A<T>)>): Bool {
        true
    }
}

main(): Int64 {
    var a: A<Int32> = A<Int32>()
    return 0
}
