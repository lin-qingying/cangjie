

func f(x: Int32): Float64 {
    10.0
}

func f(x: Int64): Int64 {
    -10
}

func narrow(x: Int32): Float64 {
    10.0
}

main() {
    let a = 2.0 <!INVALID_BINARY_OPERATOR!>**<!> f(2)
    let b = 2.0 ** narrow(2)
    let c = 2.0 ** f(2i32)
    let d = 2.0 ** f(2i64)
    0
}
