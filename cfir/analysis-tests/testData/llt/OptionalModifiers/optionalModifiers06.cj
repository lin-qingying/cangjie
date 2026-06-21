


interface I1 {
    func f() {}
}

open interface I2 {
    func f() {}
}

interface I3 <: I2 {}

main() {
    return 0
}
