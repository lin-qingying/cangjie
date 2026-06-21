

class A {
    public var a: Int64
    public var b: Int64
    public init() {
        a = <!USED_BEFORE_INITIALIZATION!>b<!>
        b = 1
    }
}
