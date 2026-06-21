

open class Base {}

class Sub <: Base {}

class A<X> {
    public func f<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>Y<!>>(x: Y): X where Y <: X {
        x
    }
}

class B<X> where X <: Base {
    public func f<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>Y<!>>(x: Y): X where Y <: X {
        x
    }
}

main() {
    A<Int64>().f<Int64>(1)
    B<Base>().f<Sub>(Sub())
    0
}
