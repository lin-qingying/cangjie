main(): Int64 {
    var x = Boo()
    if (x.a != 2) {
        return 1
    }
    if (x.b != 1) {
        return 2
    }
    if (x.c != ()) {
        return 3
    }
    return 0
}

struct Boo {
    public var a: Int32 = 1
    public mut prop m: Int32 {
        get() {
            return a
        }
        set(input) {
            a = input + 1
        }
    }
    public prop n: Int32 {
        get() {
            return a
        }
    }
    public var b: Int32 = if (true) {
        a
    } else {
        0
    }
    public var c: Unit = if (true) {
        m = 1
    }
}
