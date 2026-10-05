

const func f() {
    var <!CANNOT_DEFINE_VAR_IN_CONST_FUNCTION!>x<!> = 0
    x
}

main() {
    f()
}
