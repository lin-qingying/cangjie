

struct S {
    mut func f() {}
    operator func +(_: Int64) {
        S()
    }
}

main() {
    var a = S()
    (S() + 1).<!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>f<!>()
}
