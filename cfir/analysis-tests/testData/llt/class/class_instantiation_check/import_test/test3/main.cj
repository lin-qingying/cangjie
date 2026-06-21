// FILE: pkga/pkga.cj


package pkga

public open class C<T> {
    public static func c1(a: Int32): Int32 {
        0
    }
    public static func c1(a: T): Int32 {
        0
    }
}

// FILE: main.cj



import pkga.C

class C1 <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<Int32><!> {}

class C4 <: C<Int64> {}

open class C2<T> <: C<T> {}

class C3 <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C2<Int32><!> {}

class C5 <: C2<Int64> {}

main() {
    var a1 = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<Int32><!>()
    var a2 = C<Int64>()
    var b1 = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C2<Int32><!>()
    var b2 = C2<Int64>()
    return 0
}
