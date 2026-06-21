

interface I {
    func f(): Unit
}

abstract class A <: I {}

extend A {}

main() {
    return 0
}
