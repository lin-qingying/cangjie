


interface I {
    func add() {
        print("I1")
    }
}

interface I2 {
    func add() {
        print("I2")
    }
}

class A {}

extend A <: I & I2 {
    public func add() {
        print("A")
    }
}

main() {
    let a: A = A()
    a.add()
    0
}
