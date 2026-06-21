

class A {}

extend A {
    func f() {
        0
    }
}

const func f() {
    let a = <!EXPECT_CONST!>A<!>()
    <!EXPECT_CONST!>a.f<!>()
}

main() {
    f()
}
