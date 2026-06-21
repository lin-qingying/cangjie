


interface I0<T> {}

<!SUPER_TYPES_DUPLICATE!>class A<Q><!> <: I0<Q> & I0<Int64> {}

main() {
    var b: A<Int64>
    return 0
}
