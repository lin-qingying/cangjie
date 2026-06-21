

struct S {
    mut func f() {}
}

main() {
    S().<!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>f<!>()
}
