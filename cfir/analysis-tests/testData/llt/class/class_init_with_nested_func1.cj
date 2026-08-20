

class A {
    public let a: Int32
    public <!CLASS_UNINITIALIZED_FIELD!>init<!>(b: Int64) {
        var f = {
            c: Int32 =>
            <!CAPTURE_BEFORE_INITIALIZATION!>a<!> = c
            return 1
        }
        f(9)
    }
}

main() {}
