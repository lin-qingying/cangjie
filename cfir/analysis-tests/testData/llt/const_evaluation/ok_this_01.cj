


open class A {
    static const x = 0
}

class B <: A {
    func f() {
        const t = x
        t
    }
}

main() {
    B().f()
}
