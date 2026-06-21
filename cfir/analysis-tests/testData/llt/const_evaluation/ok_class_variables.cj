


class A {
    let x = 0
    let y = 1

    const func f() {
        x + y
    }

    const init() {}
}

main() {
    const a = A()
    a.f()
}
