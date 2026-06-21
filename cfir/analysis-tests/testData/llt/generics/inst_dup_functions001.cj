

class A<T> {
    public func foo(a: Int32) {}
    public func foo(a: T) {}
}

interface B<T> {
    func foo(a: Int32): Unit
    func foo(a: T): Unit
}

struct C<T> {
    public func foo(a: Int32) {}
    public func foo(a: T) {}
}

main() {
    var a = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>A<Int32><!>()
    var b: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>B<Int32><!>
    var c = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<Int32><!>()
    return 0
}
