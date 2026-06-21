

var g = 0

struct S {
    var x: Int64
    const init(x: Int64) {
        this.x = x
        <!EXPECT_CONST!>g = 1<!>
        <!EXPECT_CONST!>g++<!>
    }
}

main() {
    const s = S(-1)
    s.x
}
