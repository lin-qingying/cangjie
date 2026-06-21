

struct A {
    public var a: Int32 = 3
    public mut prop m: Int32 {
        get() {
            return a
        }
        set(v) {
            a = v
        }
    }
    public mut func f(v: Int32) {
        a = v
        0
    }
}

main(): Int64 {
    var a = A()
    a.m = 4
    if (a.m != 4) {
        return 1
    }
    return 0
}
