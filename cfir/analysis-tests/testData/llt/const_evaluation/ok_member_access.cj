


class A {
    let b = B()
    const init() {}
}

class B {
    let c = 0
    const init() {}
}

const func f() {
    let a = A()
    a.b.c
}

main() {
    f()
}
