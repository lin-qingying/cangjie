

class A {}

class B<T> where T <: ToString {
    public static func test<T>(data: Array<T>): Unit where T <: Comparable<T> {}
    public static func foo(a: T) {}
}

main(): Int64 {
    var arr: Array<A> = [A()]
    <!UNABLE_TO_INFER_GENERIC_FUNC!>B.test(arr)<!>
    <!UNABLE_TO_INFER_GENERIC_FUNC!>B.foo(arr[0])<!>
    return 0
}
