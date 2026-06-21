


class A {
    let x: Int64
    const init() {
        x = 0
    }
}

const func f() {
    let a = A()
    a.x
}

main() {
    f()
}
