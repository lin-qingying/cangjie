

class A {
    protected prop f: String {
        get() {
            return "C"
        }
    }
}

extend A {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>f<!>(): Int64 {
        0
    }
}
