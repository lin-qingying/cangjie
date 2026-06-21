

interface I1 {
    func foo() {
        println("I1 foo")
    }
}

interface I2 <: I1 {
    func foo() {
        println("I2 foo")
    }
}

class A {}

extend A <: I1 {}

extend A <: I2 {}

main() {
    A().foo() // I1 foo
}
