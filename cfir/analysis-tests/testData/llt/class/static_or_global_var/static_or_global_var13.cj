

class A {
    public static var ctx: Int64 = 1

    static init() {
        let l2 = {=> ctx = 10}
    }
}

class B {
    public static var ctx : Int64

    static init() {
        ctx = 10
        let l1 = {=> println(ctx)}
        let l2 = {=> ctx = 10}
        l1()
        l2()
    }
}

main() {}
