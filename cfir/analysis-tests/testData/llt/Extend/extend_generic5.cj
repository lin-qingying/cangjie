


open class A<T> {}

interface EQ {}

extend Int32 <: EQ {}

extend<T> A<T> where T <: EQ {
    public func soo() {}
    func add(a: T) {}
}

class B<E> <: A<E> { // generic-E does not satisfy the constraint T <: EQ
    func test(a: E) {
        <!UNRESOLVED_REFERENCE!>soo<!>()
        <!UNRESOLVED_REFERENCE!>add<!>(a)
    }
}
