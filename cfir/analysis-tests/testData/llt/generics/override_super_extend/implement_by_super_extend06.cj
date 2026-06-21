

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

open class D<T> <: A<I3> {}

class B<T> <: D<T> & I1 {} // ok

class C <: I3 {}

main() {
    B<C>().foo()
    0
}
