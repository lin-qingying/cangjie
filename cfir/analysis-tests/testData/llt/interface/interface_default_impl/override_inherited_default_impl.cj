

var gv = 0

interface I {
    func asFunc() {
        1
    }
    mut prop v: Int64 {
        get() {
            1
        }
        set(i) {
            gv = 2
        }
    }
}

open class A <: I {}

class B <: A {
    public func asFunc() {
        2
    }
    public mut prop v: Int64 {
        get() {
            2
        }
        set(i) {
            gv = 3
        }
    }
}

main() {
    var b: A = B()
    if (b.asFunc() != 2) {
        return 1
    }
    if (b.v != 2) {
        return 1
    }
    b.v = 2
    if (gv != 3) {
        return 1
    }
    return 0
}
