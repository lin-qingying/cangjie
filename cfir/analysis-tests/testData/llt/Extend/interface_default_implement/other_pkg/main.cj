// FILE: pkg/pkg.cj


package pkg

var x = 2

public interface I {
    func f() {
        x
    }
}

// FILE: main.cj


internal import pkg.*

struct C {}

extend C <: I {
    func g() {
        f() * 10
    }
}

main(): Int64 {
    var c = C()
    return c.f() + c.g() - 22
}
