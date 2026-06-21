


enum E {
    A(Int64)
}

const (A(a), (b, (c, d))) = (A(1), (2, (3, 4)))

const func f() {
    a == 1 && b == 2 && c == 3 && d == 4
}

main() {
    if (f()) {
        0
    } else {
        1
    }
}
