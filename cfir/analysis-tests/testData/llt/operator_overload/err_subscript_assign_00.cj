

class A {
    operator func [](index: Int64, value: Int64) {}
}

main() {
    var a = A()
    <!CANNOT_ASSIGN_TO_SUBSCRIPT!>a[1]<!> = 4
}
