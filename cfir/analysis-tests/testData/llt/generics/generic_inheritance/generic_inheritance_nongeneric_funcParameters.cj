


main(): Int64 {
    if (B<String, Int64>().m != 1) {
        return 1
    }
    return 0
}

interface I<T> {
    func foo(): Int64 {
        return 1
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>interface J<T, V><!> <: I<T> & I<V> {}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>B<!><T, V> <: I<T> & I<V> {
    public prop m: Int64 {
        get() {
            return foo()
        }
    }
}
