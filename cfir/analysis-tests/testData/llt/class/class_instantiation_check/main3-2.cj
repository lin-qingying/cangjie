

open class C<T> {
    public func c1(a: Int32): Int32 {
        0
    }
    public func c1(a: T): Int32 {
        0
    }
}

class C1 <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<Int32><!> {}

class C2 <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<Int32><!> {}

main() {
    return 0
}
