


class A {
    let a: Int64
    let b: Int64

    const init() {
        (a, b) = (1, -1)
    }
}

const func bar(a: () -> Unit) {}

const func dot() {
    bar {=>}
}

main() {}
