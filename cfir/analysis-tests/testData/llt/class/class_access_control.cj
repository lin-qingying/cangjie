

class A {
    private static let <!TYPE_UNINITIALIZED_STATIC_FIELD!>a<!>: Int64
    private <!INVALID_OPERATOR_PARAMETER_COUNT!>operator<!> func +(b: A, c: A) {}
}

main() {
    <!INVALID_ACCESS_CONTROL!>A<!>.a
    A() <!INVALID_BINARY_OPERATOR!>+<!> A()
    return 0
}
