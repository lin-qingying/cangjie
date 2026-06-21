

interface A {
    mut prop i: Int64
}

class B <: A {
    public mut prop i: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

main(): Int64 {
    return 0
}
