

func f(x: Int32): Float64 {
    10.0
}

func f(x: Int64): Int64 {
    -10
}

main() {
    let a = 2.0 <!INVALID_BINARY_OPERATOR!>**<!> f(2)
    0
}
