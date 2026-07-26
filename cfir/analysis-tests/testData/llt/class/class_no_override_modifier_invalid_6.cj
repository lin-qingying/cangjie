


class C1<T, X> {}

interface A {
    func foo(a: C1<Int32, Int64>): Int32 {
        return 0
    }
}

interface B {
    func foo(a: C1<Int32, Int64>): Int32 {
        print("calling foo() in B\n")
        return 1
    }
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>D<!> <: A & B {}

main() {
    return 1
}
