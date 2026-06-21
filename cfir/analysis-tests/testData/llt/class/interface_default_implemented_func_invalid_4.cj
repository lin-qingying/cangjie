

interface B<T> {
    func foo(x: Int32): Int64 {
        return 1
    }
}

class C<T> <: B<T> {
    public func foo(x: T): Float64 {
        return .2
    }
}

func test<T>() {
    var a = C<T>()
}

main(): Int64 {
    var a = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>test<Int32><!>()
    return 0
}
