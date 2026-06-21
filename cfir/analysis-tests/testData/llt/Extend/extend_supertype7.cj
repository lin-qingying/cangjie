


interface I {
    func foo(): Unit
}

open class A {
    protected func foo(): Unit {}
}

class B <: A {}

extend <!WEAK_VISIBILITY!>B<!> <: I {}
