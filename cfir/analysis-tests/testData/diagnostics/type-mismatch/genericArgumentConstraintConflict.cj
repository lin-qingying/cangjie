
func choose<T>(first: T, second: T): T {
    return first
}

main(): Unit {

    let _ = <!NEW_INFERENCE_ERROR!>choose<!>(1, true)
}
