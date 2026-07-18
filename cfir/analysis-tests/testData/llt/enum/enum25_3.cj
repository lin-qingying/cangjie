

enum A {
    a | b(Int64)

    public operator func ()(x: Int64) {
        0
    }
}

main(): Int64 {
    var x = A.b(10) // choose enum constructor
    var y = A.a(1)
    var z = match (x) {
        case b(w) => w
        case <!UNREACHABLE_PATTERN!>a<!> => 0
    }
    var a: Int64 = a(1)
    return z - 10 + a
}
