


interface I0<T> {}

interface I1<K> <: I0<K> & I0<Rune> {}

class A<Q> <: I1<Q> {
    public func doit(): Int64 {
        return 5
    }
}

main() {
    var j = <!SUPER_TYPES_DUPLICATE!>A<Rune><!>()
    return 0
}
