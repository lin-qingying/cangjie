

interface I0 {}

open class A {}

extend A <: I0 {}

class B <: A {}

interface I1 {}

extend B <: I1 {}

main() {
    let ins0: Any = B()
    let ins1: I1 = B()
    let v1 = ins0 is I0
    let v2 = ins1 is I0
    if (!v1) {
        return 1
    } else if (!v2) {
        return 2
    }
    return 0
}
