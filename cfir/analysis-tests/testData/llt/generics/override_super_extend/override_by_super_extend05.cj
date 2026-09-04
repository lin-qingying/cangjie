


interface I1 {
    func foo(): Unit {
        println(1)
    }
}

interface I2<T> {}

interface I3<T> <: I2<T> {}

open class A<T> {}

extend<T> A<T> where T <: I3<T> {
    public func foo(): Unit {
        println(2)
    }
}

class <!CANNOT_OVERRIDE!>B<!><T> <: A<T> & I1 where T <: I2<T> {} // error

class C <: I3<C> {}

main() {
    B<C>().foo()
    0
}
