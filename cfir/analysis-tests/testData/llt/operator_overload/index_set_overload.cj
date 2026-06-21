

class A {
    operator func [](arg1: Int64, arg2: String, value!: Int64): Unit {}
}

main() {
    let a = A()
    a[1, "2"] = 1
    return 0
}
