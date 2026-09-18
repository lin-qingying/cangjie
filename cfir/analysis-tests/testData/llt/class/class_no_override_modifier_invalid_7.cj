


interface A {
    func foo(a: Int32): Int32 {
        return a
    }
}

interface B<T> {
    func foo(a: T): T {
        return a
    }
}

class <!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>D<!> <: A & B<Int32> {}

main() {
    return 1
}
