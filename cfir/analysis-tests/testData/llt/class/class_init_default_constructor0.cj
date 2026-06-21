

class A {
    public var a: Int64 = <!USED_BEFORE_INITIALIZATION!>b<!>
    public var b: Int64 = 1
}
