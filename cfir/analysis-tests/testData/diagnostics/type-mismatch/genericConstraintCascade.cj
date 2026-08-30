
func pair<T>(left: T, right: T): T {
    return left
}

main(): Unit {
    let _: Int64 = <!UNABLE_TO_INFER_GENERIC_FUNC!>pair<!>(1, "oops")

}
