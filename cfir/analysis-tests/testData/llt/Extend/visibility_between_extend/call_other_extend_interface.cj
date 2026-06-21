


struct A {}

interface I1 {
    static func g() {}
}

extend A <: I1 {}

interface I2 {}

extend A <: I2 {
    static func f() {
        return g()
    }
}

main() {
    return 0
}
