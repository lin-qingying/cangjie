

class A {
    public let a: Int32
    public <!CLASS_UNINITIALIZED_FIELD!>init<!>(b: Int64) {
        func f(): Int32 {
            <!CAPTURE_BEFORE_INITIALIZATION!>a<!> = 8
            return 1
        }
        f()
    }
}
