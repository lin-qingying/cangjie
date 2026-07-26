


interface I1<T> {
    func i1(a: Int32): Unit
    func i1(a: T): Unit
}

interface I2 <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>I1<Int32><!> {}

interface I3 <: I1<Int64> {}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>C1<!> <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>I1<Int32><!> {}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>C2<!> <: I1<Int64> {}

main() {
    return 0
}
