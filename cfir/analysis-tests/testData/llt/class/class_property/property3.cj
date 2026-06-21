

class A {
    public mut prop a: Int64 {
        get() {
            return b
        }
        set(v) {
            return ()
        }
    }
    public var b = <!USED_BEFORE_INITIALIZATION!>a<!>
}
