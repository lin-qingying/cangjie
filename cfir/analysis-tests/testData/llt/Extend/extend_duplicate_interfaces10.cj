


main(): Int64 {
    var a = B<String, Int32>() // ok
    var b = <!SUPER_TYPES_DUPLICATE!>B<String, String><!>() // error
    return 0
}

interface I<T> {}

class B<T, V> {}

extend<T, V> B<T, V> <: I<T> & I<V> {}
