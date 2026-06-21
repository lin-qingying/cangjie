

enum E {
    A
    | A(Array<Int64>)

    operator func ()(x: Array<Int8>, a!: Int64, b!: Array<Int32>) {
        x.size + a + b.size
    }
}

main() {
    A(1, 2, a: 0, b: [])
}
