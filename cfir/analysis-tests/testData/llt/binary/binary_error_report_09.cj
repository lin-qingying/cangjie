


class Test {
    operator func <!UNABLE_TO_INFER_RETURN_TYPE!>+<!>(other: Test) {
        return this <!INVALID_BINARY_OPERATOR!>+<!> other  // recursive call without return type
    }
}

main() {
    let a = Test()
    let b = Test()
    let c = a + b
}
