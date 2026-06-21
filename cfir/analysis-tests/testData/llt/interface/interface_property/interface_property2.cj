

interface I {
    prop a: Int64
}

class RR <: I {
    public var aa: Int64 = 98
    public prop a: Int64 {
        get() {
            return aa
        }
    }
}

class R {
    public var a: I = RR()
}

main() {
    var b = R()
    print("${b.a.a}")
    return b.a.a - 98
}
