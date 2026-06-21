

var g: Int64 = 0

open class C {}

extend C {
    protected mut prop p: Int64 {
        get() {
            g
        }
        set(v) {
            g++
        }
    }
}

class C1 <: C {}

main() {
    let a = C()
    a.p = 0
    if (a.p != 1) {
        return 1
    }
    let b = C1()
    b.p = 0
    if (b.p != 2) {
        return 2
    }
    return 0
}
