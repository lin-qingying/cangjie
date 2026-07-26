interface I<T> {
    func foo1(a:T):Int64
    func foo2(a:T):Int64
    func foo3():Int64
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>C<!><T, V> <: I<T> & I<V> {}
class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>D<!><K> <: I<K> {}

main() {
    return 0
}
