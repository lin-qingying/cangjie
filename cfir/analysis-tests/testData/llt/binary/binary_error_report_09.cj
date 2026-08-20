


class Test {
    operator func +(other: Test) {
        return this <!INVALID_BINARY_OPERATOR!>+<!> other  // recursive call without return type
    }
}

main() {
    let a = Test()
    let b = Test()
    let c = a <!INVALID_BINARY_OPERATOR!>+<!> b
}
