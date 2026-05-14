
func pair<T>(left: T, right: T): T {
    return left
}

main(): Unit {
    let _: Int64 = <!NEW_INFERENCE_ERROR!>pair<!>(1, "oops")

}
