


open class A {
    public static var x: Int64 = 5
    private mut static prop v: Int64 {
        get() {
            return x
        }
        set(v) {
            x = v
        }
    }
}

class B <: A {
    public mut static <!NOTHING_TO_OVERRIDE!>redef<!> prop v: Int64 {
        set(x) {
        }
    }
}

main(): Int64 {
    return 0
}
