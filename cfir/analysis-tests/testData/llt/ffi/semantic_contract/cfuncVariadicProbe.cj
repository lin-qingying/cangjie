foreign func variadic(format: CPointer<UInt8>, ...): Int32

unsafe main() {
    let inferred = <!CFUNC_VAR_CANNOT_HAVE_VAR_PARAM!>variadic<!>
    0
}
