

class A {
    <!TYPE_UNINITIALIZED_STATIC_FIELD!>public static var ctx: Int64<!>

    static init() {
        let l2 = {=> ctx = 10}
    }
}

main() {}
