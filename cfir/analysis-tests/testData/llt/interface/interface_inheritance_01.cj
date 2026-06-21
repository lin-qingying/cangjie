

class A <: ToString {
    public func toString() {
        "A"
    }
}

interface I0 {
    func test(): ToString
}

interface I1 {
    func test(): A {
        A()
    }
}

class B <: I0 & I1 {}

main(): Int64 {
    let ins = B()
    if (ins.test().toString() != "A") {
        return 1
    }
    return 0
}
