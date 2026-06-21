

main(): Int64 {
    var a = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>A<Int32><!>()
    var b = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>B<Int32><!>()
    return 0
}

class A<T> {
    public func foo(x: Int32) {
        return 1
    }
}

extend<T> A<T> {
    public func foo(x: T) {
        return 2
    }
}

struct B<T> {
    public func foo(x: Int32) {
        return 1
    }
}

extend<T> B<T> {
    public func foo(x: T) {
        return 2
    }
}
