

class A<T> {}

interface Foo {
    func foo(): Unit {}
}

extend<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> A<T> <: Foo where T <: Int64 {}

func boo(x: Foo) {}

main(): Int64 {
    let a = A<Int32>()
    boo(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
    a.<!NOT_MEMBER_OF!>foo<!>()
    return 0
}
