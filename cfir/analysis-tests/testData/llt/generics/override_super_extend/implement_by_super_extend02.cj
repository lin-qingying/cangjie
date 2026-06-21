

interface I1 {
    func foo(): Unit
}

open class A<T> {}

extend<T> A<T> {
    public func foo(): Unit {
        println(2)
    }
}

class B<T> <: A<T> & I1 {} // ok

main() {
    B<Int64>().foo()
    0
}
