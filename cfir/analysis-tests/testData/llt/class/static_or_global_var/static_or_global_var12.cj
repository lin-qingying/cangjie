

class A {
    public static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>ctx<!>: Int64

    static init() {
        let l2 = {=> ctx = 10}
    }
}

main() {}
