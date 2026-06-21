


class C1 {}

class C2 {}

class A<X> {
    public func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>Y<!>>(x: X) where Y <: X {
        x
    }
    public func foo<Y>() {}
    public static func goo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>Y<!>>(x: X) where Y <: X {
        x
    }
}

open class Base {}

open class Sub <: Base {}

class SSub <: Sub {}

class Sub2 <: Base {}

class B<X> where X <: Sub {
    public func f<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>Y<!>>(x: Y): X where Y <: X {
        x
    }
}

main(): Int64 {
    let a = A<C1>()
    a.foo<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>C<!>2>(C1())
    let b = a.<!AMBIGUOUS_USE!>foo<C2><!>
    let c = A<C1>.goo<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>C<!>2>

    B<Sub>().f<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>S<!>ub2>(Sub2())
    let x: Sub = B<Sub>().f<SSub>(SSub())
    0
}
