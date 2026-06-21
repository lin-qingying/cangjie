// FILE: pkg/pkg.cj


package pkg

var g = 0

public interface I {
    mut prop p: Int64 {
        get() {
            return g
        }
        set(v) {
            g++
        } // side effect.
    }
}

// FILE: main.cj


internal import pkg.*

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

main(): Int64 {
    var r = R()
    if (r.f() != 1 || r.p != 1) {
        return 1
    }
    var c = C()
    if (c.f() != 2 || c.p != 2) {
        return 2
    }
    var e = E.a
    if (e.f() != 3 || e.p != 3) {
        return 3
    }
    return 0
}
