


class A<T> {
    public func f(o: T) {
        0
    }
    public func g() {
        var x = A<Object>()
        x.f(Object())
        0
    }
}

main(): Int64 {
    A<Int32>().g()
    return 0
}
