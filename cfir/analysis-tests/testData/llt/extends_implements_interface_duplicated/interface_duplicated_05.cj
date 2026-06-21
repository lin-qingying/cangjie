


interface I0<T> {}

interface I1<K> <: I0<K> & I0<Rune> {}

<!SUPER_TYPES_DUPLICATE!>class A<Q><!> <: I1<Q> {
    public func doit(): Int64 {
        return 5
    }
}

main() {
    if (A<Rune>().doit() == 5) {
        return 0
    }
    return 1
}
