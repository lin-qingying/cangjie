

interface I1 {
    func foo(): Unit {
        println(1)
    }
}

interface I2<T> {}

open class A<T> {}

extend<T> A<T> where T <: I2<T> {
    public func foo(): Unit {
        println(2)
    }
}

class B<T> <: A<T> & I1 where T <: I2<T> {} // ok

class C <: I2<C> {}

main() {
    B<C>().foo()
    0
}
