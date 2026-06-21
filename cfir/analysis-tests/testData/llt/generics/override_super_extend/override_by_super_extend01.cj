

interface I1 {
    func foo(): Unit {
        println(1)
    }
}

interface I2 {}

open class A<T> {}

extend<T> A<T> where T <: I2 {
    public func foo(): Unit {
        println(2)
    }
}

class <!EXTEND_FUNCTION_CANNOT_OVERRIDDEN!>B<T> <: A<T> & I1 {}<!> // error

main() {}
