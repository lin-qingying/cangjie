

main(): Int64 {
    let x: Int32 = 1
    var a = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C(x)<!>
    return 0
}

interface B<T> {
    func foo(x: Int32): Int64 {
        return 1
    }
}

class C<T> <: B<T> {
    init(a: T) {}
    public func foo(x: T): Float64 {
        return .2
    }
}
