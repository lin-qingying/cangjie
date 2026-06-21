

func f() {
    1 < 2
}

main() {
    var a: Int64
    var b: Int64
    if (f()) {
    } else if ((a, b) = <!TYPE_MISMATCH!>(1, 2, 3)<!>) {
    }
    <!USED_BEFORE_INITIALIZATION!>b<!> - a - a
}
