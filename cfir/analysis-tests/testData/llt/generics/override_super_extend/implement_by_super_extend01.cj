


interface I1 {
    func foo(): Unit
}

interface I2 {}

open class A<T> {}

extend<T> A<T> where T <: I2 {
    public func foo(): Unit {
        println(2)
    }
}

class <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>B<!><T> <: A<T> & I1 {} // error

main() {}
