

interface I0 {}

open class A <: I0 {}

class B <: A {}

interface I {}

extend B <: I {}

main() {
    let ins: I = B()
    let v1 = ins is A
    let v2 = ins is I0
    if (!v1) {
        return 1
    } else if (!v2) {
        return 2
    }
    return 0
}
