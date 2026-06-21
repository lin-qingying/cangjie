

func benchmark(): Unit {
    let f: (Int64) -> Unit
    f = {
        n: Int64 => var res = spawn {
            => <!CAPTURE_BEFORE_INITIALIZATION!>f<!>(n - 1)
        }
    }
    f(4)
}

main() {
    benchmark()
}
