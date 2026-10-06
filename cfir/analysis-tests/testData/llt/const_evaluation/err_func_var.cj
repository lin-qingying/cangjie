

const func f() {
    <!CANNOT_DEFINE_VAR_IN_CONST_FUNCTION!>var x = 0<!>
    x
}

main() {
    f()
}
