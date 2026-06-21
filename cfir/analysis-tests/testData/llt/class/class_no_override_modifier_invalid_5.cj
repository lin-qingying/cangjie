


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

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class D<!> <: A & B {}

main() {
    return 1
}
