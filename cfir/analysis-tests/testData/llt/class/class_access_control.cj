

class A {
    <!TYPE_UNINITIALIZED_STATIC_FIELD!>private static let a: Int64<!>
    private operator func +(b: A, c: A) {}
}

main() {
    A.a
    A() + A()
    return 0
}
