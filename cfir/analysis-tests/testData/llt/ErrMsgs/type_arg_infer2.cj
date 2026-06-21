


// test err msg for conflicting constraints
func f<T>(x: T, y: T) {
}

main() {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>f(1, "Hello")<!>
}
