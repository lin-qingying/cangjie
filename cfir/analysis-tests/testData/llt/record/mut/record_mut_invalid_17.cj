


struct R1 {
    public var i = 0

    public mut func foo() {
        i += 1
    }
}

func structMut<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>(obj: T): Unit where T <: R1 {
    <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>obj.foo()<!>
    var r = R1()
    let _ = <!USE_MUTABLE_FUNC_ALONE!>r.foo<!>
}

main() {
    return 0
}
