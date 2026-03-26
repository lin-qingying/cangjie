// RUN_PIPELINE_TILL: FRONTEND
// DUMP_INFERENCE_LOGS
func pair<T>(left: T, right: T): T {
    return left
}

main() {
    let number: Int64 = pair(1, <!ARGUMENT_TYPE_MISMATCH!>"oops"<!>)
}
