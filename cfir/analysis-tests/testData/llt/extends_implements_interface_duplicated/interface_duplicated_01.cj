


interface I0<T> {}

class A<Q> <: I0<Q> & I0<Int64> {}

main() {
    var b = <!SUPER_TYPES_DUPLICATE!>A<Int64><!>()
    return 0
}
