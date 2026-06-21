

interface I {
    mut prop i: Int64 {
        get() {
            return 2
        }
        set(v) {}
    }
}

interface I1 {
    mut prop i: Int64
}

class B <: I1 & I {}

main(): Int64 {
    if (B().i != 2) {
        return 4
    }
    print("${B().i}")
    return 0
}
