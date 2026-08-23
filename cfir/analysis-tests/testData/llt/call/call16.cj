


class A<T> {
    public func test(a: T) {}
    public func test(a: Int16) {}
    public func i() {
        let I = test(<!ARGUMENT_TYPE_MISMATCH!>Int64(1)<!>)
    }
}

main(): Int64 {
    A<Int32>().i()
    return 0
}

