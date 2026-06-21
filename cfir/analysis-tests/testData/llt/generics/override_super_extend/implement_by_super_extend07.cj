


interface I1 {
    func foo(): Unit
}

interface I2 {}

interface I3 <: I2 {}

open class A<T> {}

extend<T> A<T> where T <: I2 {
    public func foo(): Unit {
        println(2)
    }
}

class B<T1> <: A<T1> {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend<T2> B<T2> <: I1 {}<!> // error

class C<T1> <: A<T1> {}

extend<T2> C<T2> <: I1 where T2 <: I2 {} // ok

class D<T1> <: A<T1> where T1 <: I2 {}

extend<T2> D<T2> <: I1 {} // ok

main() {
    0
}
