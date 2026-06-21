

var g = 0

interface I {
    mut prop p: Int64 {
        get() {
            return 2
        }
        set(v) {
            g++
        } // side effect.
    }
}

struct R {}

extend R <: I {
    mut func f() {
        p = 0
        p
    }
}

class C {}

extend C <: I {
    func f() {
        p = 0
        p
    }
}

enum E {
    a
}

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend E <: I {<!>
    func f() {
        p = 0
        p
    }
}

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend Int32 <: I {<!>
    func f() {
        p = 0
        p
    }
}

main(): Int64 {
    var r = R()
    if (r.f() != 2 || g != 1 || r.p != 2) {
        return 1
    }
    var c = C()
    if (c.f() != 2 || g != 2 || c.p != 2) {
        return 2
    }
    var e = E.a
    if (e.f() != 2 || g != 3 || e.p != 2) {
        return 3
    }
    if (1.f() != 2 || g != 4 || 1.p != 2) {
        return 4
    }
    return 0
}
