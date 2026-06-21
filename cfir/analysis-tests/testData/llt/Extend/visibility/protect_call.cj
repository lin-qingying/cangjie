

open class C {}

extend C {
    protected func f() {
        1
    }
}

class C1 <: C {}

main() {
    let a = C()
    if (a.f() != 1) {
        return 1
    }
    let b = C1()
    if (b.f() != 1) {
        return 2
    }
    return 0
}
