


// test constraints from destruction
func f<T>(a: Array<T>): Array<T> {
    a
}

main() {
    let v: Array<Array<String>> = <!UNABLE_TO_INFER_GENERIC_FUNC!>f([[1]])<!>
}
