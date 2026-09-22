


<!INHERITANCE_CYCLE!>interface I1<T><!> <: I3<T> {}

interface I2<K> <: I1<K> {}

interface I3<Q> <: I2<Q> & <!SUPER_TYPES_DUPLICATE!>I2<Int32><!> {}

main(): Int64 {
    var i3: <!SUPER_TYPES_DUPLICATE!>I3<Int32><!>
    return 0;
}
