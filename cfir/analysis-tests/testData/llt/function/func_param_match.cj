


func foo(a: Float32) {
    0
}

main() {
    let a: Float32 = <!TYPE_MISMATCH!>1<!>
    return foo(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
}
