


interface I {
    func f(): Int64
}

class A {}

extend A {
    public func f() {
        1
    }
}

extend A <: I {}

main() {
    return A().f() - 1
}
