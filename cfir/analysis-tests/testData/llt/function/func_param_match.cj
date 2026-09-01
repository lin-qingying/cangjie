


func foo(a: Float32) {
    0
}

main() {
    let a: Float32 = <!TYPE_MISMATCH!>1<!>
    return foo(<!CANNOT_CONVERT_LITERAL!>1<!>)
}
