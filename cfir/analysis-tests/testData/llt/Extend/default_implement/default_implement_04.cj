

interface I {
    static func f() {
        0
    }
}

interface J <: I {
    static func f() {
        1
    }
}

class A <: I {}

extend A <: J {}

main() {
    return A.f()
}
