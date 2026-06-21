

class A {}

extend A {
    public func test<T>(a: T) {
        return a
    }
}

main(): Int64 {
    var a = A()
    if (a.test<Int64>(11) != 11) {
        return 1
    }
    return 0
}
