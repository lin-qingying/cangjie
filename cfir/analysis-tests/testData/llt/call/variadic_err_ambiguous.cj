

func f() {
    0
}

func f(x!: Int64 = 0) {
    x
}

func f(x: Array<Int64>) {
    x.size
}

func g(x: Int64) {
    x
}

func g(x: Int64, y!: Int64 = 0) {
    x + y
}

func g(x: Array<Int64>) {
    x.size
}

main() {
    <!AMBIGUOUS_FUNCTION_CALL!>f<!>() + <!AMBIGUOUS_FUNCTION_CALL!>g<!>(1)
}
