

var global = 0

interface I {
    func f() {
        global++
    }
    const func g() {
        <!EXPECT_CONST!>f<!>()
    }
}

main() {}
