



abstract class A {}

abstract <!REDUNDANT_MODIFIER!>open<!> class B {}

class C <: A {}

class D <: B {}

main() {
    return 0
}

