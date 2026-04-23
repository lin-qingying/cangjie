interface I<T> {
    func foo1(a:T):Int64
    func foo2(a:T):Int64
    func foo3():Int64
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C<T, V><!> <: I<T> & I<V> {} 
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class D<K><!> <: I<K> {}

main() {
    return 0
}
