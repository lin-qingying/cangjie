


open class C {
    public func foo(a: Int32) {
        return a
    }
}

interface I<T> {
    func foo(a: T) {
        return a
    }
}

interface I2<T> {
    func foo(a: T) {
        return a
    }
}

func Foo<U>(a: U) where U <: C & I<U> & I2<U> {
    var c = a.foo<!WRONG_NUMBER_OF_ARGUMENTS!>(1, 2)<!>
    var d = a.<!GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS!>bar<!>()
    return <!UNRESOLVED_REFERENCE!>b<!>
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>C1<!> <: C & I<C1> & I2<C1> {}

main() {
    var a: C1 = Foo<C1>(C1())
    return 0
}
