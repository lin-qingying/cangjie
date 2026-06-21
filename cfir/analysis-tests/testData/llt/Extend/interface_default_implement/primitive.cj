

interface I {
    func f() {
        2
    }
}

extend Int64 <: I {
    func g() {
        f() * 10
    }
}

main(): Int64 {
    return 1.f() + 1.g() - 22
}
