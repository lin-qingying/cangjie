


struct A {
    var b: Int64 = 0
}

main() {
    var a: ?A = A()
    a?.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 9
    return
}
