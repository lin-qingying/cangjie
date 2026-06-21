

open class A {
    protected var b: Int64 = 1
}

class B <: A {
    public func test() {
        return A().b
    }
}

main(): Int64 {
    if (B().test() == 1) {
        return 0
    }
    return 0
}
