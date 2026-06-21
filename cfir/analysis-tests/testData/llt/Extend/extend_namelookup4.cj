

open class B {
    public let a: Int64 = 1
}

class A <: B {}

extend A {
    func foo(): Int64 {
        return a - this.a
    }
}

main(): Int64 {
    let a = A()
    return a.foo()
}
