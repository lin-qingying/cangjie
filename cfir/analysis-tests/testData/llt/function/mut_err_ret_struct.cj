

struct S {
    mut func f() {}
}

main() {
    <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>S()<!>.f()
}
