


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

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>D<!> <: A & B<Int32> {}

main() {
    return 1
}
