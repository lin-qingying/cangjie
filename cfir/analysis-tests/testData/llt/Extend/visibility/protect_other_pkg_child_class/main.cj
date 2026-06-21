// FILE: pkg/pkg.cj


package pkg

var g: Int64 = 0

public open class C {}

extend C {
    protected func f() {
        10
    }
    protected mut prop p: Int64 {
        get() {
            g
        }
        set(v) {
            g++
        }
    }
}

// FILE: main.cj



internal import pkg.*

class C1 <: C {
    func g() {
        p = 0
        f() + p
    }
}

main() {
    let a = C1()
    if (a.g() != 11) {
        return 1
    }
    return 0
}
