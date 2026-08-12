
func choose<T>(first: T, second: T): T {
    return first
}

main(): Unit {

    let _ = <!UNABLE_TO_INFER_GENERIC_FUNC!>choose<!>(1, true)
}
