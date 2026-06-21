

struct A {
    var b: Int64 = 0
}

main() {
    var a: ?A = A()
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>a?.b<!> = 9
    return
}
