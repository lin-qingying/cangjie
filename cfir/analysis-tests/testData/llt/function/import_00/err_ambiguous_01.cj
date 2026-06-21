// DIAGNOSTICS: -warnings
// FILE: m1/p1.cj


package m1.p1

public func f() {
    1
}

// FILE: m2/p1.cj


package m2.p1

public func f() {
    11
}

// FILE: err_ambiguous_01.cj


import m1.p1
import m2.p1

main() {
    println("${<!AMBIGUOUS_USE!>p1<!>.f()}")
}
