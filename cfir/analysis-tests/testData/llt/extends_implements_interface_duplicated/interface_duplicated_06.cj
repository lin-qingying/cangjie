


interface I {}

interface I0<T> {}

interface I1<K> <: I0<K> & I0<Rune> {}

<!SUPER_TYPES_DUPLICATE!>class A<T><!> <: I & I1<T> & I {}

class B <: I1<Int64> & I1<Int32> {}

<!SUPER_TYPES_DUPLICATE!>class C<!> <: <!SUPER_TYPES_DUPLICATE!>I1<Rune><!> & <!SUPER_TYPES_DUPLICATE!>I1<Rune><!> {}

<!SUPER_TYPES_DUPLICATE!>interface I2<!> <: I & I {}

interface I4 <: I & I0<Int64> {}

main() {
    return 0
}
