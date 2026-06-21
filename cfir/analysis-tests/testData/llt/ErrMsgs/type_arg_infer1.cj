


// test err msg for no info
func f<T>() {}

main() {
    <!UNABLE_TO_INFER_GENERIC_FUNC!>f()<!>
}
