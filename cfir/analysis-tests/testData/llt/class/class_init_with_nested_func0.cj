

class A {
    public let a: Int32
    <!CLASS_UNINITIALIZED_FIELD!>public init(b: Int64)<!> {
        func f(): Int32 {
            <!CAPTURE_BEFORE_INITIALIZATION!>a<!> = 8
            return 1
        }
        f()
    }
}
