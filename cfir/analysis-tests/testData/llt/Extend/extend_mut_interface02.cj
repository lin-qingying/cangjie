

interface I {
    mut prop p: Int64 {
        get() {
            1
        }
        set(_) {}
    }
}

struct A {}

extend A <: I {}

class B {}

extend B <: I {}

main() {}
