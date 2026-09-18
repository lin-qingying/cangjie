


class C1<T> {}

class C2<T> {}

interface A {
    func foo(a: C1<C2<Int32>>): Int32 {
        return 0
    }
}

interface B {
    func foo(a: C1<C2<Int32>>): Int32 {
        return 0
    }
}

class <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>D<!> <: A & B {}

main() {
    return 1
}
