


interface I1 {
    func foo(): Int
}

interface I2 {
    func foo(): String
}

class A<T> where T <: I2 {
    func test<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>X<!>>() where <!INHERIT_MEMBER_TYPE_INCONSISTENT!>X <: I1 & T<!> {}
}

func f<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T1<!>, <!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T2<!>, T3>() where <!INHERIT_MEMBER_TYPE_INCONSISTENT!>T1 <: T2 & I1<!>, T2 <: T3, T3 <: I2 {}

main() {
}
