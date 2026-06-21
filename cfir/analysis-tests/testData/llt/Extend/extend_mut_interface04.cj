

interface I {
    mut prop p: Int64 {
        get() {
            1
        }
        set(_) {}
    }
}

interface I2 <: I {}

struct A {}

extend A <: I2 {}

class B {}

extend B <: I2 {}

main() {}
