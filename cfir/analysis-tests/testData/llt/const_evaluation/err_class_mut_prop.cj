

class A {
    // mut prop can be defined together with const init
    mut prop x: Int64 {
        get() {
            0
        }
        set(_) {}
    }

    const func f() {
        // but reference to the mut prop is not const expression
        <!EXPECT_CONST!>x<!>
    }

    const init() {}
}

main() {
    let a = A()
    a.x = 0
    a.x
}
