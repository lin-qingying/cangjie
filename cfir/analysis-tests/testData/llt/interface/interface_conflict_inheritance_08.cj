


interface I1 {
    func foo(): Int
}

interface I2 {
    func foo(): String
}

class A<T> where T <: I2 {
    func test<X>() where X <: I1 & <!INHERIT_SUPER_MEMBER_KIND_INCONSISTENT, UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!> {}
}

func f<T1, T2, T3>() where T1 <: <!INHERIT_SUPER_MEMBER_KIND_INCONSISTENT, UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T2<!> & I1, T2 <: <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T3<!>, T3 <: I2 {}

main() {
}
