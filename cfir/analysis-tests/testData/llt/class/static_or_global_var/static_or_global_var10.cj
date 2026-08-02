

class A {
    public static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>ctx<!>: Int64

    static init() {
        let l1 = {=> println(<!USED_BEFORE_INITIALIZATION!>ctx<!>)}
        let l2 = {=> ctx = 10}
        l2()
        l1()
    }
}

main() {}
