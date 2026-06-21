

class A {
    operator func [](arg1: Int64, arg2: String): Int64 {
        return 1
    }
}

main() {
    let a = A()
    return a[1, "2"] - 1
}
