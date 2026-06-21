// FILE: pkg/pkg.cj


package pkg

interface I {
    static func x(): Int64 {
        return 1
    }
}

// FILE: main.cj



internal import pkg.*

main(): Int64 {
    var x: Int64 = <!UNRESOLVED_REFERENCE!>I<!>.x()
    return 0
}
