

class A {}

var a: Int64 = 1

main() {
    if (a <= if (false) {
        var b: A = <!UNRESOLVED_REFERENCE!>A1<!>()
        1i64
    } else {
        1
    }) {
        return 1
    }
    return 0
}
