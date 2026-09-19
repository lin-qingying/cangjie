// FILE: pkg.cj


package pkg

public let a = 0
public let b = 1

// FILE: main.cj



internal import pkg.a

main() {
    println(<!UNRESOLVED_REFERENCE!>pkg<!>.b)
}
