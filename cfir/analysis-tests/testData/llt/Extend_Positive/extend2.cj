


interface I {
    func add1(): Unit
}

interface I2 {
    func add2(): Unit
}

struct A {}

extend A <: I & I2 {
    public func add1() {
        print("1")
    }
    public func add2() {
        print("2")
    }
    public func add3() {
        print("3")
    }
}

main() {
    let a: A = A()
    a.add1()
    a.add2()
    a.add3()
    0
}
