


struct R1 {
    public var i = 0

    public mut func foo() {
        i += 1
    }
}

class A<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: R1 {
    func structMut<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>X<!>>(obj: X): Unit where X <: T {
        <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>obj.foo()<!>
        var r = obj
        let _ = <!USE_MUTABLE_FUNC_ALONE!>r.foo<!>
    }
}

main() {
    return 0
}
