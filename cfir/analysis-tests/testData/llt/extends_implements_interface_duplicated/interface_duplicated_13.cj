


<!INHERITANCE_CYCLE!>i<!>nterface I1<T> <: I3<T> {}

interface I2<K> <: I1<K> {}

interface I3<Q> <: I2<Q> & <!EXTEND_DUPLICATE_INTERFACE!>I<!>2<Int32> {}

main(): Int64 {
    var i3: <!EXTEND_DUPLICATE_INTERFACE!>I<!>3<Int32>
    return 0;
}
