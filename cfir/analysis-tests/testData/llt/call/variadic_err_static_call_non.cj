


class A {
    static func f() {
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>g<!>()
    }

    func g(_: Array<Int64>) {}
}

main() {
    A.f()
}
