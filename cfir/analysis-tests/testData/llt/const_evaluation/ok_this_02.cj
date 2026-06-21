


open class C {
    const init() {}
}

class D <: C {
    const init() {
        super()
        const x = 1
        this
    }

    const func f() {
        const x = 2
    }
}

main() {
    const d = D()
    d.f()

    return 0
}
