

interface I1 {
    func foo() {
        1
    }
}

interface I2 <: I1 {
    func foo() {
        2
    }
}

class A {}

extend A <: I2 {}

extend A <: I1 {}

class B {}
// Diff order.
extend B <: I1 {}

extend B <: I2 {}

main() {
    println(A().foo())
    println(B().foo())
}
