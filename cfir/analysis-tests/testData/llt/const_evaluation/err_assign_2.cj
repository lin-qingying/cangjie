

struct S {
    static var v = 0
    const init() {
        <!EXPECT_CONST!>v = 0<!>
    }
}

main() {
    const s = S()
    S.v
}
