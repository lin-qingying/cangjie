

class A {
    operator func [](x: Array<Int8>) {
        x.size
    }
    operator func [](x: Array<Int8>, value!: Int64) {
        println(x.size + value)
    }
}

main() {
    let a = A()
    a[1] = a[1, 2, 3, 4]
}
