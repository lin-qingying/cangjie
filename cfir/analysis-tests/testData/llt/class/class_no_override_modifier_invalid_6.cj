


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

class <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>D<!> <: A & B {}

main() {
    return 1
}
