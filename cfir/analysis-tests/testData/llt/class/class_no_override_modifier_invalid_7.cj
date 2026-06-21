


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

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class D<!> <: A & B<Int32> {}

main() {
    return 1
}
