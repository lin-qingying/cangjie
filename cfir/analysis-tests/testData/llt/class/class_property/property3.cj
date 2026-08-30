

class A {
    public mut prop a: Int64 {
        get() {
            return b
        }
        set(v) {
            return ()
        }
    }
    public var b = <!ILLEGAL_USAGE_OF_MEMBER!>a<!>
}
