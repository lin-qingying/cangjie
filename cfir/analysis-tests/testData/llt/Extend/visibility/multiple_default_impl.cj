

interface I1 {
    func foo2() {
        11
    }
}

interface I2 <: I1 {
    func foo2() {
        0
    }
}

interface I3 {}

extend Int64 <: I2 & I3 {}

main() {
    return 1.foo2()
}
