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

// FILE: ok.cj



import m1.p1
import m2.p1 as p2

main() {
    println("${p1.f()} ${p2.f()}")
}
