


interface I1<T> {
    func i1(a: Int32): Unit
    func i1(a: T): Unit
}

interface I2 <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>I1<Int32><!> {}

interface I3 <: I1<Int64> {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C1<!> <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>I1<Int32><!> {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C2<!> <: I1<Int64> {}

main() {
    return 0
}
