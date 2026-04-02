
func applyFunc(f: (Int64) -> Int64, x: Int64): Int64 {
    return f(x)
}

main() {
    let result = applyFunc({ value => <!RETURN_TYPE_MISMATCH!>true<!> }, 10)
}
