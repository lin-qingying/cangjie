

interface A {
    func foo1(input: Bool): Int32
}

interface B {
    func foo1(input: Bool): Int32
}

interface C <: A & B {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class D<!> <: C {}

abstract class E <: C {}
