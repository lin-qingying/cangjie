
interface I<T> {
    func foo1(a:T):Int64
    func foo2(a:T):Int64
    func foo3():Int64
}

class C<T, V> {} 

class A{}
class B{}

<!NEED_MEMBER_IMPLEMENTATION!>e<!>xtend C<A, B> <: I<A> & I<B> {}

main() {
    return 0
}

