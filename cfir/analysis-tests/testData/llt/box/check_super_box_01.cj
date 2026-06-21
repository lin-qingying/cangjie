

interface I {}

open class A <: I {}

class B <: A {}

interface I1 {}

class C <: A & I1 {}

main() {
    let ins: Any = B()
    let ins1: I1 = C()
    let v1 = ins is A && ins1 is A
    let v2 = ins is I
    if (!v1) {
        return 1
    } else if (!v2) {
        return 2
    }
    return 0
}
