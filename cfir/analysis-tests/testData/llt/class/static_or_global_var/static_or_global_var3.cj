

class A {
    public <!CLASS_UNINITIALIZED_FIELD!>init<!>() {}

    public var a: UInt
    public mut static prop v: UInt64 {
        get() {
            ()
            UInt64(123)
            b
        }
        set(Value) {
            Int64(40)
            ()
            b = Value
        }
    }
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>b<!>: UInt64
    static init() {}
    // Should not report error bellow
    func goo() {
        get()
    }
    func get() {
        a
        this.a
    }
}

main(): Int64 {
    1
}
