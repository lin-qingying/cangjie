

open class A {
    public var a: Int64 = 1
    public mut open prop i: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

class B <: A {
    public mut override prop i: Int64 {
        set(v) {}
    }
}

main(): Int64 {
    if (A().i != 1) {
        return 3
    }
    if (B().i != 1) {
        return 4
    }
    return 0
}
