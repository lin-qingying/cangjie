

struct S {
    mut func f() {}
    operator func +(_: Int64) {
        S()
    }
}

main() {
    var a = S()
    <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>(S() + 1)<!>.f()
}
