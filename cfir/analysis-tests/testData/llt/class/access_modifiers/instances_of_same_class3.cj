

struct A {
    private var b: Int64 = 1
    public func test() {
        foo().b
    }
}

func foo() {
    A()
}

class B {
    private var b: Int64 = 1
    public func test() {
        foo1().b
    }
}

func foo1() {
    B()
}

main(): Int64 {
    if (A().test() == 1 && B().test() == 1) {
        return 0
    }
    return 1
}
