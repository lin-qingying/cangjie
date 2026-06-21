


interface I<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, R> where T <: R {
    func foo(): R
}

class C<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>, R> <: I<T, R> where T <: R {
    var x: T
    init(_x: T) {
        x = _x
    }
    public func <!OVERRIDING_RETURN_TYPE_MISMATCH!>foo<!>(): T {
        x
    }
}

main() {
    0
}
