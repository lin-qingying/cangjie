


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

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class B<T><!> <: A<T> & I1 {} // error

main() {}
