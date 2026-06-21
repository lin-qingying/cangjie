

open class A {
    public mut static prop v: Int64 {
        get() {
            return 2
        }
        set(v) {}
    }
}

class B <: A {
    public mut static redef prop v: Int64 {
        set(v) {
        }
    }
    public func f() {
        return v
    }
}

main(): Int64 {
    if (A.v == 2 && B.v == 2 && B().f() == 2) {
        return 0
    }
    return 1
}
