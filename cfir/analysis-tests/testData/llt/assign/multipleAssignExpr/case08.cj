

class A {
    operator func [](index: Int64): A {
        A()
    }
    operator func [](index: Int64, value!: Int64): Unit {}
}

main() {
    var val = 1
    var a = A()
    (val, a[0]) = (2, 3)
    return val - 2
}
