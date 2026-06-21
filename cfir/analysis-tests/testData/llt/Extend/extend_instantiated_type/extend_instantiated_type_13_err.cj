
interface I<T> {
    func foo1(a:T):Int64
    func foo2(a:T):Int64
    func foo3():Int64
}

class C<T, V> {} 

<!NEED_MEMBER_IMPLEMENTATION!>e<!>xtend<T,V> C<T, V> <: I<T> & I<V> {}

main() {
    return 0
}

