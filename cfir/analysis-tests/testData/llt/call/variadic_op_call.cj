

class A {
    operator func ()(x: Array<Int64>) {
        x.size
    }
}

main() {
    let a = A()
    a(1, 2, 3)
}
