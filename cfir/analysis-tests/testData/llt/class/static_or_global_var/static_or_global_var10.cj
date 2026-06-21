

class A {
    <!TYPE_UNINITIALIZED_STATIC_FIELD!>public static var ctx: Int64<!>

    static init() {
        let l1 = {=> println(ctx)}
        let l2 = {=> ctx = 10}
        l2()
        l1()
    }
}

main() {}
