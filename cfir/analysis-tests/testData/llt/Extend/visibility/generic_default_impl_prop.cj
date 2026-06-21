


interface I<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: UIntNative {
    prop b1: T {
        get() {
            <!CANNOT_CONVERT_LITERAL!>1<!>
        }
    }
}

extend<T> Option<T> <: I<UIntNative> {}

main() {
    var u1: I<UIntNative> = Some(1)
    return u1.b1 - 1
}
