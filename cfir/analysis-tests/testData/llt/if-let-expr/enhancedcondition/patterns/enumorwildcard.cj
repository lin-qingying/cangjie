

enum A {
    | B | C(Int64) | ...
}

var a = B

main() {
    while (let <!DIFFERENT_OR_PATTERN!>B | _<!> <- a) {
        break
    }
    if (let <!DIFFERENT_OR_PATTERN!>B | _<!> <- a) {
        0
    } else {
        1
    }
}
