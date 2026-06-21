

class A {
    operator func [](arg1: Int64, value!: Int64): Unit {}
}

main() {
    var b = A()
    <!CANNOT_ASSIGN_TO_SUBSCRIPT!>b[r'c']<!> = 3
    0
}
