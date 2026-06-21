


interface I0<T> {}

interface I1<K> <: I0<K> & I0<Rune> {}

interface I2<Q> <: I1<Q> & I0<Int64> {}

interface I3<P> <: I2<Int32> {}

main() {
    var i2: I2<Int64>
    var i1: <!SUPER_TYPES_DUPLICATE!>I1<Rune><!>
    return 0
}
