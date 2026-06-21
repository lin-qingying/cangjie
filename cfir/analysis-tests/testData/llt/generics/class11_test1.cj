


struct B<T> {}

class A<T> {
    public operator func [](b: <!GENERIC_INFINITE_INSTANTIATION!>A<!><B<T>>): Bool {
        true
    }
    public func test(a: A<T>, b: A<B<T>>): Bool {
        true
    }
}

main(): Int64 {
    var a: A<B<Int32>> = A<B<Int32>>()
    var b: A<Int32> = A<Int32>()
    b[a]
    A<Int32>().test(b, a)
    1
}
