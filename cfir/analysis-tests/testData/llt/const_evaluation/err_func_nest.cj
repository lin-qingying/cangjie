


// f(x) = 2 * x + 1
const func f(x: Int64) {
    func <!EXPECT_CONST!>g<!>(x: Int64) {
        2 * x
    }
    g(x) + 1
}

const x = f(3)

main() {
    x
}
