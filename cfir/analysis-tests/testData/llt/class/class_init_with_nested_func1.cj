

class A {
    public let a: Int32
    <!CLASS_UNINITIALIZED_FIELD!>public init(b: Int64)<!> {
        var f = {
            c: Int32 =>
            <!CAPTURE_BEFORE_INITIALIZATION!>a<!> = c
            return 1
        }
        f(9)
    }
}

main() {}
