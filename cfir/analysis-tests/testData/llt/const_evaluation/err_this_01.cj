

struct A {
    const func x() {
        0
    }
    const init() {}
    func f() {
        const t = <!EXPECT_CONST!>x<!>
        t
    }
}

main() {}
