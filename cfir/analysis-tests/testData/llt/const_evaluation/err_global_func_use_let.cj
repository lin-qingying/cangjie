

let x = 0

const func f() {
    <!EXPECT_CONST!>x<!> + 1
}

main() {
    f()
}
