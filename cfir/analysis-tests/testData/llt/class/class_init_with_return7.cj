


class A {
    public init(a: Int64, b: Int64, c: Int64) {
        while (a > 5) {
            if (b > 5) {
                if (c < 5) {
                    return <!RETURN_TYPE_MISMATCH!>A(a, b, c)<!>
                }
            }
        }
    }
}
