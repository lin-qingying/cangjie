struct S0 {
    var t0 = 0
}

struct S1 {
    mut prop t1: S0 {
        get() {
            S0()
        }
        set(value) {}
    }

    mut func f() {
        t1.t0 += 1
    }
}

main() {}
