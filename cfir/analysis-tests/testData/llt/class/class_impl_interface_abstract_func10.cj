

interface I<T> {
    func foo(a: T): Int64
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>C<!><T, V> <: I<T> & I<V> {}

main() {
    let c = C<Int64, String>()
    return 0
}
