

open class A {
    private var b: Int64 = 1
    public func test() {
        return A().b
    }
    public func test1(a: A) {
        return a.b
    }
}

main(): Int64 {
    var c = A()
    if (A().test() == 1 && A().test1(c) == 1) {
        return 0
    }
    return 1
}
