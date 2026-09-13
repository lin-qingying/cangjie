


func doSth<T>(): Option<T> {
    let a = 0
    <!RETURN_TYPE_MISMATCH!>a as Option<T><!>
}

main() {
    doSth<Float64>()
    0
}
