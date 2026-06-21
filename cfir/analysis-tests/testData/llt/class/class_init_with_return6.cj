


class A {
    public init() {
        if (true) {
            return <!RETURN_TYPE_MISMATCH!>A()<!>
        }
    }
}
