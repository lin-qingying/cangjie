

interface A {
    func foo1(input: Bool): Int32
}

interface B {
    func foo1(input: Bool): Int32
}

interface C <: A & B {}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>D<!> <: C {}

abstract class E <: C {}
