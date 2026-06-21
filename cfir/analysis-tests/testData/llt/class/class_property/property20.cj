

open class A {
    public mut static prop v: Int64 {
        get() {
            return 2
        }
        set(v) {}
    }
}

class B <: A {
    public mut static prop v: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

main() {
    return B.v - 1
}
