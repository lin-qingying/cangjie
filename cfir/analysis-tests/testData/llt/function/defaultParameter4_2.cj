


func foo(a!: Int32 = 1, b!: Int32 = 1) {
    return a + b
}

main() {
    var x = foo
    var res = x(a: 2, b: 2)
    if (res != 4) {
        return 1
    }
    return 0
}

